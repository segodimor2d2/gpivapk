#include "mpv_context.h"

#include <new>

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

    return context;
}

void mpv_context_destroy(
    MpvContext* context
)
{
    if (!context) {
        return;
    }

    if (context->mpv) {
        mpv_terminate_destroy(
            context->mpv
        );

        context->mpv = nullptr;
    }

    delete context;
}

int mpv_context_initialize(
    MpvContext* context
)
{
    if (!context || !context->mpv) {
        return -1;
    }

    return mpv_initialize(
        context->mpv
    );
}
