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
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeProbeFd(
    JNIEnv* env,
    jobject thiz,
    jint fd
)
{
    if (fd < 0) {

        LOGI(
            "FFmpeg: probeFd() -> fd inválido: %d",
            fd
        );

        return;
    }

    int probeFd =
        dup(fd);

    if (probeFd < 0) {

        LOGI(
            "FFmpeg: probeFd() -> dup() falhou"
        );

        return;
    }

    LOGI(
        "FFmpeg: probeFd() -> fd original=%d, cópia=%d",
        fd,
        probeFd
    );

    unsigned char* ioBuffer =
        static_cast<unsigned char*>(
            av_malloc(32768)
        );

    if (!ioBuffer) {
        LOGI(
            "FFmpeg: probeFd() -> av_malloc() falhou"
        );
        close(probeFd);
        return;
    }

    AVIOContext* ioContext =
        avio_alloc_context(
            ioBuffer,
            32768,
            0,
            &probeFd,

            [](void* opaque, uint8_t* buffer, int bufferSize) -> int {

                int* fdPtr =
                    static_cast<int*>(opaque);

                if (!fdPtr || *fdPtr < 0) {
                    return AVERROR(EINVAL);
                }

                ssize_t bytesRead =
                    read(*fdPtr, buffer, static_cast<size_t>(bufferSize));

                if (bytesRead < 0) {
                    LOGI(
                        "FFmpeg: AVIO READ ERRO fd=%d errno=%d",
                        *fdPtr,
                        errno
                    );
                    return AVERROR(errno);
                }

                if (bytesRead == 0) {
                    LOGI(
                        "FFmpeg: AVIO READ EOF fd=%d",
                        *fdPtr
                    );
                    return AVERROR_EOF;
                }

                return static_cast<int>(bytesRead);


            },

            nullptr,

            [](void* opaque, int64_t offset, int whence) -> int64_t {
                int* fdPtr = static_cast<int*>(opaque);

                if (!fdPtr || *fdPtr < 0) {
                    return AVERROR(EINVAL);
                }

                if (whence == AVSEEK_SIZE) {
                    off_t current = lseek(*fdPtr, 0, SEEK_CUR);
                    off_t end = lseek(*fdPtr, 0, SEEK_END);

                    if (end < 0) {
                        return AVERROR(errno);
                    }

                    if (lseek(*fdPtr, current, SEEK_SET) < 0) {
                        return AVERROR(errno);
                    }

                    return static_cast<int64_t>(end);
                }

                int seekWhence = whence & 0xFFFF;

                off_t result =
                    lseek(
                        *fdPtr,
                        static_cast<off_t>(offset),
                        seekWhence
                    );

                if (result < 0) {
                    LOGI(
                        "FFmpeg: AVIO SEEK ERRO offset=%lld whence=0x%x errno=%d",
                        static_cast<long long>(offset),
                        whence,
                        errno
                    );

                    return AVERROR(errno);
                }

                return static_cast<int64_t>(result);
            }
        );

    if (!ioContext) {
        LOGI(
            "FFmpeg: probeFd() -> avio_alloc_context() falhou"
        );
        av_free(ioBuffer);
        close(probeFd);
        return;
    }

    AVFormatContext* formatContext =
        avformat_alloc_context();

    if (!formatContext) {

        LOGI(
            "FFmpeg: probeFd() -> avformat_alloc_context() falhou"
        );

        av_freep(
            &ioContext->buffer
        );

        avio_context_free(
            &ioContext
        );

        close(probeFd);

        return;
    }

    formatContext->pb =
        ioContext;

    formatContext->flags |=
        AVFMT_FLAG_CUSTOM_IO;

    int result =
        avformat_open_input(
            &formatContext,
            nullptr,
            nullptr,
            nullptr
        );

    if (result < 0) {

        char errorBuffer[
            AV_ERROR_MAX_STRING_SIZE
        ];

        av_strerror(
            result,
            errorBuffer,
            sizeof(errorBuffer)
        );

        LOGI(
            "FFmpeg: avformat_open_input() falhou: %s",
            errorBuffer
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );

            avio_context_free(
                &ioContext
            );
        }

        close(probeFd);

        return;
    }

    result =
        avformat_find_stream_info(
            formatContext,
            nullptr
        );

    if (result < 0) {

        char errorBuffer[
            AV_ERROR_MAX_STRING_SIZE
        ];

        av_strerror(
            result,
            errorBuffer,
            sizeof(errorBuffer)
        );

        LOGI(
            "FFmpeg: avformat_find_stream_info() falhou: %s",
            errorBuffer
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );

            avio_context_free(
                &ioContext
            );
        }

        close(probeFd);

        return;
    }

    LOGI(
        "FFmpeg: formato=%s, streams=%u, duração=%lld us",
        formatContext->iformat
            ? formatContext->iformat->name
            : "unknown",
        formatContext->nb_streams,
        static_cast<long long>(
            formatContext->duration
        )
    );

    for (
        unsigned int i = 0;
        i < formatContext->nb_streams;
        ++i
    ) {

        AVStream* stream =
            formatContext->streams[i];

        if (
            stream->codecpar->codec_type
            == AVMEDIA_TYPE_VIDEO
        ) {

            LOGI(
                "FFmpeg: vídeo stream=%u codec=%s",
                i,
                avcodec_get_name(
                    stream->codecpar->codec_id
                )
            );

            LOGI(
                "FFmpeg: vídeo %dx%d",
                stream->codecpar->width,
                stream->codecpar->height
            );

            LOGI(
                "FFmpeg: FPS=%d/%d",
                stream->avg_frame_rate.num,
                stream->avg_frame_rate.den
            );
        }

        if (
            stream->codecpar->codec_type
            == AVMEDIA_TYPE_AUDIO
        ) {

            LOGI(
                "FFmpeg: áudio stream=%u codec=%s",
                i,
                avcodec_get_name(
                    stream->codecpar->codec_id
                )
            );
        }
    }

    avformat_close_input(
        &formatContext
    );

    if (ioContext) {
        av_freep(
            &ioContext->buffer
        );

        avio_context_free(
            &ioContext
        );
    }

    close(probeFd);

    LOGI(
        "FFmpeg: probeFd() concluído"
    );
}
