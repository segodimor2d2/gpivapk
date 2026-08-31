#include "mpv_context.h"

#include <android/log.h>

#include <new>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

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

        if (event->event_id == MPV_EVENT_END_FILE) {

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
                    mpv_error_string(endFile->error)
                );
            }
        }

        if (
            event->event_id ==
            MPV_EVENT_SHUTDOWN
        ) {
            break;
        }
    }

    LOGI("event loop: thread finalizado");
}

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

    context->eventLoopRunning = false;

    return context;
}

int mpv_context_initialize(
    MpvContext* context
)
{
    if (!context || !context->mpv) {
        return -1;
    }

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

    status =
        mpv_initialize(
            context->mpv
        );

    if (status < 0) {
        return status;
    }

    context->eventLoopRunning = true;

    context->eventThread =
        std::thread(
            mpv_event_loop,
            context
        );

    return 0;
}

void mpv_context_destroy(
    MpvContext* context
)
{
    if (!context) {
        return;
    }

    if (context->mpv) {

        context->eventLoopRunning = false;

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
