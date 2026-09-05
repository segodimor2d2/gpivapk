#include "mpv_context_stream.h"
#include "mpv_context.h"

#include <mpv/stream_cb.h>

#include <android/log.h>

#include <cerrno>
#include <cstdint>
#include <cstring>

#include <mutex>
#include <new>
#include <string>
#include <unordered_map>

#include <sys/stat.h>

#include <unistd.h>


#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print( \
        ANDROID_LOG_INFO, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGW(...) \
    __android_log_print( \
        ANDROID_LOG_WARN, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGE(...) \
    __android_log_print( \
        ANDROID_LOG_ERROR, \
        LOG_TAG, \
        __VA_ARGS__ \
    )


/* ============================================================
 * Stream aberto
 * ============================================================ */

struct MpvStreamCookie {

    int fd;
};


/* ============================================================
 * Streams aguardando open()
 * ============================================================ */

struct MpvStreamManager {

    std::mutex mutex;

    std::unordered_map<
        std::string,
        int
    > pendingFds;

    uint64_t nextId = 1;
};

static int64_t mpv_stream_read(
    void* cookie,
    char* buf,
    uint64_t nbytes
);

static int64_t mpv_stream_seek(
    void* cookie,
    int64_t offset
);

static int64_t mpv_stream_size(
    void* cookie
);

static void mpv_stream_close(
    void* cookie
);


/* ============================================================
 * OPEN
 * ============================================================ */

static int mpv_stream_open(
    void* userData,
    char* uri,
    mpv_stream_cb_info* info
)
{
    auto* context =
        static_cast<MpvContext*>(userData);

    if (!context || !uri || !info) {
        return MPV_ERROR_INVALID_PARAMETER;
    }

    auto* manager =
        context->streamManager;

    if (!manager) {
        return MPV_ERROR_GENERIC;
    }

    const std::string key(uri);

    int fd = -1;

    {
        std::lock_guard<std::mutex> lock(
            manager->mutex
        );

        auto it =
            manager->pendingFds.find(key);

        if (
            it ==
            manager->pendingFds.end()
        ) {

            LOGE(
                "STREAM: URI não encontrada: %s",
                key.c_str()
            );

            return MPV_ERROR_LOADING_FAILED;
        }

        fd = it->second;

        manager->pendingFds.erase(it);
    }

    auto* cookie =
        new (std::nothrow) MpvStreamCookie;

    if (!cookie) {

        ::close(fd);

        return MPV_ERROR_NOMEM;
    }

    cookie->fd = fd;

    info->cookie = cookie;

    info->read_fn =
        mpv_stream_read;

    info->seek_fn =
        mpv_stream_seek;

    info->size_fn =
        mpv_stream_size;

    info->close_fn =
        mpv_stream_close;

    info->cancel_fn =
        nullptr;

    LOGI(
        "STREAM: open(%s) -> fd=%d",
        key.c_str(),
        fd
    );

    return 0;
}


/* ============================================================
 * READ
 * ============================================================ */

static int64_t mpv_stream_read(
    void* cookie,
    char* buffer,
    uint64_t nbytes
)
{
    auto* stream =
        static_cast<MpvStreamCookie*>(cookie);

    if (!stream || stream->fd < 0) {
        return -1;
    }

    while (true) {

        ssize_t result =
            ::read(
                stream->fd,
                buffer,
                static_cast<size_t>(nbytes)
            );

        if (result >= 0) {
            return result;
        }

        if (errno == EINTR) {
            continue;
        }

        LOGW(
            "STREAM: read(fd=%d): %s",
            stream->fd,
            std::strerror(errno)
        );

        return -1;
    }
}


/* ============================================================
 * SEEK
 * ============================================================ */

static int64_t mpv_stream_seek(
    void* cookie,
    int64_t offset
)
{
    auto* stream =
        static_cast<MpvStreamCookie*>(cookie);

    if (!stream || stream->fd < 0) {
        return MPV_ERROR_GENERIC;
    }

    off_t result =
        ::lseek(
            stream->fd,
            static_cast<off_t>(offset),
            SEEK_SET
        );

    if (result == static_cast<off_t>(-1)) {

        LOGW(
            "STREAM: seek(fd=%d): %s",
            stream->fd,
            std::strerror(errno)
        );

        return MPV_ERROR_UNSUPPORTED;
    }

    return static_cast<int64_t>(result);
}


/* ============================================================
 * SIZE
 * ============================================================ */

static int64_t mpv_stream_size(
    void* cookie
)
{
    auto* stream =
        static_cast<MpvStreamCookie*>(cookie);

    if (!stream || stream->fd < 0) {
        return MPV_ERROR_GENERIC;
    }

    struct stat st {};

    if (
        ::fstat(
            stream->fd,
            &st
        ) != 0
    ) {

        LOGW(
            "STREAM: fstat(fd=%d): %s",
            stream->fd,
            std::strerror(errno)
        );

        return MPV_ERROR_UNSUPPORTED;
    }

    return static_cast<int64_t>(
        st.st_size
    );
}


/* ============================================================
 * CLOSE
 * ============================================================ */

static void mpv_stream_close(
    void* cookie
)
{
    auto* stream =
        static_cast<MpvStreamCookie*>(cookie);

    if (!stream) {
        return;
    }

    LOGI(
        "STREAM: close(fd=%d)",
        stream->fd
    );

    if (stream->fd >= 0) {
        ::close(stream->fd);
    }

    delete stream;
}


/* ============================================================
 * INITIALIZE
 * ============================================================ */

bool mpv_context_stream_initialize(
    MpvContext* context
)
{
    if (!context || !context->mpv) {
        return false;
    }

    if (context->streamManager) {
        return true;
    }

    auto* manager =
        new (std::nothrow) MpvStreamManager;

    if (!manager) {
        return false;
    }

    context->streamManager =
        manager;

    int status =
        mpv_stream_cb_add_ro(
            context->mpv,
            "gpiv",
            context,
            mpv_stream_open
        );

    if (status < 0) {

        LOGE(
            "STREAM: mpv_stream_cb_add_ro() = %d",
            status
        );

        delete manager;

        context->streamManager =
            nullptr;

        return false;
    }

    LOGI(
        "STREAM: protocolo gpiv:// registrado"
    );

    return true;
}


/* ============================================================
 * ADD FD
 * ============================================================ */

std::string mpv_context_stream_add_fd(
    MpvContext* context,
    int fd
)
{
    if (
        !context ||
        !context->streamManager ||
        fd < 0
    ) {
        return {};
    }

    auto* manager =
        context->streamManager;

    int duplicatedFd =
        ::dup(fd);

    if (duplicatedFd < 0) {

        LOGE(
            "STREAM: dup(%d): %s",
            fd,
            std::strerror(errno)
        );

        return {};
    }

    std::string uri;

    {
        std::lock_guard<std::mutex> lock(
            manager->mutex
        );

        uri =
            "gpiv://" +
            std::to_string(
                manager->nextId++
            );

        manager->pendingFds.emplace(
            uri,
            duplicatedFd
        );
    }

    LOGI(
        "STREAM: fd=%d dup=%d -> %s",
        fd,
        duplicatedFd,
        uri.c_str()
    );

    return uri;
}


/* ============================================================
 * CANCEL
 * ============================================================ */

bool mpv_context_stream_cancel_uri(
    MpvContext* context,
    const std::string& uri
)
{
    if (
        !context ||
        !context->streamManager
    ) {
        return false;
    }

    auto* manager =
        context->streamManager;

    int fd = -1;

    {
        std::lock_guard<std::mutex> lock(
            manager->mutex
        );

        auto it =
            manager->pendingFds.find(uri);

        if (
            it ==
            manager->pendingFds.end()
        ) {
            return false;
        }

        fd = it->second;

        manager->pendingFds.erase(it);
    }

    if (fd >= 0) {
        ::close(fd);
    }

    return true;
}
