#ifndef GPIV_MPV_CONTEXT_EGL_H
#define GPIV_MPV_CONTEXT_EGL_H

#include "mpv_context.h"

bool mpv_context_egl_create(
    MpvContext* context
);

void mpv_context_egl_destroy(
    MpvContext* context
);

bool mpv_context_egl_create_surface(
    MpvContext* context
);

void mpv_context_egl_destroy_surface(
    MpvContext* context
);

bool mpv_context_egl_swap(
    MpvContext* context
);

void* mpv_context_egl_get_proc_address(
    const char* name
);

#endif
