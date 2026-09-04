#ifndef GPIV_MPV_CONTEXT_MPV_H
#define GPIV_MPV_CONTEXT_MPV_H

#include "mpv_context.h"

bool mpv_context_mpv_configure(
    MpvContext* context
);

bool mpv_context_mpv_initialize(
    MpvContext* context
);

int mpv_context_mpv_observe_properties(
    MpvContext* context
);

void mpv_context_mpv_destroy(
    MpvContext* context
);

#endif
