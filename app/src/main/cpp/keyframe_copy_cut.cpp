#include "keyframe_copy_cut.h"
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
}
#include <cerrno>
#include <cmath>
#include <cstdio>
#include <stdexcept>
#include <unistd.h>

namespace {
void check(int code, const char* operation) {
    if (code >= 0) return;
    char message[AV_ERROR_MAX_STRING_SIZE];
    av_strerror(code, message, sizeof(message));
    throw std::runtime_error(std::string(operation) + ": " + message);
}
struct Session {
    int fd = -1;
    AVIOContext* io = nullptr;
    AVFormatContext* input = nullptr;
    AVFormatContext* output = nullptr;
    AVPacket* packet = av_packet_alloc();
    std::string path;
    bool complete = false;
    ~Session() {
        av_packet_free(&packet);
        if (output) {
            if (output->pb) avio_closep(&output->pb);
            avformat_free_context(output);
        }
        avformat_close_input(&input);
        if (io) { av_freep(&io->buffer); avio_context_free(&io); }
        if (fd >= 0) close(fd);
        if (!complete && !path.empty()) std::remove(path.c_str());
    }
    void open(int source) {
        if (!packet) throw std::bad_alloc();
        fd = dup(source);
        if (fd < 0) check(AVERROR(errno), "dup");
        if (lseek(fd, 0, SEEK_SET) < 0) check(AVERROR(errno), "seek input");
        auto* buffer = static_cast<unsigned char*>(av_malloc(32768));
        if (!buffer) throw std::bad_alloc();
        io = avio_alloc_context(buffer, 32768, 0, &fd,
            [](void* opaque, uint8_t* data, int size) -> int {
                ssize_t count;
                do { count = read(*static_cast<int*>(opaque), data, size); }
                while (count < 0 && errno == EINTR);
                return count > 0 ? static_cast<int>(count) : count == 0 ? AVERROR_EOF : AVERROR(errno);
            }, nullptr,
            [](void* opaque, int64_t offset, int whence) -> int64_t {
                int sourceFd = *static_cast<int*>(opaque);
                if (whence & AVSEEK_SIZE) {
                    off_t current = lseek(sourceFd, 0, SEEK_CUR);
                    off_t end = lseek(sourceFd, 0, SEEK_END);
                    if (current < 0 || end < 0 || lseek(sourceFd, current, SEEK_SET) < 0)
                        return AVERROR(errno);
                    return end;
                }
                off_t result = lseek(sourceFd, offset, whence & ~AVSEEK_FORCE);
                return result < 0 ? AVERROR(errno) : result;
            });
        if (!io) { av_free(buffer); throw std::bad_alloc(); }
        input = avformat_alloc_context();
        if (!input) throw std::bad_alloc();
        input->pb = io;
        input->flags |= AVFMT_FLAG_CUSTOM_IO;
        check(avformat_open_input(&input, nullptr, nullptr, nullptr), "open input");
        check(avformat_find_stream_info(input, nullptr), "stream info");
    }
};
}

