#include <jni.h>
#include <android/log.h>

#include "mpv_context.h"

#include <string>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSetVolume(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble value
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeSetVolume() -> contexto inválido"
        );

        return;
    }

    std::string volume =
        std::to_string(value);

    const char* command[] = {
        "set",
        "volume",
        volume.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: set volume %.1f -> %d",
        value,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeMute(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {
        LOGI(
            "JNI: nativeMute() -> contexto inválido"
        );
        return;
    }

    const char* command[] = {
        "cycle",
        "mute",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mute -> %d",
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeFrameForward(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint frames
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeFrameForward() -> contexto inválido"
        );

        return;
    }

    std::string frameCount = std::to_string(frames);

    const char* command[] = {
        "frame-step",
        frameCount.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(frame-step %d) -> %d",
        frames,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeFrameBackward(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint frames
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeFrameBackward() -> contexto inválido"
        );

        return;
    }

    std::string frameCount = std::to_string(-frames);

    const char* command[] = {
        "frame-step",
        frameCount.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(frame-step %d) -> %d",
        -frames,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeScreenshot(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {
        LOGI(
            "JNI: nativeScreenshot() -> contexto inválido"
        );
        return;
    }

    LOGI(
        "JNI: screenshot(%p)",
        static_cast<void*>(context)
    );

    context->screenshotRequested = true;

    LOGI(
        "JNI: screenshot solicitado"
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeScreenshotMpv(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {
        LOGI("JNI: nativeScreenshotMpv() -> contexto inválido");
        return;
    }

    LOGI(
        "JNI: screenshot MPV(%p)",
        static_cast<void*>(context)
    );

    const char* command[] = {
        "screenshot",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(screenshot) -> %d",
        status
    );
}
