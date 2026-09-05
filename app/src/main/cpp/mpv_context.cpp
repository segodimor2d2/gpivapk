#include "mpv_context.h"
#include "mpv_context_egl.h"
#include "mpv_context_render.h"
#include "mpv_context_surface.h"
#include "mpv_context_events.h"
#include "mpv_context_mpv.h"
#include "mpv_context_jni.h"
#include "mpv_context_stream.h"

#include <android/log.h>
#include <new>

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
        "GPIV_NATIVE", \
        __VA_ARGS__ \
    )

#define LOGE(...) \
    __android_log_print( \
        ANDROID_LOG_ERROR, \
        LOG_TAG, \
        __VA_ARGS__ \
    )


bool mpv_context_render(
    MpvContext* context
)
{
    return mpv_context_render_frame(
        context
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

        LOGI(
            "MpvContext: falha ao alocar contexto"
        );

        return nullptr;
    }


    context->mpv = mpv_create();


    mpv_request_log_messages(
        context->mpv,
        "info"
    );


    if (!context->mpv) {

        LOGI(
            "MpvContext: mpv_create() falhou"
        );

        delete context;

        return nullptr;
    }


    context->renderContext = nullptr;

    context->streamManager = nullptr;

    context->eglDisplay = EGL_NO_DISPLAY;

    context->eglConfig = nullptr;

    context->eglContext = EGL_NO_CONTEXT;

    context->eglSurface = EGL_NO_SURFACE;

    context->eglInitialized = false;

    context->eventLoopRunning = false;

    context->javaVm = nullptr;

    context->nativeObject = nullptr;

    context->onDoubleProperty = nullptr;

    context->onBooleanProperty = nullptr;

    context->onStringProperty = nullptr;

    context->onLoadingChanged = nullptr;

    context->window = nullptr;

    context->surfaceAvailable = false;


    LOGI(
        "MpvContext: criado sem Surface"
    );


    LOGI(
        "MpvContext: renderContext=null"
    );


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
     * CONFIGURA MPV
     * ======================================================== */

    if (!mpv_context_mpv_configure(context))
        return -1;


    /* ========================================================
     * INICIALIZA MPV
     * ======================================================== */

    if (!mpv_context_mpv_initialize(context))
        return -1;

    if (!mpv_context_stream_initialize(context))
        return -1;

    /* ========================================================
     * OBSERVA PROPRIEDADES
     * ======================================================== */

    int status =
        mpv_context_mpv_observe_properties(context);

    if (status < 0)
        return status;


    /* ========================================================
     * INICIA EVENT LOOP
     * ======================================================== */

    if (!mpv_context_events_start(context))
        return -1;


    LOGI(
        "MpvContext: inicialização concluída"
    );


    return 0;
}

/* ============================================================
 * SURFACE ANDROID
 * ============================================================ */

void mpv_context_set_surface(
    MpvContext* context,
    ANativeWindow* window
)
{
    mpv_context_surface_set(
        context,
        window
    );
}

/* ============================================================
 * DESTRUIÇÃO
 * ============================================================ */

void mpv_context_destroy(
    MpvContext* context
)
{
    if (!context)
        return;

    /* ========================================================
     * EVENT LOOP
     * ======================================================== */

    mpv_context_events_stop(context);


    /* ========================================================
     * MPV RENDER CONTEXT
     * ======================================================== */

    mpv_context_render_destroy(context);

    /* ========================================================
     * MPV
     * ======================================================== */

    mpv_context_mpv_destroy(context);

    /* ========================================================
     * REFERÊNCIA GLOBAL KOTLIN
     * ======================================================== */

    mpv_context_jni_release_reference(context);

    /* ========================================================
     * EGL
     * ======================================================== */

    mpv_context_egl_destroy(context);

    /* ========================================================
     * ANDROID SURFACE
     * ======================================================== */

    mpv_context_surface_release(
        context
    );

    LOGI(
        "MpvContext: destruído"
    );

    delete context;
}


