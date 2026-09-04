#include "mpv_context_surface.h"

#include "mpv_context_egl.h"
#include "mpv_context_render.h"

#include <android/log.h>

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


static void remove_current_surface(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->window)
        return;

    LOGI(
        "MpvContext: removendo Surface anterior"
    );

    /*
     * A Surface Android pode ser destruída e recriada
     * durante a vida do player.
     *
     * Não destruímos o mpv_render_context nem o EGLContext.
     * Apenas removemos a EGLSurface associada à janela anterior.
     */

    mpv_context_egl_destroy_surface(
        context
    );

    ANativeWindow_release(
        context->window
    );

    context->window = nullptr;
    context->surfaceAvailable = false;
}


static bool acquire_surface(
    MpvContext* context,
    ANativeWindow* window
)
{
    if (!context || !window)
        return false;

    ANativeWindow_acquire(
        window
    );

    context->window =
        window;

    context->surfaceAvailable =
        true;

    LOGI(
        "MpvContext: ANativeWindow adquirido: %p",
        context->window
    );

    return true;
}


static bool setup_egl_for_surface(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (
        context->eglDisplay == EGL_NO_DISPLAY ||
        context->eglContext == EGL_NO_CONTEXT
    )
    {
        LOGI(
            "MpvContext: EGL ainda não existe, "
            "criando EGL completo"
        );

        if (!mpv_context_egl_create(context))
        {
            LOGE(
                "MpvContext: falha ao criar EGL"
            );

            return false;
        }

        LOGI(
            "MpvContext: EGL inicializado com sucesso"
        );
    }
    else
    {
        LOGI(
            "MpvContext: reutilizando EGL existente"
        );

        if (!mpv_context_egl_create_surface(context))
        {
            LOGE(
                "MpvContext: falha ao recriar EGLSurface"
            );

            return false;
        }

        LOGI(
            "MpvContext: EGLSurface recriada com sucesso"
        );
    }

    return true;
}


static void release_surface(
    MpvContext* context
)
{
    if (!context)
        return;

    if (context->window) {
        ANativeWindow_release(
            context->window
        );

        context->window = nullptr;
    }

    context->surfaceAvailable = false;
}


void mpv_context_surface_set(
    MpvContext* context,
    ANativeWindow* window
)
{
    if (!context)
        return;

    LOGI(
        "MpvContext: set_surface()"
    );

    /*
     * ========================================================
     * REMOVER SURFACE ANTERIOR
     * ========================================================
     */

    remove_current_surface(
        context
    );

    /*
     * ========================================================
     * NENHUMA SURFACE
     * ========================================================
     */

    if (!window)
    {
        LOGI(
            "MpvContext: Surface removida"
        );

        return;
    }

    /*
     * ========================================================
     * ADQUIRIR ANativeWindow
     * ========================================================
     */

    if (!acquire_surface(
            context,
            window))
    {
        LOGE(
            "MpvContext: falha ao adquirir ANativeWindow"
        );

        return;
    }

    /*
     * ========================================================
     * EGL
     * ========================================================
     */

    if (!setup_egl_for_surface(
            context))
    {
        release_surface(
            context
        );

        return;
    }

    /*
     * ========================================================
     * MPV RENDER CONTEXT
     * ========================================================
     */

    if (context->renderContext)
    {
        LOGI(
            "MpvContext: reutilizando mpv_render_context"
        );

        return;
    }

    if (!mpv_context_render_create(
            context))
    {
        mpv_context_egl_destroy(
            context
        );

        release_surface(
            context
        );

        return;
    }
}


void mpv_context_surface_release(
    MpvContext* context
)
{
    release_surface(
        context
    );
}