AccurateCutResult copyCutAtKeyframes(int fd, const std::string& outputPath,
                                    double positionA, double positionB) {
    if (fd < 0 || !std::isfinite(positionA) || !std::isfinite(positionB) ||
        positionA < 0 || positionB < positionA) throw std::runtime_error("Invalid cut markers");
    Session s;
    s.open(fd);
    int videoIndex = -1, audioIndex = -1;
    for (unsigned i = 0; i < s.input->nb_streams; ++i) {
        auto type = s.input->streams[i]->codecpar->codec_type;
        if (type == AVMEDIA_TYPE_VIDEO && videoIndex < 0) videoIndex = i;
        if (type == AVMEDIA_TYPE_AUDIO && audioIndex < 0) audioIndex = i;
    }
    if (videoIndex < 0) throw std::runtime_error("Video stream missing");
    auto* video = s.input->streams[videoIndex];
    const auto tb = video->time_base;
    const int64_t origin = s.input->start_time != AV_NOPTS_VALUE
        ? av_rescale_q(s.input->start_time, AV_TIME_BASE_Q, tb)
        : video->start_time != AV_NOPTS_VALUE ? video->start_time : 0;
    const int64_t targetA = origin + av_rescale_q(std::llround(positionA * AV_TIME_BASE), AV_TIME_BASE_Q, tb);
    const int64_t targetB = origin + av_rescale_q(std::llround(positionB * AV_TIME_BASE), AV_TIME_BASE_Q, tb);
    int64_t start = AV_NOPTS_VALUE, end = AV_NOPTS_VALUE, endExclusive = AV_NOPTS_VALUE;
    int64_t last = AV_NOPTS_VALUE, lastDuration = 0;
    int status;
    // Scan from the beginning: demuxer seek indexes can select an older GOP,
    // and estimated frame numbers / average FPS do not identify VFR markers.
    while ((status = av_read_frame(s.input, s.packet)) >= 0) {
        auto* p = s.packet;
        if (p->stream_index == videoIndex && p->pts != AV_NOPTS_VALUE) {
            if (last == AV_NOPTS_VALUE || p->pts > last) { last = p->pts; lastDuration = p->duration; }
            if (p->flags & AV_PKT_FLAG_KEY) {
                if (p->pts <= targetA && (start == AV_NOPTS_VALUE || p->pts > start)) start = p->pts;
                if (p->pts >= targetB && (end == AV_NOPTS_VALUE || p->pts < end)) {
                    end = p->pts;
                    endExclusive = end + p->duration;
                }
            }
        }
        av_packet_unref(p);
    }
    if (status != AVERROR_EOF) check(status, "scan boundaries");
    if (start == AV_NOPTS_VALUE || last == AV_NOPTS_VALUE || targetA > last)
        throw std::runtime_error("No keyframe at or before A");
    if (end == AV_NOPTS_VALUE) { end = last; endExclusive = last + lastDuration; }
    if (endExclusive <= end) {
        const auto rate = av_guess_frame_rate(s.input, video, nullptr);
        if (rate.num <= 0 || rate.den <= 0) throw std::runtime_error("Frame duration unavailable");
        endExclusive = end + av_rescale_q(1, av_inv_q(rate), tb);
    }
    check(av_seek_frame(s.input, videoIndex, start, AVSEEK_FLAG_BACKWARD), "seek start keyframe");
    s.path = outputPath;
    check(avformat_alloc_output_context2(&s.output, nullptr, "mov", outputPath.c_str()), "create MOV");
    auto addStream = [&](int index) {
        auto* out = avformat_new_stream(s.output, nullptr);
        if (!out) throw std::bad_alloc();
        check(avcodec_parameters_copy(out->codecpar, s.input->streams[index]->codecpar), "copy codec parameters");
        out->codecpar->codec_tag = 0;
        out->time_base = s.input->streams[index]->time_base;
        check(av_dict_copy(&out->metadata, s.input->streams[index]->metadata, 0), "copy metadata");
        return out;
    };
    auto* outputVideo = addStream(videoIndex);
    auto* outputAudio = audioIndex >= 0 ? addStream(audioIndex) : nullptr;
    check(avio_open(&s.output->pb, outputPath.c_str(), AVIO_FLAG_WRITE), "open output");
    check(avformat_write_header(s.output, nullptr), "write header");
    int written = 0;
    bool started = false;
    // Keep reading past the end boundary: B-frames with smaller PTS can occur
    // after it in decode order. Do not stop at the first PTS greater than B.
    while ((status = av_read_frame(s.input, s.packet)) >= 0) {
        auto* p = s.packet;
        AVStream* out = nullptr;
        if (p->stream_index == videoIndex && p->pts != AV_NOPTS_VALUE && p->pts >= start && p->pts <= end) {
            if (!started) {
                if (p->pts != start || !(p->flags & AV_PKT_FLAG_KEY)) {
                    av_packet_unref(p);
                    continue;
                }
                started = true;
            }
            out = outputVideo;
            ++written;
        } else if (p->stream_index == audioIndex && outputAudio && p->pts != AV_NOPTS_VALUE && p->duration > 0) {
            auto audioTB = s.input->streams[audioIndex]->time_base;
            if (av_compare_ts(p->pts + p->duration, audioTB, start, tb) > 0 &&
                av_compare_ts(p->pts, audioTB, endExclusive, tb) < 0) out = outputAudio;
        }
        if (out) {
            const auto inputTB = s.input->streams[p->stream_index]->time_base;
            const int64_t offset = av_rescale_q(start, tb, inputTB);
            p->pts -= offset;
            if (p->dts != AV_NOPTS_VALUE) p->dts -= offset;
            // MOV can change stream time_base in write_header (e.g. 1/1000 to
            // 1/16000). Use the actual output base, for PTS, DTS AND duration.
            av_packet_rescale_ts(p, inputTB, out->time_base);
            p->stream_index = out->index;
            p->pos = -1;
            check(av_interleaved_write_frame(s.output, p), "write packet");
        }
        av_packet_unref(p);
    }
    if (status != AVERROR_EOF) check(status, "read packets");
    if (!written) throw std::runtime_error("Empty cut");
    check(av_write_trailer(s.output), "write trailer");
    s.complete = true;
    return {start, end, tb.num, tb.den};
}
