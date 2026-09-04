#include "mpv_context_render.h"

#include "mpv_context_egl.h"

#include <android/log.h>
#include <mpv/render_gl.h>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGE(...) \
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool create_mpv_render_context(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (!context->mpv)
        return false;

    if (context->renderContext)
        return true;

    mpv_opengl_init_params gl_init = {
        .get_proc_address =
            [](void* /* ctx */, const char* name) {
                return mpv_context_egl_get_proc_address(name);
            },
        .get_proc_address_ctx = context
    };

    mpv_render_param params[] = {
        {
            MPV_RENDER_PARAM_API_TYPE,
            const_cast<char*>(
                MPV_RENDER_API_TYPE_OPENGL
            )
        },
        {
            MPV_RENDER_PARAM_OPENGL_INIT_PARAMS,
            &gl_init
        },
        {
            MPV_RENDER_PARAM_INVALID,
            nullptr
        }
    };

    int result =
        mpv_render_context_create(
            &context->renderContext,
            context->mpv,
            params
        );

    if (result < 0) {
        LOGE(
            "MpvContext: falha ao criar render context: %d",
            result
        );

        context->renderContext = nullptr;

        return false;
    }

    LOGI(
        "MpvContext: render context criado"
    );

    return true;
}

static void destroy_mpv_render_context(
    MpvContext* context
)
{
    if (!context)
        return;

    if (context->renderContext) {

        mpv_render_context_free(
            context->renderContext
        );

        context->renderContext =
            nullptr;

        LOGI(
            "MpvContext: render context destruído"
        );
    }
}

static bool render_mpv(
    MpvContext* context,
    mpv_render_param* params
)
{
    if (!context)
        return false;

    if (!context->renderContext)
        return false;

    if (!params)
        return false;

    mpv_render_context_render(
        context->renderContext,
        params
    );

    return true;
}

static void report_mpv_swap(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->renderContext)
        return;

    mpv_render_context_report_swap(
        context->renderContext
    );
}

bool mpv_context_render_create(
    MpvContext* context
)
{
    return create_mpv_render_context(
        context
    );
}

void mpv_context_render_destroy(
    MpvContext* context
)
{
    destroy_mpv_render_context(
        context
    );
}

bool mpv_context_render_frame(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (!context->renderContext)
        return false;

    if (!context->eglInitialized)
        return false;

    if (!context->window)
        return false;

    int width =
        ANativeWindow_getWidth(
            context->window
        );

    int height =
        ANativeWindow_getHeight(
            context->window
        );

    if (width <= 0 || height <= 0)
        return false;

    mpv_opengl_fbo fbo = {
        .fbo = 0,
        .w = width,
        .h = height,
        .internal_format = 0
    };

    int flip_y = 1;

    mpv_render_param params[] = {
        {
            MPV_RENDER_PARAM_OPENGL_FBO,
            &fbo
        },
        {
            MPV_RENDER_PARAM_FLIP_Y,
            &flip_y
        },
        {
            MPV_RENDER_PARAM_INVALID,
            nullptr
        }
    };

    if (!render_mpv(context, params))
        return false;

    if (!mpv_context_egl_swap(context))
        return false;

    report_mpv_swap(context);

    return true;
}
