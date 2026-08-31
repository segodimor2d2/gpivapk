#include "mpv_context.h"

#include <android/log.h>

#include <new>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


/* ============================================================
 * IDENTIFICADORES DAS PROPRIEDADES OBSERVADAS
 * ============================================================ */

#define PROPERTY_TIME_POS   1
#define PROPERTY_DURATION   2
#define PROPERTY_PAUSE      3
#define PROPERTY_FILENAME   4


/* ============================================================
 * EVENT LOOP
 * ============================================================ */

static void mpv_event_loop(
    MpvContext* context
)
{
    LOGI("event loop: thread iniciado");

    while (
        context->eventLoopRunning.load()
    ) {

        mpv_event* event =
            mpv_wait_event(
                context->mpv,
                -1.0
            );

        if (!event) {
            continue;
        }

        LOGI(
            "event loop: event_id=%d name=%s",
            event->event_id,
            mpv_event_name(event->event_id)
        );


        /* ====================================================
         * PROPERTY CHANGE
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_PROPERTY_CHANGE
        ) {

            mpv_event_property* property =
                static_cast<mpv_event_property*>(
                    event->data
                );

            if (!property) {
                continue;
            }

            if (!property->name) {
                continue;
            }


            /* ================================================
             * PROPRIEDADE DOUBLE
             * ================================================ */

            if (
                property->format ==
                MPV_FORMAT_DOUBLE
            ) {

                if (!property->data) {
                    LOGI(
                        "PROPERTY: %s -> unavailable",
                        property->name
                    );

                    continue;
                }

                double value =
                    *static_cast<double*>(
                        property->data
                    );

                LOGI(
                    "PROPERTY: %s = %f",
                    property->name,
                    value
                );

                continue;
            }


            /* ================================================
             * PROPRIEDADE FLAG
             * ================================================ */

            if (
                property->format ==
                MPV_FORMAT_FLAG
            ) {

                if (!property->data) {
                    LOGI(
                        "PROPERTY: %s -> unavailable",
                        property->name
                    );

                    continue;
                }

                int value =
                    *static_cast<int*>(
                        property->data
                    );

                LOGI(
                    "PROPERTY: %s = %s",
                    property->name,
                    value ? "true" : "false"
                );

                continue;
            }


            /* ================================================
             * PROPRIEDADE STRING
             * ================================================ */

            if (
                property->format ==
                MPV_FORMAT_STRING
            ) {

                if (!property->data) {
                    LOGI(
                        "PROPERTY: %s -> unavailable",
                        property->name
                    );

                    continue;
                }

                char* value =
                    *static_cast<char**>(
                        property->data
                    );

                if (!value) {
                    LOGI(
                        "PROPERTY: %s = NULL",
                        property->name
                    );

                    continue;
                }

                LOGI(
                    "PROPERTY: %s = %s",
                    property->name,
                    value
                );

                continue;
            }


            /* ================================================
             * OUTRO FORMATO
             * ================================================ */

            LOGI(
                "PROPERTY: %s format=%d",
                property->name,
                static_cast<int>(
                    property->format
                )
            );
        }


        /* ====================================================
         * END FILE
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_END_FILE
        ) {

            mpv_event_end_file* endFile =
                static_cast<mpv_event_end_file*>(
                    event->data
                );

            if (endFile) {

                LOGI(
                    "END_FILE: reason=%d error=%d",
                    endFile->reason,
                    endFile->error
                );

                LOGI(
                    "END_FILE: error_string=%s",
                    mpv_error_string(
                        endFile->error
                    )
                );
            }
        }


        /* ====================================================
         * SHUTDOWN
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_SHUTDOWN
        ) {
            break;
        }
    }

    LOGI(
        "event loop: thread finalizado"
    );
}


/* ============================================================
 * CRIAÇÃO
 * ============================================================ */

MpvContext* mpv_context_create()
{
    MpvContext* context =
        new (std::nothrow) MpvContext;

    if (!context) {
        return nullptr;
    }

    context->mpv =
        mpv_create();

    if (!context->mpv) {
        delete context;
        return nullptr;
    }

    context->eventLoopRunning =
        false;

    return context;
}


/* ============================================================
 * INICIALIZAÇÃO
 * ============================================================ */

int mpv_context_initialize(
    MpvContext* context
)
{
    if (
        !context ||
        !context->mpv
    ) {
        return -1;
    }


    /* ========================================================
     * CONFIGURAÇÃO DE TESTE
     * ======================================================== */

    LOGI(
        "mpv: configurando vo=null para teste"
    );

    int status =
        mpv_set_option_string(
            context->mpv,
            "vo",
            "null"
        );

    if (status < 0) {
        return status;
    }


    /* ========================================================
     * INICIALIZA MPV
     * ======================================================== */

    status =
        mpv_initialize(
            context->mpv
        );

    if (status < 0) {
        return status;
    }


    /* ========================================================
     * OBSERVA PROPRIEDADES
     * ======================================================== */

    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_TIME_POS,
            "time-pos",
            MPV_FORMAT_DOUBLE
        );

    if (status < 0) {

        LOGI(
            "mpv_observe_property(time-pos) falhou: %s",
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
            "mpv_observe_property(duration) falhou: %s",
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
            "mpv_observe_property(pause) falhou: %s",
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
            "mpv_observe_property(filename) falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    /* ========================================================
     * INICIA EVENT LOOP
     * ======================================================== */

    context->eventLoopRunning =
        true;

    context->eventThread =
        std::thread(
            mpv_event_loop,
            context
        );

    return 0;
}


/* ============================================================
 * DESTRUIÇÃO
 * ============================================================ */

void mpv_context_destroy(
    MpvContext* context
)
{
    if (!context) {
        return;
    }

    if (context->mpv) {

        context->eventLoopRunning =
            false;

        mpv_wakeup(
            context->mpv
        );

        if (
            context->eventThread.joinable()
        ) {
            context->eventThread.join();
        }

        mpv_terminate_destroy(
            context->mpv
        );

        context->mpv = nullptr;
    }

    delete context;
}
