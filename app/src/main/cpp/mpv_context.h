#ifndef GPIV_MPV_CONTEXT_H
#define GPIV_MPV_CONTEXT_H

struct MpvContext {
    int testValue;
};

MpvContext* mpv_context_create();

void mpv_context_destroy(
    MpvContext* context
);

#endif
