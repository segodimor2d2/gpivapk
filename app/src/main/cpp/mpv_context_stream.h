#ifndef GPIV_MPV_CONTEXT_STREAM_H
#define GPIV_MPV_CONTEXT_STREAM_H

#include <string>

struct MpvContext;

bool mpv_context_stream_initialize(
    MpvContext* context
);

std::string mpv_context_stream_add_fd(
    MpvContext* context,
    int fd
);

bool mpv_context_stream_cancel_uri(
    MpvContext* context,
    const std::string& uri
);

#endif
