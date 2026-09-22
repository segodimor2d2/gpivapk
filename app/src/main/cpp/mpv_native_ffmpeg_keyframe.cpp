#include <jni.h>
#include <android/log.h>

#include "mpv_context.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavcodec/jni.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/error.h>
}

#include <stdio.h>
#include <string>
#include <unistd.h>
#include <errno.h>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jlong JNICALL

Java_com_rec_gpiv_player_MpvNative_nativeFindPreviousKeyframeFrame(
    JNIEnv* env,
    jobject thiz,
    jint fd,
    jlong frame
)
{
    if (fd < 0 || frame < 0) {
        return -1;
    }

    int sourceFd =
        dup(fd);

    if (sourceFd < 0) {
        LOGI(
            "FFmpeg KEYFRAME: dup() falhou: %s",
            strerror(errno)
        );

        return -1;
    }

    if (lseek(sourceFd, 0, SEEK_SET) < 0) {
        LOGI(
            "FFmpeg KEYFRAME: lseek(0) falhou: %s",
            strerror(errno)
        );

        close(sourceFd);

        return -1;
    }

    unsigned char* ioBuffer =
        static_cast<unsigned char*>(
            av_malloc(32768)
        );

    if (!ioBuffer) {
        close(sourceFd);
        return -1;
    }

    AVIOContext* ioContext =
        avio_alloc_context(
            ioBuffer,
            32768,
            0,
            &sourceFd,
            [](void* opaque, uint8_t* buffer, int bufferSize) -> int {
                int* fdPtr =
                    static_cast<int*>(opaque);

                ssize_t bytesRead =
                    read(*fdPtr, buffer, bufferSize);

                if (bytesRead < 0) {
                    return AVERROR(errno);
                }

                return bytesRead == 0
                    ? AVERROR_EOF
                    : static_cast<int>(bytesRead);
            },
            nullptr,
            [](void* opaque, int64_t offset, int whence) -> int64_t {
                int* fdPtr =
                    static_cast<int*>(opaque);

                if (whence == AVSEEK_SIZE) {
                    off_t current = lseek(*fdPtr, 0, SEEK_CUR);
                    off_t end = lseek(*fdPtr, 0, SEEK_END);

                    if (end < 0 || lseek(*fdPtr, current, SEEK_SET) < 0) {
                        return AVERROR(errno);
                    }

                    return end;
                }

                off_t position =
                    lseek(*fdPtr, offset, whence & 0xFFFF);

                return position < 0
                    ? AVERROR(errno)
                    : position;
            }
        );

    if (!ioContext) {
        av_free(ioBuffer);
        close(sourceFd);
        return -1;
    }

    AVFormatContext* inputContext =
        avformat_alloc_context();

    if (!inputContext) {
        av_freep(&ioContext->buffer);
        avio_context_free(&ioContext);
        close(sourceFd);
        return -1;
    }

    inputContext->pb = ioContext;
    inputContext->flags |= AVFMT_FLAG_CUSTOM_IO;

    auto closeInput = [&]() {
        avformat_close_input(&inputContext);
        av_freep(&ioContext->buffer);
        avio_context_free(&ioContext);
        close(sourceFd);
    };

    int result =
        avformat_open_input(
            &inputContext,
            nullptr,
            nullptr,
            nullptr
        );

    if (result < 0) {
        LOGI(
            "FFmpeg KEYFRAME: avformat_open_input() falhou: %d",
            result
        );

        closeInput();

        return -1;
    }

    result =
        avformat_find_stream_info(
            inputContext,
            nullptr
        );

    if (result < 0) {
        LOGI(
            "FFmpeg KEYFRAME: find_stream_info() falhou: %d",
            result
        );

        closeInput();

        return -1;
    }

    int videoStreamIndex =
        av_find_best_stream(
            inputContext,
            AVMEDIA_TYPE_VIDEO,
            -1,
            -1,
            nullptr,
            0
        );

    if (videoStreamIndex < 0) {
        LOGI(
            "FFmpeg KEYFRAME: stream de vídeo não encontrado"
        );

        closeInput();

        return -1;
    }

    AVStream* videoStream =
        inputContext->streams[videoStreamIndex];

    AVRational frameRate =
        videoStream->avg_frame_rate;

    if (frameRate.num <= 0 || frameRate.den <= 0) {
        frameRate =
            videoStream->r_frame_rate;
    }

    if (frameRate.num <= 0 || frameRate.den <= 0) {
        closeInput();

        return -1;
    }

    int64_t targetPts =
        av_rescale_q(
            frame,
            AVRational{
                frameRate.den,
                frameRate.num
            },
            videoStream->time_base
        );

    result =
        av_seek_frame(
            inputContext,
            videoStreamIndex,
            targetPts,
            AVSEEK_FLAG_BACKWARD
        );

    if (result < 0) {
        LOGI(
            "FFmpeg KEYFRAME: av_seek_frame() falhou: %d",
            result
        );

        closeInput();

        return -1;
    }

    AVPacket* packet =
        av_packet_alloc();

    int64_t keyframePts =
        AV_NOPTS_VALUE;

    if (packet) {
        while (
            av_read_frame(
                inputContext,
                packet
            ) >= 0
        ) {
            if (
                packet->stream_index == videoStreamIndex &&
                packet->pts != AV_NOPTS_VALUE &&
                (packet->flags & AV_PKT_FLAG_KEY)
            ) {
                keyframePts = packet->pts;
                av_packet_unref(packet);
                break;
            }

            av_packet_unref(packet);
        }

        av_packet_free(
            &packet
        );
    }

    jlong keyframeFrame =
        keyframePts == AV_NOPTS_VALUE
            ? -1
            : av_rescale_q(
                keyframePts,
                videoStream->time_base,
                AVRational{
                    frameRate.den,
                    frameRate.num
                }
            );

    closeInput();

    return keyframeFrame;
}

