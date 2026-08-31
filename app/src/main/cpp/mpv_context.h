#ifndef GPIV_MPV_CONTEXT_H
#define GPIV_MPV_CONTEXT_H

#include <mpv/client.h>

struct MpvContext {
    mpv_handle* mpv;
};

MpvContext* mpv_context_create();

int mpv_context_initialize(
    MpvContext* context
);

void mpv_context_destroy(
    MpvContext* context
);

#endif
