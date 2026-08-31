#include "mpv_context.h"

#include <new>

MpvContext* mpv_context_create()
{
    MpvContext* context =
        new (std::nothrow) MpvContext;

    if (!context) {
        return nullptr;
    }

    context->testValue = 1234;

    return context;
}

void mpv_context_destroy(
    MpvContext* context
)
{
    delete context;
}
