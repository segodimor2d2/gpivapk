#include "frame_accurate_cut.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/display.h>
#include <libavutil/opt.h>
#include <libswscale/swscale.h>
}

#include <algorithm>
#include <cerrno>
#include <cmath>
#include <cstdio>
#include <functional>
#include <limits>
#include <stdexcept>
#include <unistd.h>

namespace {
void check(int code, const char* operation) {
    if (code >= 0) return;
    char message[AV_ERROR_MAX_STRING_SIZE];
    av_strerror(code, message, sizeof(message));
    throw std::runtime_error(std::string(operation) + ": " + message);
}

struct CutSession {
    int fd = -1;
    AVIOContext* io = nullptr;
    AVFormatContext* input = nullptr;
    AVFormatContext* output = nullptr;
    AVCodecContext* decoder = nullptr;
    AVCodecContext* encoder = nullptr;
    AVPacket* packet = av_packet_alloc();
    AVPacket* encoded = av_packet_alloc();
    AVFrame* decoded = av_frame_alloc();
    AVFrame* converted = av_frame_alloc();
    SwsContext* scaler = nullptr;
    AVPixelFormat sourceFormat = AV_PIX_FMT_NONE;
    std::string path;
    bool complete = false;

    ~CutSession() {
        av_packet_free(&packet);
        av_packet_free(&encoded);
        av_frame_free(&decoded);
        av_frame_free(&converted);
        sws_freeContext(scaler);
        avcodec_free_context(&decoder);
        avcodec_free_context(&encoder);
        if (output) {
            if (output->pb) avio_closep(&output->pb);
            avformat_free_context(output);
        }
        avformat_close_input(&input);
        if (io) {
            av_freep(&io->buffer);
            avio_context_free(&io);
        }
        if (fd >= 0) close(fd);
        if (!complete && !path.empty()) std::remove(path.c_str());
    }

