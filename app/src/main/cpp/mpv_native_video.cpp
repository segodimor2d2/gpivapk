#include <jni.h>
#include <android/log.h>

#include "mpv_context.h"

#include <string>
#include <algorithm>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeToggleNegative(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    auto* context = reinterpret_cast<MpvContext*>(handle);
    if (!context || !context->mpv) return;

    // O rótulo permite alternar só este filtro, preservando os demais.
    const char* command[] = {
        "vf", "toggle", "@gpiv-negative:lavfi=[negate]", nullptr
    };
    int status = mpv_command(context->mpv, command);
    LOGI("JNI: vf toggle negative -> %d (%s)", status, mpv_error_string(status));
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeToggleMirror(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jboolean vertical
)
{
    auto* context = reinterpret_cast<MpvContext*>(handle);
    if (!context || !context->mpv) return;

    const char* filter = vertical
        ? "@gpiv-mirror-vertical:lavfi=[vflip]"
        : "@gpiv-mirror-horizontal:lavfi=[hflip]";
    const char* command[] = {"vf", "toggle", filter, nullptr};
    int status = mpv_command(context->mpv, command);
    LOGI("JNI: vf toggle %s -> %d (%s)", filter, status, mpv_error_string(status));
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSetAdjustableFilter(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jint id,
    jboolean enabled,
    jdouble strength
)
{
    auto* context = reinterpret_cast<MpvContext*>(handle);
    if (!context || !context->mpv) return JNI_FALSE;

    std::string label;
    std::string graph;
    const std::string value = std::to_string(strength);
    switch (id) {
        case 0:
            label = "@gpiv-noise-reduction";
            // Mantém a proporção padrão entre redução espacial, cromática e temporal.
            graph = "hqdn3d=" + value + ":" + std::to_string(strength * 0.75)
                + ":" + std::to_string(strength * 1.5)
                + ":" + std::to_string(strength * 1.125);
            break;
        case 1:
            label = "@gpiv-sharpness";
            graph = "unsharp=luma_amount=" + value;
            break;
        case 2:
            label = "@gpiv-blur";
            graph = "gblur=sigma=" + value;
            break;
        case 3:
            label = "@gpiv-deband";
            {
                // Zero remove o filtro; o FFmpeg exige este mínimo quando ativo.
                const std::string threshold = std::to_string(std::max(0.00003, strength));
                graph = "deband=1thr=" + threshold + ":2thr=" + threshold
                    + ":3thr=" + threshold + ":4thr=" + threshold;
            }
            break;
        default:
            return JNI_FALSE;
    }
    // Atualiza somente o filtro com este rótulo, sem acumular instâncias.
    const std::string filter = enabled ? label + ":lavfi=[" + graph + "]" : label;
    const char* command[] = {"vf", enabled ? "add" : "remove", filter.c_str(), nullptr};
    int status = mpv_command(context->mpv, command);
    LOGI("JNI: vf %s -> %d (%s)", filter.c_str(), status, mpv_error_string(status));
    return status >= 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSetEdgeDetection(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jboolean enabled,
    jdouble low,
    jdouble high
)
{
    auto* context = reinterpret_cast<MpvContext*>(handle);
    if (!context || !context->mpv) return JNI_FALSE;

    // Substituir pelo mesmo rótulo evita acumular filtros durante o arraste.
    const std::string filter = enabled
        ? "@gpiv-edges:lavfi=[edgedetect=low=" + std::to_string(low)
            + ":high=" + std::to_string(high) + "]"
        : "@gpiv-edges";
    const char* command[] = {
        "vf", enabled ? "add" : "remove", filter.c_str(), nullptr
    };
    int status = mpv_command(context->mpv, command);
    LOGI("JNI: vf edges %s -> %d (%s)", filter.c_str(), status, mpv_error_string(status));
    return status >= 0 ? JNI_TRUE : JNI_FALSE;
}

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
