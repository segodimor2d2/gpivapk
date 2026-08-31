#include <jni.h>
#include <android/log.h>

#include "mpv_context.h"

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)


extern "C"
JNIEXPORT jlong JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeCreate(
    JNIEnv* env,
    jobject thiz
)
{
    MpvContext* context =
        mpv_context_create();

    if (!context) {
        LOGI(
            "JNI: nativeCreate() -> NULL"
        );

        return 0;
    }

    LOGI(
        "JNI: nativeCreate() -> %p",
        static_cast<void*>(context)
    );

    return reinterpret_cast<jlong>(context);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeInitialize(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context) {
        LOGI(
            "JNI: nativeInitialize() -> NULL"
        );

        return;
    }

    int error =
        mpv_context_initialize(
            context
        );

    if (error < 0) {

        LOGI(
            "JNI: mpv_initialize() falhou: %s",
            mpv_error_string(error)
        );

        return;
    }

    LOGI(
        "JNI: mpv_initialize() OK"
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeDestroy(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context) {
        LOGI("JNI: nativeDestroy(NULL)");
        return;
    }

    LOGI(
        "JNI: nativeDestroy(%p)",
        static_cast<void*>(context)
    );

    mpv_context_destroy(context);
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeGetVersion(
    JNIEnv* env,
    jobject thiz
)
{
    return env->NewStringUTF(
        "GPIV Native 0.1"
    );
}

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

    if (!context) {
        return;
    }

    LOGI(
        "JNI: seekForward(%p, %f)",
        static_cast<void*>(context),
        seconds
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeLoad(
    JNIEnv* env,
    jobject thiz,
    jstring uri
)
{
    const char* uriString =
        env->GetStringUTFChars(
            uri,
            nullptr
        );

    if (uriString) {

        LOGI(
            "JNI: load(%s)",
            uriString
        );

        env->ReleaseStringUTFChars(
            uri,
            uriString
        );
    }
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

    if (!context) {
        return;
    }

    LOGI(
        "JNI: loadFd(%p, fd=%d)",
        static_cast<void*>(context),
        fd
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

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeLoadLocalPath(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jstring path
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {
        LOGI(
            "JNI: nativeLoadLocalPath() -> contexto inválido"
        );

        return;
    }

    const char* pathString =
        env->GetStringUTFChars(
            path,
            nullptr
        );

    if (!pathString) {
        LOGI(
            "JNI: nativeLoadLocalPath() -> string inválida"
        );

        return;
    }

    const char* command[] = {
        "loadfile",
        pathString,
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
        pathString,
        status
    );

    env->ReleaseStringUTFChars(
        path,
        pathString
    );
}
