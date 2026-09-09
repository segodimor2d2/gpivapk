#ifndef GPIV_MPV_CONTEXT_RENDER_H
#define GPIV_MPV_CONTEXT_RENDER_H

#include "mpv_context.h"

bool mpv_context_render_create(
    MpvContext* context
);

void mpv_context_render_destroy(
    MpvContext* context
);

bool mpv_context_render_frame(
    MpvContext* context
);

bool mpv_context_render_capture(
    MpvContext* context
);

#endif
