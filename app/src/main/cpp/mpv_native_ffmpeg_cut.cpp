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
JNIEXPORT jlongArray JNICALL

Java_com_rec_gpiv_player_MpvNative_nativeTestCutFrames(
    JNIEnv* env,
    jobject thiz,
    jint fd,
    jlong frameA,
    jlong frameB
)
{
    if (fd < 0) {

        LOGI(
            "FFmpeg COPY: fd inválido: %d",
            fd
        );

        return nullptr;
    }

    if (
        frameA < 0 ||
        frameB < frameA
    ) {

        LOGI(
            "FFmpeg COPY: intervalo inválido A=%lld B=%lld",
            static_cast<long long>(frameA),
            static_cast<long long>(frameB)
        );

        return nullptr;
    }

    int sourceFd =
        dup(fd);

    if (sourceFd < 0) {

        LOGI(
            "FFmpeg COPY: dup() falhou: %s",
            strerror(errno)
        );

        return nullptr;
    }

    if (
        lseek(
            sourceFd,
            0,
            SEEK_SET
        ) < 0
    ) {

        LOGI(
            "FFmpeg COPY: lseek(0) falhou: %s",
            strerror(errno)
        );

        close(sourceFd);

        return nullptr;
    }

    unsigned char* ioBuffer =
        static_cast<unsigned char*>(
            av_malloc(32768)
        );

    if (!ioBuffer) {

        LOGI(
            "FFmpeg COPY: av_malloc() falhou"
        );

        close(sourceFd);

        return nullptr;
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

                if (
                    !fdPtr ||
                    *fdPtr < 0
                ) {
                    return AVERROR(EINVAL);
                }

                ssize_t bytesRead =
                    read(
                        *fdPtr,
                        buffer,
                        static_cast<size_t>(bufferSize)
                    );

                if (bytesRead < 0) {
                    return AVERROR(errno);
                }

                if (bytesRead == 0) {
                    return AVERROR_EOF;
                }

                return static_cast<int>(
                    bytesRead
                );
            },

            nullptr,

            [](void* opaque, int64_t offset, int whence) -> int64_t {

                int* fdPtr =
                    static_cast<int*>(opaque);

                if (
                    !fdPtr ||
                    *fdPtr < 0
                ) {
                    return AVERROR(EINVAL);
                }

                if (
                    whence == AVSEEK_SIZE
                ) {

                    off_t current =
                        lseek(
                            *fdPtr,
                            0,
                            SEEK_CUR
                        );

                    off_t end =
                        lseek(
                            *fdPtr,
                            0,
                            SEEK_END
                        );

                    if (end < 0) {
                        return AVERROR(errno);
                    }

                    if (
                        lseek(
                            *fdPtr,
                            current,
                            SEEK_SET
                        ) < 0
                    ) {
                        return AVERROR(errno);
                    }

                    return static_cast<int64_t>(
                        end
                    );
                }

                int seekWhence =
                    whence & 0xFFFF;

                off_t result =
                    lseek(
                        *fdPtr,
                        static_cast<off_t>(offset),
                        seekWhence
                    );

                if (result < 0) {
                    return AVERROR(errno);
                }

                return static_cast<int64_t>(
                    result
                );
            }
        );

    if (!ioContext) {

        LOGI(
            "FFmpeg COPY: avio_alloc_context() falhou"
        );

        av_free(
            ioBuffer
        );

        close(sourceFd);

        return nullptr;
    }

    AVFormatContext* inputContext =
        avformat_alloc_context();

    if (!inputContext) {

        LOGI(
            "FFmpeg COPY: avformat_alloc_context() falhou"
        );

        av_freep(
            &ioContext->buffer
        );

        avio_context_free(
            &ioContext
        );

        close(sourceFd);

        return nullptr;
    }

    inputContext->pb =
        ioContext;

    inputContext->flags |=
        AVFMT_FLAG_CUSTOM_IO;

    int result =
        avformat_open_input(
            &inputContext,
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
            "FFmpeg COPY: avformat_open_input(): %s",
            errorBuffer
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    result =
        avformat_find_stream_info(
            inputContext,
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
            "FFmpeg COPY: find_stream_info(): %s",
            errorBuffer
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    int videoStreamIndex = -1;
    int audioStreamIndex = -1;

    for (
        unsigned int i = 0;
        i < inputContext->nb_streams;
        ++i
    ) {

        AVCodecParameters* parameters =
            inputContext
                ->streams[i]
                ->codecpar;

        if (
            parameters->codec_type
            == AVMEDIA_TYPE_VIDEO &&
            videoStreamIndex < 0
        ) {

            videoStreamIndex =
                static_cast<int>(i);
        }

        if (
            parameters->codec_type
            == AVMEDIA_TYPE_AUDIO &&
            audioStreamIndex < 0
        ) {

            audioStreamIndex =
                static_cast<int>(i);
        }
    }

    if (videoStreamIndex < 0) {

        LOGI(
            "FFmpeg COPY: stream de vídeo não encontrado"
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    AVStream* inputVideoStream =
        inputContext
            ->streams[videoStreamIndex];

    AVRational resultTimeBase =
        inputVideoStream->time_base;

    AVStream* inputAudioStream =
        audioStreamIndex >= 0
            ? inputContext->streams[audioStreamIndex]
            : nullptr;

    LOGI(
        "FFmpeg COPY: vídeo stream=%d codec=%s "
        "time_base=%d/%d",
        videoStreamIndex,
        avcodec_get_name(
            inputVideoStream
                ->codecpar
                ->codec_id
        ),
        inputVideoStream->time_base.num,
        inputVideoStream->time_base.den
    );

    if (inputAudioStream) {

        LOGI(
            "FFmpeg COPY: áudio stream=%d codec=%s "
            "time_base=%d/%d",
            audioStreamIndex,
            avcodec_get_name(
                inputAudioStream
                    ->codecpar
                    ->codec_id
            ),
            inputAudioStream->time_base.num,
            inputAudioStream->time_base.den
        );
    }

    AVRational frameRate =
        inputVideoStream->avg_frame_rate;

    if (
        frameRate.num <= 0 ||
        frameRate.den <= 0
    ) {
        frameRate =
            inputVideoStream->r_frame_rate;
    }

    if (
        frameRate.num <= 0 ||
        frameRate.den <= 0
    ) {

        LOGI(
            "FFmpeg COPY: frame rate inválido"
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    int64_t startVideoPts =
        av_rescale_q(
            frameA,
            AVRational{
                frameRate.den,
                frameRate.num
            },
            inputVideoStream->time_base
        );

    int64_t endVideoPts =
        av_rescale_q(
            frameB,
            AVRational{
                frameRate.den,
                frameRate.num
            },
            inputVideoStream->time_base
        );

    LOGI(
        "FFmpeg COPY: frame A=%lld B=%lld "
        "startPTS=%lld endPTS=%lld fps=%d/%d",
        static_cast<long long>(frameA),
        static_cast<long long>(frameB),
        static_cast<long long>(startVideoPts),
        static_cast<long long>(endVideoPts),
        frameRate.num,
        frameRate.den
    );

    /*
     * Procurar o keyframe mais próximo anterior a A.
     *
     * Isso é necessário para que o H.264 copiado forme
     * um stream decodificável independente.
     */
    result =
        av_seek_frame(
            inputContext,
            videoStreamIndex,
            startVideoPts,
            AVSEEK_FLAG_BACKWARD
        );

    if (result < 0) {

        LOGI(
            "FFmpeg COPY: av_seek_frame() falhou: %d",
            result
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    AVFormatContext* outputContext =
        nullptr;

    const char* outputPath =
        "/data/data/com.rec.gpiv/cache/frameA.mov";

    result =
        avformat_alloc_output_context2(
            &outputContext,
            nullptr,
            "mov",
            outputPath
        );

    if (
        result < 0 ||
        !outputContext
    ) {

        LOGI(
            "FFmpeg COPY: alloc output falhou: %d",
            result
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    AVStream* outputVideoStream =
        avformat_new_stream(
            outputContext,
            nullptr
        );

    if (!outputVideoStream) {

        LOGI(
            "FFmpeg COPY: stream de vídeo de saída falhou"
        );

        avformat_free_context(
            outputContext
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    result =
        avcodec_parameters_copy(
            outputVideoStream->codecpar,
            inputVideoStream->codecpar
        );

    if (result < 0) {

        LOGI(
            "FFmpeg COPY: copy codecpar vídeo falhou: %d",
            result
        );

        avformat_free_context(
            outputContext
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    outputVideoStream->time_base =
        inputVideoStream->time_base;

    AVStream* outputAudioStream =
        nullptr;

    if (inputAudioStream) {

        outputAudioStream =
            avformat_new_stream(
                outputContext,
                nullptr
            );

        if (!outputAudioStream) {

            LOGI(
                "FFmpeg COPY: stream de áudio de saída falhou"
            );

            avformat_free_context(
                outputContext
            );

            avformat_close_input(
                &inputContext
            );

            close(sourceFd);

            return nullptr;
        }

        result =
            avcodec_parameters_copy(
                outputAudioStream->codecpar,
                inputAudioStream->codecpar
            );

        if (result < 0) {

            LOGI(
                "FFmpeg COPY: copy codecpar áudio falhou: %d",
                result
            );

            avformat_free_context(
                outputContext
            );

            avformat_close_input(
                &inputContext
            );

            close(sourceFd);

            return nullptr;
        }

        outputAudioStream->time_base =
            inputAudioStream->time_base;
    }

    if (
        !(outputContext->oformat->flags &
          AVFMT_NOFILE)
    ) {

        result =
            avio_open(
                &outputContext->pb,
                outputPath,
                AVIO_FLAG_WRITE
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
                "FFmpeg COPY: avio_open(): %s",
                errorBuffer
            );

            avformat_free_context(
                outputContext
            );

            avformat_close_input(
                &inputContext
            );

            close(sourceFd);

            return nullptr;
        }
    }

    result =
        avformat_write_header(
            outputContext,
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
            "FFmpeg COPY: write_header(): %s",
            errorBuffer
        );

        if (
            !(outputContext->oformat->flags &
              AVFMT_NOFILE)
        ) {
            avio_closep(
                &outputContext->pb
            );
        }

        avformat_free_context(
            outputContext
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    AVPacket* packet =
        av_packet_alloc();

    if (!packet) {

        LOGI(
            "FFmpeg COPY: av_packet_alloc() falhou"
        );

        av_write_trailer(
            outputContext
        );

        if (
            !(outputContext->oformat->flags &
              AVFMT_NOFILE)
        ) {
            avio_closep(
                &outputContext->pb
            );
        }

        avformat_free_context(
            outputContext
        );

        avformat_close_input(
            &inputContext
        );

        close(sourceFd);

        return nullptr;
    }

    bool videoStarted = false;
    bool audioStarted = false;

    int videoPacketsWritten = 0;
    int audioPacketsWritten = 0;

    int64_t firstOutputVideoPts =
        AV_NOPTS_VALUE;

    int64_t firstOutputAudioPts =
        AV_NOPTS_VALUE;

    int64_t lastOutputVideoPts =
        AV_NOPTS_VALUE;

    int64_t lastOutputAudioPts =
        AV_NOPTS_VALUE;

    while (
        av_read_frame(
            inputContext,
            packet
        ) >= 0
    ) {

        if (
            packet->stream_index
            == videoStreamIndex
        ) {

            if (
                packet->pts == AV_NOPTS_VALUE
            ) {

                av_packet_unref(packet);
                continue;
            }

            if (
                packet->pts > endVideoPts
            ) {

                av_packet_unref(packet);
                break;
            }

            if (!videoStarted) {

                if (
                    !(packet->flags &
                      AV_PKT_FLAG_KEY)
                ) {

                    av_packet_unref(packet);
                    continue;
                }

                firstOutputVideoPts =
                    packet->pts;

                videoStarted = true;

                LOGI(
                    "FFmpeg COPY: primeiro vídeo "
                    "input pts=%lld key=%d",
                    static_cast<long long>(
                        packet->pts
                    ),
                    !!(
                        packet->flags &
                        AV_PKT_FLAG_KEY
                    )
                );
            }

            packet->stream_index =
                outputVideoStream->index;

            lastOutputVideoPts =
                packet->pts;

            packet->pts -=
                firstOutputVideoPts;

            if (
                packet->dts != AV_NOPTS_VALUE
            ) {
                packet->dts -=
                    firstOutputVideoPts;
            }

            result =
                av_interleaved_write_frame(
                    outputContext,
                    packet
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
                    "FFmpeg COPY: write vídeo falhou: %s",
                    errorBuffer
                );

                av_packet_unref(packet);
                break;
            }

            ++videoPacketsWritten;

            av_packet_unref(packet);

            continue;
        }

        if (
            packet->stream_index
            == audioStreamIndex &&
            outputAudioStream
        ) {

            if (
                packet->pts == AV_NOPTS_VALUE ||
                packet->duration <= 0
            ) {

                av_packet_unref(packet);
                continue;
            }

            AVRational videoTB =
                inputVideoStream->time_base;

            AVRational audioTB =
                inputAudioStream->time_base;

            int64_t audioStartPts =
                av_rescale_q(
                    startVideoPts,
                    videoTB,
                    audioTB
                );

            int64_t audioEndPts =
                av_rescale_q(
                    endVideoPts,
                    videoTB,
                    audioTB
                );

            int64_t packetEnd =
                packet->pts +
                packet->duration;

            if (
                packetEnd <= audioStartPts
            ) {

                av_packet_unref(packet);
                continue;
            }

            if (
                packet->pts >= audioEndPts
            ) {

                av_packet_unref(packet);
                continue;
            }

            if (!audioStarted) {

                firstOutputAudioPts =
                    packet->pts;

                audioStarted = true;

                LOGI(
                    "FFmpeg COPY: primeiro áudio "
                    "input pts=%lld",
                    static_cast<long long>(
                        packet->pts
                    )
                );
            }

            packet->stream_index =
                outputAudioStream->index;

            lastOutputAudioPts =
                packet->pts;

            packet->pts -=
                firstOutputAudioPts;

            if (
                packet->dts != AV_NOPTS_VALUE
            ) {

                packet->dts -=
                    firstOutputAudioPts;
            }

            result =
                av_interleaved_write_frame(
                    outputContext,
                    packet
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
                    "FFmpeg COPY: write áudio falhou: %s",
                    errorBuffer
                );

                av_packet_unref(packet);
                break;
            }

            ++audioPacketsWritten;

            av_packet_unref(packet);

            continue;
        }

        av_packet_unref(packet);
    }

    av_packet_free(
        &packet
    );

    LOGI(
        "FFmpeg COPY: vídeo packets=%d áudio packets=%d",
        videoPacketsWritten,
        audioPacketsWritten
    );

    LOGI(
        "FFmpeg COPY: vídeo primeiro=%lld último=%lld",
        static_cast<long long>(
            firstOutputVideoPts
        ),
        static_cast<long long>(
            lastOutputVideoPts
        )
    );

    LOGI(
        "FFmpeg COPY: áudio primeiro=%lld último=%lld",
        static_cast<long long>(
            firstOutputAudioPts
        ),
        static_cast<long long>(
            lastOutputAudioPts
        )
    );

    result =
        av_write_trailer(
            outputContext
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
            "FFmpeg COPY: av_write_trailer(): %s",
            errorBuffer
        );
    } else {

        LOGI(
            "FFmpeg COPY: trailer OK"
        );
    }

    if (
        !(outputContext->oformat->flags &
          AVFMT_NOFILE)
    ) {

        avio_closep(
            &outputContext->pb
        );
    }

    avformat_free_context(
        outputContext
    );

    avformat_close_input(
        &inputContext
    );

    close(sourceFd);

    LOGI(
        "FFmpeg COPY: corte concluído: %s",
        outputPath
    );

    jlongArray resultArray =
        env->NewLongArray(6);

    if (resultArray) {

        jlong values[6] = {
            frameA,
            startVideoPts,
            frameB,
            endVideoPts,
            inputVideoStream->time_base.num,
            inputVideoStream->time_base.den
        };

        env->SetLongArrayRegion(
            resultArray,
            0,
            6,
            values
        );
    }

    return resultArray;
}
