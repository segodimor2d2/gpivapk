#ifndef GPIV_MPV_CONTEXT_SURFACE_H
#define GPIV_MPV_CONTEXT_SURFACE_H

#include "mpv_context.h"

void mpv_context_surface_set(
    MpvContext* context,
    ANativeWindow* window
);

void mpv_context_surface_release(
    MpvContext* context
);

#endif