    void open(int source) {
        if (!packet || !encoded || !decoded || !converted) throw std::bad_alloc();
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
                return count > 0 ? static_cast<int>(count)
                    : count == 0 ? AVERROR_EOF : AVERROR(errno);
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

    // Entrega os frames em ordem de apresentação, inclusive os B-frames atrasados.
    bool decode(AVPacket* source, const std::function<bool(AVFrame*)>& consume) {
        check(avcodec_send_packet(decoder, source), "send decoder packet");
        for (;;) {
            int status = avcodec_receive_frame(decoder, decoded);
            if (status == AVERROR(EAGAIN) || status == AVERROR_EOF) return true;
            check(status, "decode frame");
            bool keepGoing = consume(decoded);
            av_frame_unref(decoded);
            if (!keepGoing) return false;
        }
    }
};
}

AccurateCutResult cutFrameAccurately(int fd, const std::string& outputPath,
                                   double positionA, double positionB, const VideoCrop& crop) {
    if (fd < 0 || !std::isfinite(positionA) || !std::isfinite(positionB) ||
        positionA < 0 || positionB < positionA) throw std::runtime_error("Invalid cut markers");
    CutSession s;
    s.path = outputPath;
    s.open(fd);
    int videoIndex = av_find_best_stream(s.input, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);
    check(videoIndex, "find video");
    int audioIndex = av_find_best_stream(s.input, AVMEDIA_TYPE_AUDIO, -1, -1, nullptr, 0);
    AVStream* video = s.input->streams[videoIndex];
    const AVRational timeBase = video->time_base;
    const AVCodec* codec = avcodec_find_decoder(video->codecpar->codec_id);
    if (!codec) throw std::runtime_error("Video decoder unavailable");
    s.decoder = avcodec_alloc_context3(codec);
    if (!s.decoder) throw std::bad_alloc();
    check(avcodec_parameters_to_context(s.decoder, video->codecpar), "decoder parameters");
    s.decoder->pkt_timebase = timeBase;
    check(avcodec_open2(s.decoder, codec, nullptr), "open decoder");

    // mpv time-pos usa a linha de tempo relativa ao início do contêiner.
    const int64_t origin = s.input->start_time != AV_NOPTS_VALUE
        ? av_rescale_q(s.input->start_time, AV_TIME_BASE_Q, timeBase)
        : video->start_time != AV_NOPTS_VALUE ? video->start_time : 0;
    const int64_t targetA = origin + av_rescale_q(std::llround(positionA * AV_TIME_BASE), AV_TIME_BASE_Q, timeBase);
    const int64_t targetB = origin + av_rescale_q(std::llround(positionB * AV_TIME_BASE), AV_TIME_BASE_Q, timeBase);
    int64_t start = AV_NOPTS_VALUE, end = AV_NOPTS_VALUE, endExclusive = AV_NOPTS_VALUE;
    int64_t distanceA = INT64_MAX, distanceB = INT64_MAX;
    int64_t lastDuration = 0;
    AVRational fps = av_guess_frame_rate(s.input, video, nullptr);
    if (fps.num <= 0 || fps.den <= 0) fps = AVRational{30, 1};
    auto findBounds = [&](AVFrame* frame) {
        const int64_t pts = frame->best_effort_timestamp;
        if (pts == AV_NOPTS_VALUE) throw std::runtime_error("Frame without presentation timestamp");
        if (end != AV_NOPTS_VALUE && end >= targetB) {
            endExclusive = pts;
            return false;
        }
        const int64_t da = std::llabs(pts - targetA), db = std::llabs(pts - targetB);
        if (da < distanceA) { start = pts; distanceA = da; }
        if (db < distanceB) { end = pts; distanceB = db; lastDuration = frame->duration; }
        if (pts > targetB && end != pts) { endExclusive = pts; return false; }
        return true;
    };
    bool scanning = true;
    int status;
    while (scanning && (status = av_read_frame(s.input, s.packet)) >= 0) {
        if (s.packet->stream_index == videoIndex) scanning = s.decode(s.packet, findBounds);
        av_packet_unref(s.packet);
    }
    if (scanning) {
        if (status != AVERROR_EOF) check(status, "read input bounds");
        s.decode(nullptr, findBounds);
    }
    if (start == AV_NOPTS_VALUE || end == AV_NOPTS_VALUE || start > end)
        throw std::runtime_error("No video frames in cut interval");
    if (endExclusive == AV_NOPTS_VALUE)
        endExclusive = end + std::max<int64_t>(1, lastDuration > 0 ? lastDuration
            : av_rescale_q(1, av_inv_q(fps), timeBase));
    if (targetA >= endExclusive || targetB > endExclusive)
        throw std::runtime_error("Cut marker beyond end of video");

    // Segunda passagem: decodifica o GOP anterior, mas exporta só os frames A..B.
    check(av_seek_frame(s.input, videoIndex, start, AVSEEK_FLAG_BACKWARD), "seek cut start");
    avcodec_flush_buffers(s.decoder);
    const AVCodec* mjpeg = avcodec_find_encoder(AV_CODEC_ID_MJPEG);
    if (!mjpeg) throw std::runtime_error("MJPEG encoder unavailable");
    s.encoder = avcodec_alloc_context3(mjpeg);
    if (!s.encoder) throw std::bad_alloc();
    if (!std::isfinite(crop.left) || !std::isfinite(crop.top) ||
        !std::isfinite(crop.right) || !std::isfinite(crop.bottom) ||
        crop.left < 0 || crop.top < 0 || crop.right > 1 || crop.bottom > 1 ||
        crop.left >= crop.right || crop.top >= crop.bottom)
        throw std::runtime_error("Invalid video crop");
    // Align the origin with chroma samples; keep the full image when no crop is set.
    const int cropX = static_cast<int>(crop.left * s.decoder->width) / 2 * 2;
    const int cropY = static_cast<int>(crop.top * s.decoder->height) / 2 * 2;
    const int cropRight = std::clamp(static_cast<int>(std::ceil(crop.right * s.decoder->width)), cropX + 1, s.decoder->width);
    const int cropBottom = std::clamp(static_cast<int>(std::ceil(crop.bottom * s.decoder->height)), cropY + 1, s.decoder->height);
    s.encoder->width = cropRight - cropX;
    s.encoder->height = cropBottom - cropY;
    s.encoder->pix_fmt = AV_PIX_FMT_YUVJ420P;
    s.encoder->time_base = timeBase;
    s.encoder->framerate = fps;
    s.encoder->sample_aspect_ratio = s.decoder->sample_aspect_ratio;
    s.encoder->color_range = AVCOL_RANGE_JPEG;
    s.encoder->colorspace = s.decoder->colorspace;
    s.encoder->color_primaries = s.decoder->color_primaries;
    s.encoder->color_trc = s.decoder->color_trc;
    s.encoder->flags |= AV_CODEC_FLAG_QSCALE;
    s.encoder->global_quality = FF_QP2LAMBDA * 2;
    check(avformat_alloc_output_context2(&s.output, nullptr, "mov", outputPath.c_str()), "create MOV");
    if (!s.output) throw std::bad_alloc();
    if (s.output->oformat->flags & AVFMT_GLOBALHEADER) s.encoder->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    check(avcodec_open2(s.encoder, mjpeg, nullptr), "open MJPEG encoder");
    AVStream* outVideo = avformat_new_stream(s.output, nullptr);
    if (!outVideo) throw std::bad_alloc();
    outVideo->time_base = timeBase;
    outVideo->avg_frame_rate = fps;
    check(avcodec_parameters_from_context(outVideo->codecpar, s.encoder), "output video parameters");
    av_dict_copy(&outVideo->metadata, video->metadata, 0);
    const auto* matrix = av_packet_side_data_get(video->codecpar->coded_side_data,
        video->codecpar->nb_coded_side_data, AV_PKT_DATA_DISPLAYMATRIX);
    if (matrix) {
        auto* copy = av_packet_side_data_new(&outVideo->codecpar->coded_side_data,
            &outVideo->codecpar->nb_coded_side_data, AV_PKT_DATA_DISPLAYMATRIX, matrix->size, 0);
        if (!copy) throw std::bad_alloc();
        std::copy(matrix->data, matrix->data + matrix->size, copy->data);
    }
    AVStream* audio = audioIndex >= 0 ? s.input->streams[audioIndex] : nullptr;
    AVStream* outAudio = nullptr;
    if (audio) {
        outAudio = avformat_new_stream(s.output, nullptr);
        if (!outAudio) throw std::bad_alloc();
        check(avcodec_parameters_copy(outAudio->codecpar, audio->codecpar), "audio parameters");
        outAudio->codecpar->codec_tag = 0;
        outAudio->time_base = audio->time_base;
        av_dict_copy(&outAudio->metadata, audio->metadata, 0);
    }
    check(avio_open(&s.output->pb, outputPath.c_str(), AVIO_FLAG_WRITE), "open cut output");
    check(avformat_write_header(s.output, nullptr), "write MOV header");
    s.converted->format = s.encoder->pix_fmt;
    s.converted->width = s.encoder->width;
    s.converted->height = s.encoder->height;
    check(av_frame_get_buffer(s.converted, 32), "allocate converted frame");
    int64_t written = 0;
    auto writeEncoded = [&](AVFrame* frame) {
        check(avcodec_send_frame(s.encoder, frame), "send encoder frame");
        for (;;) {
            int result = avcodec_receive_packet(s.encoder, s.encoded);
            if (result == AVERROR(EAGAIN) || result == AVERROR_EOF) break;
            check(result, "encode frame");
            av_packet_rescale_ts(s.encoded, s.encoder->time_base, outVideo->time_base);
            s.encoded->stream_index = outVideo->index;
            check(av_interleaved_write_frame(s.output, s.encoded), "write video packet");
            av_packet_unref(s.encoded);
        }
    };
    bool videoDone = false, audioDone = audio == nullptr;
    auto encodeSelected = [&](AVFrame* frame) {
        int64_t pts = frame->best_effort_timestamp;
        if (pts == AV_NOPTS_VALUE) throw std::runtime_error("Frame without PTS during cut");
        if (pts > end) { videoDone = true; return false; }
        if (pts < start) return true;
        if (frame->width != s.decoder->width || frame->height != s.decoder->height)
            throw std::runtime_error("Video dimensions changed during cut");
        frame->crop_left = cropX;
        frame->crop_top = cropY;
        frame->crop_right = frame->width - cropRight;
        frame->crop_bottom = frame->height - cropBottom;
        check(av_frame_apply_cropping(frame, AV_FRAME_CROP_UNALIGNED), "crop video frame");
        if (!s.scaler || s.sourceFormat != frame->format) {
            sws_freeContext(s.scaler);
            s.sourceFormat = static_cast<AVPixelFormat>(frame->format);
            s.scaler = sws_getContext(frame->width, frame->height, s.sourceFormat,
                s.encoder->width, s.encoder->height, s.encoder->pix_fmt,
                SWS_BICUBIC, nullptr, nullptr, nullptr);
        }
        if (!s.scaler) throw std::runtime_error("Pixel converter unavailable");
        const int colorspace = frame->colorspace == AVCOL_SPC_BT709 ? SWS_CS_ITU709
            : frame->colorspace == AVCOL_SPC_BT2020_NCL ? SWS_CS_BT2020 : SWS_CS_DEFAULT;
        const int* coefficients = sws_getCoefficients(colorspace);
        check(sws_setColorspaceDetails(s.scaler, coefficients, frame->color_range == AVCOL_RANGE_JPEG,
            coefficients, 1, 0, 1 << 16, 1 << 16), "set color range");
        check(av_frame_make_writable(s.converted), "make frame writable");
        check(sws_scale(s.scaler, frame->data, frame->linesize, 0, frame->height,
            s.converted->data, s.converted->linesize), "convert pixels");
        s.converted->pts = pts - start;
        s.converted->duration = pts == end ? endExclusive - end : frame->duration;
        s.converted->quality = s.encoder->global_quality;
        s.converted->color_range = AVCOL_RANGE_JPEG;
        writeEncoded(s.converted);
        ++written;
        return true;
    };
    while (!(videoDone && audioDone) && (status = av_read_frame(s.input, s.packet)) >= 0) {
        if (s.packet->stream_index == videoIndex && !videoDone) s.decode(s.packet, encodeSelected);
        else if (audio && s.packet->stream_index == audioIndex && !audioDone) {
            int64_t pts = s.packet->pts != AV_NOPTS_VALUE ? s.packet->pts : s.packet->dts;
            if (pts == AV_NOPTS_VALUE) throw std::runtime_error("Audio packet without timestamp");
            int64_t a = av_rescale_q(start, timeBase, audio->time_base);
            int64_t b = av_rescale_q(endExclusive, timeBase, audio->time_base);
            if (pts >= b) audioDone = true;
            else if (pts + std::max<int64_t>(1, s.packet->duration) > a) {
                // Uma única origem para áudio e vídeo conserva a sincronização.
                if (s.packet->pts != AV_NOPTS_VALUE) s.packet->pts -= a;
                if (s.packet->dts != AV_NOPTS_VALUE) s.packet->dts -= a;
                av_packet_rescale_ts(s.packet, audio->time_base, outAudio->time_base);
                s.packet->stream_index = outAudio->index;
                s.packet->pos = -1;
                check(av_interleaved_write_frame(s.output, s.packet), "write audio packet");
            }
        }
        av_packet_unref(s.packet);
    }
    if (!(videoDone && audioDone) && status != AVERROR_EOF) check(status, "read cut packets");
    if (!videoDone) s.decode(nullptr, encodeSelected);
    writeEncoded(nullptr);
    if (written == 0) throw std::runtime_error("No frames exported");
    check(av_write_trailer(s.output), "write MOV trailer");
    check(avio_closep(&s.output->pb), "close MOV");
    s.complete = true;
    return {start, end, timeBase.num, timeBase.den};
}
