#include <jni.h>
#include <android/log.h>

#include "mpv_context.h"
#include "mpv_context_stream.h"

#include <string>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSeekForward(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble seconds
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeSeekForward() -> contexto inválido"
        );

        return;
    }

    LOGI(
        "JNI: seekForward(%p, %f)",
        static_cast<void*>(context),
        seconds
    );

    std::string amount =
        std::to_string(seconds);

    const char* command[] = {
        "seek",
        amount.c_str(),
        "relative",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(seek +%f relative) -> %d",
        seconds,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSeekBackward(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble seconds
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeSeekBackward() -> contexto inválido"
        );

        return;
    }

    LOGI(
        "JNI: seekBackward(%p, %f)",
        static_cast<void*>(context),
        seconds
    );

    std::string amount =
        std::to_string(-seconds);

    const char* command[] = {
        "seek",
        amount.c_str(),
        "relative",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(seek %f relative) -> %d",
        -seconds,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSeekTo(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble position
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeSeekTo() -> contexto inválido"
        );

        return;
    }

    LOGI(
        "JNI: seekTo(%p, %f)",
        static_cast<void*>(context),
        position
    );

    std::string amount =
        std::to_string(position);

    const char* command[] = {
        "seek",
        amount.c_str(),
        "absolute",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(seek %f absolute) -> %d",
        position,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeLoadFd(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint fd
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {
        LOGI("JNI: loadFd() -> contexto inválido");
        return;
    }

    if (fd < 0) {
        LOGI("JNI: loadFd() -> fd inválido: %d", fd);
        return;
    }

    std::string uri =
        mpv_context_stream_add_fd(
            context,
            fd
        );

    if (uri.empty()) {
        LOGI("JNI: loadFd() -> falha ao registrar fd");
        return;
    }

    LOGI(
        "JNI: loadFd(fd=%d) -> %s",
        fd,
        uri.c_str()
    );

    const char* command[] = {
        "loadfile",
        uri.c_str(),
        "replace",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: loadfile(%s) -> %d",
        uri.c_str(),
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSetPause(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jboolean paused
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeSetPause() -> contexto inválido"
        );

        return;
    }

    const char* value =
        paused ? "yes" : "no";

    const char* command[] = {
        "set",
        "pause",
        value,
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(set pause %s) -> %d",
        value,
        status
    );
}
