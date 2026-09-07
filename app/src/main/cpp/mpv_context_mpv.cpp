#include "mpv_context_mpv.h"

#include <android/log.h>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print( \
        ANDROID_LOG_INFO, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGW(...) \
    __android_log_print( \
        ANDROID_LOG_WARN, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGE(...) \
    __android_log_print( \
        ANDROID_LOG_ERROR, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define PROPERTY_TIME_POS   1
#define PROPERTY_DURATION   2
#define PROPERTY_PAUSE      3
#define PROPERTY_FILENAME   4


bool mpv_context_mpv_configure(
    MpvContext* context
)
{
    if (!context || !context->mpv)
        return false;

    /*
     * Neste projeto usamos o render API do libmpv.
     *
     * Portanto não queremos que o mpv tente selecionar
     * automaticamente uma saída de vídeo como mediacodec_embed
     * antes de existir uma Surface.
     */

    int status =
        mpv_set_option_string(
            context->mpv,
            "vo",
            "libmpv"
        );

    if (status < 0) {
        LOGI(
            "mpv_set_option_string(vo) falhou: %s",
            mpv_error_string(status)
        );

        return false;
    }

    status =
        mpv_set_option_string(
            context->mpv,
            "loop-file",
            "yes"
        );

    if (status < 0) {

        LOGI(
            "mpv_set_option_string(loop-file) falhou: %s",
            mpv_error_string(status)
        );

        return false;
    }

    if (!context->screenshotDirectory.empty()) {

        status =
            mpv_set_option_string(
                context->mpv,
                "screenshot-dir",
                context->screenshotDirectory.c_str()
            );

        if (status < 0) {

            LOGI(
                "mpv_set_option_string(screenshot-dir) falhou: %s",
                mpv_error_string(status)
            );

            return false;
        }

        LOGI(
            "mpv: screenshot-dir = %s",
            context->screenshotDirectory.c_str()
        );
    }

    return true;
}


bool mpv_context_mpv_initialize(
    MpvContext* context
)
{
    if (!context || !context->mpv)
        return false;

    LOGI(
        "mpv: mpv_initialize()"
    );

    int status =
        mpv_initialize(
            context->mpv
        );

    if (status < 0) {
        LOGI(
            "mpv_initialize() falhou: %s",
            mpv_error_string(status)
        );

        return false;
    }

    LOGI(
        "mpv: inicializado"
    );

    return true;
}


int mpv_context_mpv_observe_properties(
    MpvContext* context
)
{
    if (!context || !context->mpv)
        return -1;

    int status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_TIME_POS,
            "time-pos",
            MPV_FORMAT_DOUBLE
        );

    if (status < 0) {
        LOGI(
            "mpv_observe_property(time-pos) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_DURATION,
            "duration",
            MPV_FORMAT_DOUBLE
        );

    if (status < 0) {
        LOGI(
            "mpv_observe_property(duration) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_PAUSE,
            "pause",
            MPV_FORMAT_FLAG
        );

    if (status < 0) {
        LOGI(
            "mpv_observe_property(pause) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_FILENAME,
            "filename",
            MPV_FORMAT_STRING
        );

    if (status < 0) {
        LOGI(
            "mpv_observe_property(filename) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }

    return 0;
}


void mpv_context_mpv_destroy(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->mpv)
        return;

    LOGI(
        "mpv: destruindo mpv"
    );

    mpv_terminate_destroy(
        context->mpv
    );

    context->mpv =
        nullptr;
}
