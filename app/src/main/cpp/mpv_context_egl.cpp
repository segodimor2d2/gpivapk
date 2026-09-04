#include "mpv_context_egl.h"

#include <android/log.h>

#include <EGL/egl.h>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print( \
        ANDROID_LOG_INFO, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGE(...) \
    __android_log_print( \
        ANDROID_LOG_ERROR, \
        LOG_TAG, \
        __VA_ARGS__ \
    )


/* ============================================================
 * OPENGL / EGL
 * ============================================================ */

void* mpv_context_egl_get_proc_address(
    const char* name
)
{
    if (!name) {
        return nullptr;
    }

    return reinterpret_cast<void*>(
        eglGetProcAddress(name)
    );
}


/* ============================================================
 * EGL ERROR
 * ============================================================ */

static void log_egl_error(
    const char* operation
)
{
    EGLint error = eglGetError();

    LOGE(
        "EGL: %s falhou: 0x%04x",
        operation,
        error
    );
}


/* ============================================================
 * CURRENT CONTEXT
 * ============================================================ */

static bool clear_egl_current(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (
        context->eglDisplay == EGL_NO_DISPLAY
    ) {
        return false;
    }

    if (
        !eglMakeCurrent(
            context->eglDisplay,
            EGL_NO_SURFACE,
            EGL_NO_SURFACE,
            EGL_NO_CONTEXT
        )
    ) {
        log_egl_error(
            "eglMakeCurrent(clear)"
        );

        return false;
    }

    LOGI(
        "EGL: contexto desvinculado"
    );

    return true;
}


static bool make_egl_current(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (
        context->eglDisplay == EGL_NO_DISPLAY ||
        context->eglSurface == EGL_NO_SURFACE ||
        context->eglContext == EGL_NO_CONTEXT
    ) {
        LOGE(
            "EGL: estado inválido para eglMakeCurrent"
        );

        return false;
    }

    if (
        !eglMakeCurrent(
            context->eglDisplay,
            context->eglSurface,
            context->eglSurface,
            context->eglContext
        )
    ) {
        log_egl_error(
            "eglMakeCurrent"
        );

        return false;
    }

    LOGI(
        "EGL: surface vinculada ao EGLContext"
    );

    return true;
}


/* ============================================================
 * EGL SURFACE
 * ============================================================ */

bool mpv_context_egl_create_surface(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (
        context->eglDisplay == EGL_NO_DISPLAY ||
        context->eglContext == EGL_NO_CONTEXT ||
        !context->eglConfig ||
        !context->window
    ) {
        LOGE(
            "EGL: não é possível recriar EGLSurface"
        );

        return false;
    }

    context->eglSurface =
        eglCreateWindowSurface(
            context->eglDisplay,
            context->eglConfig,
            context->window,
            nullptr
        );

    if (
        context->eglSurface == EGL_NO_SURFACE
    ) {
        log_egl_error(
            "eglCreateWindowSurface"
        );

        return false;
    }

    LOGI(
        "EGL: nova surface criada"
    );

    if (!make_egl_current(context)) {

        eglDestroySurface(
            context->eglDisplay,
            context->eglSurface
        );

        context->eglSurface =
            EGL_NO_SURFACE;

        return false;
    }

    return true;
}


void mpv_context_egl_destroy_surface(
    MpvContext* context
)
{
    if (!context)
        return;

    if (
        context->eglDisplay != EGL_NO_DISPLAY &&
        context->eglSurface != EGL_NO_SURFACE
    ) {

        clear_egl_current(context);

        eglDestroySurface(
            context->eglDisplay,
            context->eglSurface
        );

        context->eglSurface =
            EGL_NO_SURFACE;

        LOGI(
            "EGL: surface destruída"
        );
    }
}


/* ============================================================
 * EGL CONTEXT
 * ============================================================ */

static bool create_egl_context(
    MpvContext* context
)
{
    if (!context)
        return false;

    const EGLint contextAttributes[] = {
        EGL_CONTEXT_CLIENT_VERSION,
        2,
        EGL_NONE
    };

    context->eglContext =
        eglCreateContext(
            context->eglDisplay,
            context->eglConfig,
            EGL_NO_CONTEXT,
            contextAttributes
        );

    if (
        context->eglContext == EGL_NO_CONTEXT
    ) {
        log_egl_error(
            "eglCreateContext"
        );

        return false;
    }

    LOGI(
        "EGL: context OK"
    );

    return true;
}


