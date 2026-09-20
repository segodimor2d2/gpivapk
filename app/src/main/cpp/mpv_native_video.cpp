#include <jni.h>
#include <android/log.h>

#include "mpv_context.h"

#include <string>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeChangeZoom(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble amount
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeChangeZoom() -> contexto inválido"
        );

        return;
    }

    std::string value =
        std::to_string(amount);

    const char* command[] = {
        "add",
        "video-zoom",
        value.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: video-zoom %+f -> %d",
        amount,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeResetView(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeResetView() -> contexto inválido"
        );

        return;
    }

    const char* zoomCommand[] = {
        "set", "video-zoom", "0", nullptr
    };
    const char* panXCommand[] = {
        "set", "video-pan-x", "0", nullptr
    };
    const char* panYCommand[] = {
        "set", "video-pan-y", "0", nullptr
    };
    const char* rotateCommand[] = {
        "set", "video-rotate", "0", nullptr
    };

    mpv_command(context->mpv, zoomCommand);
    mpv_command(context->mpv, panXCommand);
    mpv_command(context->mpv, panYCommand);
    mpv_command(context->mpv, rotateCommand);

    LOGI(
        "JNI: resetView()"
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeRotateClockwise(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeRotateClockwise() -> contexto inválido"
        );

        return;
    }

    const char* command[] = {
        "cycle-values",
        "video-rotate",
        "90",
        "180",
        "270",
        "0",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: rotate clockwise -> %d",
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativePan(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble x,
    jdouble y
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativePan() -> contexto inválido"
        );

        return;
    }

    std::string xValue =
        std::to_string(x);
    std::string yValue =
        std::to_string(y);

    const char* commandX[] = {
        "add", "video-pan-x", xValue.c_str(), nullptr
    };
    const char* commandY[] = {
        "add", "video-pan-y", yValue.c_str(), nullptr
    };

    int statusX = mpv_command(context->mpv, commandX);
    int statusY = mpv_command(context->mpv, commandY);

    LOGI(
        "JNI: pan x=%f y=%f -> %d / %d",
        x,
        y,
        statusX,
        statusY
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeResetVideoAdjustments(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeResetVideoAdjustments() -> contexto inválido"
        );

        return;
    }

    const char* brightnessCommand[] = {
        "set", "brightness", "0", nullptr
    };
    const char* contrastCommand[] = {
        "set", "contrast", "0", nullptr
    };
    const char* gammaCommand[] = {
        "set", "gamma", "0", nullptr
    };
    const char* saturationCommand[] = {
        "set", "saturation", "0", nullptr
    };

    mpv_command(context->mpv, brightnessCommand);
    mpv_command(context->mpv, contrastCommand);
    mpv_command(context->mpv, gammaCommand);
    mpv_command(context->mpv, saturationCommand);

    LOGI(
        "JNI: resetVideoAdjustments()"
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeChangeSaturation(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble amount
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeChangeSaturation() -> contexto inválido"
        );

        return;
    }

    std::string value =
        std::to_string(amount);

    const char* command[] = {
        "add",
        "saturation",
        value.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: saturation %+f -> %d",
        amount,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeChangeBrightness(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble amount
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeChangeBrightness() -> contexto inválido"
        );

        return;
    }

    std::string value =
        std::to_string(amount);

    const char* command[] = {
        "add",
        "brightness",
        value.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: brightness %+f -> %d",
        amount,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeChangeContrast(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble amount
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeChangeContrast() -> contexto inválido"
        );

        return;
    }

    std::string value =
        std::to_string(amount);

    const char* command[] = {
        "add",
        "contrast",
        value.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: contrast %+f -> %d",
        amount,
        status
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeChangeGamma(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jdouble amount
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !context->mpv) {

        LOGI(
            "JNI: nativeChangeGamma() -> contexto inválido"
        );

        return;
    }

    std::string value =
        std::to_string(amount);

    const char* command[] = {
        "add",
        "gamma",
        value.c_str(),
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: gamma %+f -> %d",
        amount,
        status
    );
}