static void destroy_egl_context(
    MpvContext* context
)
{
    if (!context)
        return;

    if (
        context->eglContext != EGL_NO_CONTEXT &&
        context->eglDisplay != EGL_NO_DISPLAY
    ) {

        eglDestroyContext(
            context->eglDisplay,
            context->eglContext
        );

        context->eglContext =
            EGL_NO_CONTEXT;

        LOGI(
            "EGL: context destruído"
        );
    }
}


/* ============================================================
 * EGL DISPLAY
 * ============================================================ */

static bool create_egl_display(
    MpvContext* context
)
{
    if (!context)
        return false;

    context->eglDisplay =
        eglGetDisplay(
            EGL_DEFAULT_DISPLAY
        );

    if (
        context->eglDisplay == EGL_NO_DISPLAY
    ) {
        log_egl_error(
            "eglGetDisplay"
        );

        return false;
    }

    EGLint major = 0;
    EGLint minor = 0;

    if (
        !eglInitialize(
            context->eglDisplay,
            &major,
            &minor
        )
    ) {
        log_egl_error(
            "eglInitialize"
        );

        context->eglDisplay =
            EGL_NO_DISPLAY;

        return false;
    }

    LOGI(
        "EGL: display OK (%d.%d)",
        major,
        minor
    );

    return true;
}


static void terminate_egl_display(
    MpvContext* context
)
{
    if (!context)
        return;

    if (
        context->eglDisplay != EGL_NO_DISPLAY
    ) {

        eglTerminate(
            context->eglDisplay
        );

        context->eglDisplay =
            EGL_NO_DISPLAY;

        LOGI(
            "EGL: display terminado"
        );
    }
}


/* ============================================================
 * EGL CONFIG
 * ============================================================ */

static bool choose_egl_config(
    MpvContext* context
)
{
    if (!context)
        return false;

    const EGLint configAttributes[] = {

        EGL_SURFACE_TYPE,
        EGL_WINDOW_BIT,

        EGL_RENDERABLE_TYPE,
        EGL_OPENGL_ES2_BIT,

        EGL_RED_SIZE,
        8,

        EGL_GREEN_SIZE,
        8,

        EGL_BLUE_SIZE,
        8,

        EGL_ALPHA_SIZE,
        8,

        EGL_NONE
    };

    EGLint numConfigs = 0;

    if (
        !eglChooseConfig(
            context->eglDisplay,
            configAttributes,
            &context->eglConfig,
            1,
            &numConfigs
        )
    ) {
        log_egl_error(
            "eglChooseConfig"
        );

        return false;
    }

    if (numConfigs <= 0) {

        LOGE(
            "EGL: nenhum EGLConfig encontrado"
        );

        return false;
    }

    LOGI(
        "EGL: config OK"
    );

    return true;
}


/* ============================================================
 * EGL API
 * ============================================================ */

static bool bind_egl_api()
{
    if (
        !eglBindAPI(
            EGL_OPENGL_ES_API
        )
    ) {

        log_egl_error(
            "eglBindAPI"
        );

        return false;
    }

    LOGI(
        "EGL: OpenGL ES API OK"
    );

    return true;
}


/* ============================================================
 * EGL CREATE / DESTROY
 * ============================================================ */

bool mpv_context_egl_create(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (!context->window) {

        LOGE(
            "EGL: não existe ANativeWindow"
        );

        return false;
    }

    if (!create_egl_display(context)) {
        return false;
    }

    if (!bind_egl_api()) {

        mpv_context_egl_destroy(context);

        return false;
    }

    if (!choose_egl_config(context)) {

        mpv_context_egl_destroy(context);

        return false;
    }

    if (!create_egl_context(context)) {

        mpv_context_egl_destroy(context);

        return false;
    }

    if (!mpv_context_egl_create_surface(context)) {

        mpv_context_egl_destroy(context);

        return false;
    }

    context->eglInitialized =
        true;

    LOGI(
        "EGL: inicialização completa"
    );

    return true;
}


void mpv_context_egl_destroy(
    MpvContext* context
)
{
    if (!context)
        return;

    mpv_context_egl_destroy_surface(context);

    destroy_egl_context(context);

    terminate_egl_display(context);

    context->eglConfig =
        nullptr;

    context->eglInitialized =
        false;

    LOGI(
        "EGL: destruído"
    );
}


/* ============================================================
 * SWAP
 * ============================================================ */

bool mpv_context_egl_swap(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (
        context->eglDisplay == EGL_NO_DISPLAY ||
        context->eglSurface == EGL_NO_SURFACE
    ) {
        return false;
    }

    if (
        !eglSwapBuffers(
            context->eglDisplay,
            context->eglSurface
        )
    ) {
        log_egl_error(
            "eglSwapBuffers"
        );

        return false;
    }

    return true;
}
