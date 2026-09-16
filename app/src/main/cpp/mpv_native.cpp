#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

#include "mpv_context.h"
#include "mpv_context_stream.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavcodec/jni.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/error.h>
}

#include <stdio.h>
#include <string>
#include <unistd.h>
#include <errno.h>

#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>

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
Java_com_rec_gpiv_player_MpvNative_nativeSetScreenshotDirectory(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jstring directory
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context || !directory) {

        LOGI(
            "JNI: nativeSetScreenshotDirectory() -> contexto ou diretório inválido"
        );

        return;
    }

    const char* path =
        env->GetStringUTFChars(
            directory,
            nullptr
        );

    if (!path) {

        LOGI(
            "JNI: GetStringUTFChars() falhou"
        );

        return;
    }

    LOGI(
        "JNI: screenshot directory = %s",
        path
    );

    mpv_context_set_screenshot_directory(
        context,
        path
    );

    env->ReleaseStringUTFChars(
        directory,
        path
    );
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


    /* ========================================================
     * JAVA VM
     * ======================================================== */

    JavaVM* vm = nullptr;

    if (
        env->GetJavaVM(&vm)
        != JNI_OK
    ) {

        LOGI(
            "JNI: GetJavaVM() falhou"
        );

        return;
    }

    context->javaVm = vm;

    av_jni_set_java_vm(
        vm,
        nullptr
    );

    /* ========================================================
     * REFERÊNCIA GLOBAL DO OBJETO KOTLIN
     * ======================================================== */

    context->nativeObject =
        env->NewGlobalRef(thiz);

    if (!context->nativeObject) {

        LOGI(
            "JNI: NewGlobalRef() falhou"
        );

        return;
    }


    /* ========================================================
     * CLASSE JAVA
     * ======================================================== */

    jclass clazz =
        env->GetObjectClass(thiz);

    if (!clazz) {

        LOGI(
            "JNI: GetObjectClass() falhou"
        );

        return;
    }


    /* ========================================================
     * CALLBACK DOUBLE
     * ======================================================== */

    context->onDoubleProperty =
        env->GetMethodID(
            clazz,
            "onNativeDoubleProperty",
            "(Ljava/lang/String;D)V"
        );


    /* ========================================================
     * CALLBACK BOOLEAN
     * ======================================================== */

    context->onBooleanProperty =
        env->GetMethodID(
            clazz,
            "onNativeBooleanProperty",
            "(Ljava/lang/String;Z)V"
        );


    /* ========================================================
     * CALLBACK STRING
     * ======================================================== */

    context->onStringProperty =
        env->GetMethodID(
            clazz,
            "onNativeStringProperty",
            "(Ljava/lang/String;Ljava/lang/String;)V"
        );


    /* ========================================================
     * CALLBACK LOADING
     * ======================================================== */

    context->onLoadingChanged =
        env->GetMethodID(
            clazz,
            "onNativeLoadingChanged",
            "(Z)V"
        );

    /* ========================================================
     * CALLBACK SCREENSHOT
     * ======================================================== */

    context->onScreenshot =
        env->GetMethodID(
            clazz,
            "onNativeScreenshot",
            "([BII)V"
        );

    env->DeleteLocalRef(clazz);


    /* ========================================================
     * VALIDA CALLBACKS
     * ======================================================== */

    if (
        !context->onDoubleProperty ||
        !context->onBooleanProperty ||
        !context->onStringProperty ||
        !context->onLoadingChanged ||
        !context->onScreenshot
    ) {

        LOGI(
            "JNI: não foi possível localizar callbacks"
        );

        return;
    }


    /* ========================================================
     * INICIALIZA MPV
     * ======================================================== */

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

        LOGI(
            "JNI: nativeDestroy(NULL)"
        );

        return;
    }

    LOGI(
        "JNI: nativeDestroy(%p)",
        static_cast<void*>(context)
    );

    mpv_context_destroy(
        context
    );
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

    LOGI("JNI: screenshot MPV(%p)", static_cast<void*>(context));

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
        "set",
        "brightness",
        "0",
        nullptr
    };

    const char* contrastCommand[] = {
        "set",
        "contrast",
        "0",
        nullptr
    };

    const char* gammaCommand[] = {
        "set",
        "gamma",
        "0",
        nullptr
    };

    const char* saturationCommand[] = {
        "set",
        "saturation",
        "0",
        nullptr
    };

    mpv_command(
        context->mpv,
        brightnessCommand
    );

    mpv_command(
        context->mpv,
        contrastCommand
    );

    mpv_command(
        context->mpv,
        gammaCommand
    );

    mpv_command(
        context->mpv,
        saturationCommand
    );

    LOGI(
        "JNI: resetVideoAdjustments()"
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
        "add",
        "video-pan-x",
        xValue.c_str(),
        nullptr
    };

    const char* commandY[] = {
        "add",
        "video-pan-y",
        yValue.c_str(),
        nullptr
    };

    int statusX =
        mpv_command(
            context->mpv,
            commandX
        );

    int statusY =
        mpv_command(
            context->mpv,
            commandY
        );

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
        "set",
        "video-zoom",
        "0",
        nullptr
    };

    const char* panXCommand[] = {
        "set",
        "video-pan-x",
        "0",
        nullptr
    };

    const char* panYCommand[] = {
        "set",
        "video-pan-y",
        "0",
        nullptr
    };

    const char* rotateCommand[] = {
        "set",
        "video-rotate",
        "0",
        nullptr
    };

    mpv_command(
        context->mpv,
        zoomCommand
    );

    mpv_command(
        context->mpv,
        panXCommand
    );

    mpv_command(
        context->mpv,
        panYCommand
    );

    mpv_command(
        context->mpv,
        rotateCommand
    );

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

static std::vector<uint8_t> h264AnnexBToAvcC(
    const std::vector<uint8_t>& annexB
)
{
    std::vector<uint8_t> sps;
    std::vector<uint8_t> pps;

    size_t i = 0;

    while (i + 4 <= annexB.size()) {

        size_t startCodeSize = 0;

        if (
            i + 4 <= annexB.size() &&
            annexB[i] == 0x00 &&
            annexB[i + 1] == 0x00 &&
            annexB[i + 2] == 0x00 &&
            annexB[i + 3] == 0x01
        ) {
            startCodeSize = 4;

        } else if (
            i + 3 <= annexB.size() &&
            annexB[i] == 0x00 &&
            annexB[i + 1] == 0x00 &&
            annexB[i + 2] == 0x01
        ) {
            startCodeSize = 3;

        } else {
            ++i;
            continue;
        }

        size_t nalStart =
            i + startCodeSize;

        size_t nextStart =
            nalStart;

        bool foundNextStart = false;

        while (nextStart + 3 <= annexB.size()) {

            if (
                nextStart + 4 <= annexB.size() &&
                annexB[nextStart] == 0x00 &&
                annexB[nextStart + 1] == 0x00 &&
                annexB[nextStart + 2] == 0x00 &&
                annexB[nextStart + 3] == 0x01
            ) {
                foundNextStart = true;
                break;
            }

            if (
                annexB[nextStart] == 0x00 &&
                annexB[nextStart + 1] == 0x00 &&
                annexB[nextStart + 2] == 0x01
            ) {
                foundNextStart = true;
                break;
            }

            ++nextStart;
        }

        if (!foundNextStart) {
            nextStart = annexB.size();
        }

        size_t nalSize =
            nextStart - nalStart;

        if (nalSize > 0) {

            uint8_t nalType =
                annexB[nalStart] & 0x1F;

            if (nalType == 7) {

                sps.assign(
                    annexB.begin() + nalStart,
                    annexB.begin() + nalStart + nalSize
                );

            } else if (nalType == 8) {

                pps.assign(
                    annexB.begin() + nalStart,
                    annexB.begin() + nalStart + nalSize
                );
            }
        }

        i = nextStart;
    }

    if (sps.empty() || pps.empty()) {

        LOGI(
            "H264: não encontrei SPS/PPS no codecConfig"
        );

        return {};
    }

    LOGI(
        "H264: SPS=%zu bytes PPS=%zu bytes",
        sps.size(),
        pps.size()
    );

    std::vector<uint8_t> avcC;

    avcC.reserve(
        11 + sps.size() + pps.size()
    );

    avcC.push_back(1);

    avcC.push_back(
        sps.size() > 1 ? sps[1] : 0
    );

    avcC.push_back(
        sps.size() > 2 ? sps[2] : 0
    );

    avcC.push_back(
        sps.size() > 3 ? sps[3] : 0
    );

    avcC.push_back(0xFF);

    avcC.push_back(0xE1);

    avcC.push_back(
        static_cast<uint8_t>(
            (sps.size() >> 8) & 0xFF
        )
    );

    avcC.push_back(
        static_cast<uint8_t>(
            sps.size() & 0xFF
        )
    );

    avcC.insert(
        avcC.end(),
        sps.begin(),
        sps.end()
    );

    avcC.push_back(1);

    avcC.push_back(
        static_cast<uint8_t>(
            (pps.size() >> 8) & 0xFF
        )
    );

    avcC.push_back(
        static_cast<uint8_t>(
            pps.size() & 0xFF
        )
    );

    avcC.insert(
        avcC.end(),
        pps.begin(),
        pps.end()
    );

    LOGI(
        "H264: avcC gerado: %zu bytes",
        avcC.size()
    );

    return avcC;
}


static std::vector<uint8_t> h264AnnexBToAvcc(
    const std::vector<uint8_t>& annexB
)
{
    std::vector<uint8_t> avcc;

    size_t i = 0;

    while (i + 3 <= annexB.size()) {

        size_t startCodeSize = 0;

        if (
            i + 4 <= annexB.size() &&
            annexB[i] == 0x00 &&
            annexB[i + 1] == 0x00 &&
            annexB[i + 2] == 0x00 &&
            annexB[i + 3] == 0x01
        ) {
            startCodeSize = 4;

        } else if (
            annexB[i] == 0x00 &&
            annexB[i + 1] == 0x00 &&
            annexB[i + 2] == 0x01
        ) {
            startCodeSize = 3;

        } else {
            ++i;
            continue;
        }

        size_t nalStart =
            i + startCodeSize;

        size_t nextStart =
            annexB.size();

        for (
            size_t search = nalStart;
            search + 3 <= annexB.size();
            ++search
        ) {

            if (
                search + 4 <= annexB.size() &&
                annexB[search] == 0x00 &&
                annexB[search + 1] == 0x00 &&
                annexB[search + 2] == 0x00 &&
                annexB[search + 3] == 0x01
            ) {
                nextStart = search;
                break;
            }

            if (
                annexB[search] == 0x00 &&
                annexB[search + 1] == 0x00 &&
                annexB[search + 2] == 0x01
            ) {
                nextStart = search;
                break;
            }
        }

        size_t nalSize =
            nextStart - nalStart;

        if (nalSize > 0) {

            avcc.push_back(
                static_cast<uint8_t>(
                    (nalSize >> 24) & 0xFF
                )
            );

            avcc.push_back(
                static_cast<uint8_t>(
                    (nalSize >> 16) & 0xFF
                )
            );

            avcc.push_back(
                static_cast<uint8_t>(
                    (nalSize >> 8) & 0xFF
                )
            );

            avcc.push_back(
                static_cast<uint8_t>(
                    nalSize & 0xFF
                )
            );

            avcc.insert(
                avcc.end(),
                annexB.begin() + nalStart,
                annexB.begin() + nalStart + nalSize
            );
        }

        i = nextStart;
    }

    LOGI(
        "H264: Annex-B %zu bytes -> AVCC %zu bytes",
        annexB.size(),
        avcc.size()
    );

    return avcc;
}


extern "C"
JNIEXPORT jlongArray JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeTestCutFrames(
    JNIEnv* env,
    jobject thiz,
    jint fd,
    jlong frameA,
    jlong frameB
)
{
    if (fd < 0) {
        LOGI(
            "FFmpeg: testCutFrames() -> fd inválido: %d",
            fd
        );
        return nullptr;
    }

    if (frameA < 0 || frameB < frameA) {
        LOGI(
            "FFmpeg: testCutFrames() -> intervalo inválido A=%lld B=%lld",
            static_cast<long long>(frameA),
            static_cast<long long>(frameB)
        );
        return nullptr;
    }

    AMediaCodec* encoder =
        AMediaCodec_createEncoderByType(
            "video/avc"
        );

    if (!encoder) {

        LOGI(
            "MediaCodec: encoder H264 NÃO encontrado"
        );

        return nullptr;
    }

    LOGI(
        "MediaCodec: encoder H264 criado"
    );

    AMediaFormat* format =
        AMediaFormat_new();

    if (!format) {

        LOGI(
            "MediaCodec: AMediaFormat_new() falhou"
        );

        AMediaCodec_delete(
            encoder
        );

        return nullptr;
    }

    AMediaFormat_setString(
        format,
        AMEDIAFORMAT_KEY_MIME,
        "video/avc"
    );

    AMediaFormat_setInt32(
        format,
        AMEDIAFORMAT_KEY_WIDTH,
        640
    );

    AMediaFormat_setInt32(
        format,
        AMEDIAFORMAT_KEY_HEIGHT,
        1138
    );

    AMediaFormat_setInt32(
        format,
        AMEDIAFORMAT_KEY_COLOR_FORMAT,
        19
    );

    AMediaFormat_setInt32(
        format,
        AMEDIAFORMAT_KEY_BIT_RATE,
        2 * 1000 * 1000
    );

    AMediaFormat_setInt32(
        format,
        AMEDIAFORMAT_KEY_FRAME_RATE,
        30
    );

    AMediaFormat_setInt32(
        format,
        AMEDIAFORMAT_KEY_I_FRAME_INTERVAL,
        1
    );

    LOGI(
        "MediaCodec: configurando H264 640x1138"
    );

    media_status_t configureResult =
        AMediaCodec_configure(
            encoder,
            format,
            nullptr,
            nullptr,
            AMEDIACODEC_CONFIGURE_FLAG_ENCODE
        );

    if (configureResult != AMEDIA_OK) {

        LOGI(
            "MediaCodec: configure() falhou: %d",
            configureResult
        );

        AMediaFormat_delete(
            format
        );

        AMediaCodec_delete(
            encoder
        );

        return nullptr;
    }

    LOGI(
        "MediaCodec: configure() OK"
    );

    const char* configuredFormat =
        AMediaFormat_toString(
            format
        );

    if (configuredFormat) {

        LOGI(
            "MediaCodec: configured input format = %s",
            configuredFormat
        );
    }

    AMediaFormat_delete(
        format
    );

    media_status_t startResult =
        AMediaCodec_start(
            encoder
        );

    if (startResult != AMEDIA_OK) {

        LOGI(
            "MediaCodec: start() falhou: %d",
            startResult
        );

        AMediaCodec_delete(
            encoder
        );

        return nullptr;
    }

    LOGI(
        "MediaCodec: start() OK"
    );

    const char* outputPath =
        "/data/data/com.rec.gpiv/cache/frameA.h264";

    FILE* outputFile =
        fopen(outputPath, "wb");

    if (outputFile) {

        fclose(outputFile);

        LOGI(
            "MediaCodec: arquivo H264 anterior removido: %s",
            outputPath
        );

    } else {

        LOGI(
            "MediaCodec: não foi possível limpar %s",
            outputPath
        );
    }

    ssize_t inputIndex =
        AMediaCodec_dequeueInputBuffer(
            encoder,
            0
        );

    LOGI(
        "MediaCodec: input buffer index = %zd",
        inputIndex
    );

    if (inputIndex >= 0) {

        size_t inputBufferSize = 0;

        uint8_t* inputBuffer =
            AMediaCodec_getInputBuffer(
                encoder,
                static_cast<size_t>(inputIndex),
                &inputBufferSize
            );

        LOGI(
            "MediaCodec: input buffer size = %zu ptr=%p",
            inputBufferSize,
            inputBuffer
        );
    }

    AMediaFormat* outputFormat =
        AMediaCodec_getOutputFormat(
            encoder
        );

    if (outputFormat) {

        const char* formatString =
            AMediaFormat_toString(
                outputFormat
            );

        if (formatString) {

            LOGI(
                "MediaCodec: output format = %s",
                formatString
            );

        }

        AMediaFormat_delete(
            outputFormat
        );

    } else {

        LOGI(
            "MediaCodec: output format = NULL"
        );
    }

    int sourceFd =
        dup(fd);

    if (sourceFd < 0) {
        LOGI(
            "FFmpeg: testCutFrames() -> dup() falhou"
        );
        return nullptr;
    }

    if (
        lseek(
            sourceFd,
            0,
            SEEK_SET
        ) < 0
    ) {
        LOGI(
            "FFmpeg: testCutFrames() -> lseek(0) falhou: %s",
            strerror(errno)
        );
        close(sourceFd);
        return nullptr;
    }

    LOGI(
        "FFmpeg: testCutFrames() -> fd=%d A=%lld B=%lld",
        sourceFd,
        static_cast<long long>(frameA),
        static_cast<long long>(frameB)
    );

    unsigned char* ioBuffer =
        static_cast<unsigned char*>(
            av_malloc(32768)
        );

    if (!ioBuffer) {
        LOGI(
            "FFmpeg: testCutFrames() -> av_malloc() falhou"
        );
        close(sourceFd);
        return nullptr;
    }

    AVIOContext* ioContext =
        avio_alloc_context(
            ioBuffer,
            32768,
            0,
            &sourceFd,

            [](void* opaque, uint8_t* buffer, int bufferSize) -> int {

                int* fdPtr =
                    static_cast<int*>(opaque);

                if (!fdPtr || *fdPtr < 0) {
                    return AVERROR(EINVAL);
                }

                ssize_t bytesRead =
                    read(
                        *fdPtr,
                        buffer,
                        static_cast<size_t>(bufferSize)
                    );

                if (bytesRead < 0) {
                    return AVERROR(errno);
                }

                if (bytesRead == 0) {
                    return AVERROR_EOF;
                }

                return static_cast<int>(
                    bytesRead
                );
            },

            nullptr,

            [](void* opaque, int64_t offset, int whence) -> int64_t {

                int* fdPtr =
                    static_cast<int*>(opaque);

                if (!fdPtr || *fdPtr < 0) {
                    return AVERROR(EINVAL);
                }

                if (whence == AVSEEK_SIZE) {

                    off_t current =
                        lseek(
                            *fdPtr,
                            0,
                            SEEK_CUR
                        );

                    off_t end =
                        lseek(
                            *fdPtr,
                            0,
                            SEEK_END
                        );

                    if (end < 0) {
                        return AVERROR(errno);
                    }

                    if (
                        lseek(
                            *fdPtr,
                            current,
                            SEEK_SET
                        ) < 0
                    ) {
                        return AVERROR(errno);
                    }

                    return static_cast<int64_t>(
                        end
                    );
                }

                int seekWhence =
                    whence & 0xFFFF;

                off_t result =
                    lseek(
                        *fdPtr,
                        static_cast<off_t>(offset),
                        seekWhence
                    );

                if (result < 0) {
                    return AVERROR(errno);
                }

                return static_cast<int64_t>(
                    result
                );
            }
        );

    if (!ioContext) {
        LOGI(
            "FFmpeg: testCutFrames() -> avio_alloc_context() falhou"
        );
        av_free(ioBuffer);
        close(sourceFd);
        return nullptr;
    }

    AVFormatContext* formatContext =
        avformat_alloc_context();

    if (!formatContext) {
        LOGI(
            "FFmpeg: testCutFrames() -> avformat_alloc_context() falhou"
        );

        av_freep(
            &ioContext->buffer
        );

        avio_context_free(
            &ioContext
        );

        close(sourceFd);
        return nullptr;
    }

    formatContext->pb =
        ioContext;

    formatContext->flags |=
        AVFMT_FLAG_CUSTOM_IO;

    int result =
        avformat_open_input(
            &formatContext,
            nullptr,
            nullptr,
            nullptr
        );

    if (result < 0) {

        char errorBuffer[
            AV_ERROR_MAX_STRING_SIZE
        ];

        av_strerror(
            result,
            errorBuffer,
            sizeof(errorBuffer)
        );

        LOGI(
            "FFmpeg: testCutFrames() -> avformat_open_input(): %s",
            errorBuffer
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    result =
        avformat_find_stream_info(
            formatContext,
            nullptr
        );

    if (result < 0) {

        char errorBuffer[
            AV_ERROR_MAX_STRING_SIZE
        ];

        av_strerror(
            result,
            errorBuffer,
            sizeof(errorBuffer)
        );

        LOGI(
            "FFmpeg: testCutFrames() -> find_stream_info(): %s",
            errorBuffer
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    int videoStreamIndex = -1;

    for (
        unsigned int i = 0;
        i < formatContext->nb_streams;
        ++i
    ) {
        if (
            formatContext->streams[i]
                ->codecpar
                ->codec_type
            == AVMEDIA_TYPE_VIDEO
        ) {
            videoStreamIndex =
                static_cast<int>(i);

            break;
        }
    }

    int audioStreamIndex = -1;

    for (
        unsigned int i = 0;
        i < formatContext->nb_streams;
        ++i
    ) {
        if (
            formatContext->streams[i]
                ->codecpar
                ->codec_type
            == AVMEDIA_TYPE_AUDIO
        ) {
            audioStreamIndex =
                static_cast<int>(i);

            LOGI(
                "FFmpeg: áudio encontrado stream=%d codec=%s",
                audioStreamIndex,
                avcodec_get_name(
                    formatContext->streams[i]
                        ->codecpar
                        ->codec_id
                )
            );

            break;
        }
    }
    if (videoStreamIndex < 0) {

        LOGI(
            "FFmpeg: testCutFrames() -> stream de vídeo não encontrado"
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    AVStream* videoStream =
        formatContext->streams[
            videoStreamIndex
        ];

    AVCodecParameters* codecParameters =
        videoStream->codecpar;

    LOGI(
        "FFmpeg: video codec=%s width=%d height=%d pix_fmt=%d",
        avcodec_get_name(codecParameters->codec_id),
        codecParameters->width,
        codecParameters->height,
        codecParameters->format
    );

    const AVCodec* h264Encoder =
        avcodec_find_encoder(
            AV_CODEC_ID_H264
        );

    if (h264Encoder) {

        LOGI(
            "FFmpeg: encoder H264 encontrado: %s",
            h264Encoder->name
        );

    } else {

        LOGI(
            "FFmpeg: encoder H264 NAO encontrado"
        );
    }

    void* codecIterator = nullptr;

    const AVCodec* codec = nullptr;

    while (
        (codec = av_codec_iterate(&codecIterator)) != nullptr
    ) {
        if (av_codec_is_encoder(codec)) {

            LOGI(
                "FFmpeg: encoder disponível: %s",
                codec->name
            );
        }
    }

    LOGI(
        "FFmpeg: video time_base = %d/%d",
        videoStream->time_base.num,
        videoStream->time_base.den
    );

    const AVCodec* decoder =
        avcodec_find_decoder(
            videoStream->codecpar->codec_id
        );

    if (!decoder) {

        LOGI(
            "FFmpeg: testCutFrames() -> decoder não encontrado"
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    AVCodecContext* codecContext =
        avcodec_alloc_context3(
            decoder
        );

    if (!codecContext) {

        LOGI(
            "FFmpeg: testCutFrames() -> avcodec_alloc_context3() falhou"
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    result =
        avcodec_parameters_to_context(
            codecContext,
            videoStream->codecpar
        );

    LOGI(
        "FFmpeg: decoder config: codec_id=%d width=%d height=%d "
        "extradata_size=%d format=%d",
        codecContext->codec_id,
        codecContext->width,
        codecContext->height,
        codecContext->extradata_size,
        codecContext->pix_fmt
    );

    if (result < 0) {

        LOGI(
            "FFmpeg: testCutFrames() -> avcodec_parameters_to_context() falhou"
        );

        avcodec_free_context(
            &codecContext
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    result =
        avcodec_open2(
            codecContext,
            decoder,
            nullptr
        );

    LOGI(
        "FFmpeg: decoder aberto: %s",
        codecContext->codec->name
    );

    if (result < 0) {

        char errorBuffer[
            AV_ERROR_MAX_STRING_SIZE
        ];

        av_strerror(
            result,
            errorBuffer,
            sizeof(errorBuffer)
        );

        LOGI(
            "FFmpeg: testCutFrames() -> avcodec_open2(): %s",
            errorBuffer
        );

        avcodec_free_context(
            &codecContext
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    LOGI(
        "FFmpeg: testCutFrames() -> decodificando stream=%d",
        videoStreamIndex
    );

    AVPacket* packet =
        av_packet_alloc();

    AVFrame* frame =
        av_frame_alloc();

    if (!packet || !frame) {

        LOGI(
            "FFmpeg: testCutFrames() -> falha ao alocar packet/frame"
        );

        av_packet_free(
            &packet
        );

        av_frame_free(
            &frame
        );

        avcodec_free_context(
            &codecContext
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );
            avio_context_free(
                &ioContext
            );
        }

        close(sourceFd);
        return nullptr;
    }

    int64_t decodedFrame = 0;
    int64_t framesInCut = 0;

    std::vector<uint8_t> yPlane;
    std::vector<uint8_t> uPlane;
    std::vector<uint8_t> vPlane;

    std::vector<uint8_t> codecConfig;

    AVFormatContext* outputFormatContext = nullptr;
    AVStream* outputVideoStream = nullptr;
    AVStream* outputAudioStream = nullptr;
    bool mp4Started = false;
    int64_t encodedFrameIndex = 0;

    std::vector<AVPacket*> audioPackets;

    std::vector<uint8_t> avcC;

    bool foundA = false;
    bool foundB = false;

    int64_t ptsA = AV_NOPTS_VALUE;
    int64_t ptsB = AV_NOPTS_VALUE;

    int videoPacketCount = 0;

    int64_t audioStartPts = AV_NOPTS_VALUE;
    int64_t audioEndPts = AV_NOPTS_VALUE;
    bool audioEndFound = false;

    while (!foundB) {

        result =
            av_read_frame(
                formatContext,
                packet
            );

        if (result < 0) {

            char errorBuffer[
                AV_ERROR_MAX_STRING_SIZE
            ];

            av_strerror(
                result,
                errorBuffer,
                sizeof(errorBuffer)
            );

            LOGI(
                "FFmpeg: testCutFrames() -> av_read_frame(): %s (%d)",
                errorBuffer,
                result
            );

            break;
        }

        if (
            packet->stream_index
            == audioStreamIndex
        ) {

            AVPacket* audioPacket =
                av_packet_alloc();

            if (audioPacket) {

                if (
                    av_packet_ref(
                        audioPacket,
                        packet
                    ) == 0
                ) {

                    audioPackets.push_back(
                        audioPacket
                    );

                } else {

                    av_packet_free(
                        &audioPacket
                    );
                }
            }

            av_packet_unref(packet);

            continue;
        }

        if (
            packet->stream_index
            != videoStreamIndex
        ) {
            av_packet_unref(packet);
            continue;
        }

        if (packet->stream_index == videoStreamIndex &&
            videoPacketCount < 6) {

            LOGI(
                "FFmpeg: VIDEO PACKET #%d size=%d pts=%lld dts=%lld flags=0x%x",
                videoPacketCount,
                packet->size,
                static_cast<long long>(packet->pts),
                static_cast<long long>(packet->dts),
                packet->flags
            );

            videoPacketCount++;
        }

        result =
            avcodec_send_packet(
                codecContext,
                packet
            );

        if (result < 0) {

            char errorBuffer[
                AV_ERROR_MAX_STRING_SIZE
            ];

            av_strerror(
                result,
                errorBuffer,
                sizeof(errorBuffer)
            );

            LOGI(
                "FFmpeg: PRIMEIRO ERRO send_packet: %s (%d) "
                "stream=%d size=%d pts=%lld dts=%lld",
                errorBuffer,
                result,
                packet->stream_index,
                packet->size,
                static_cast<long long>(packet->pts),
                static_cast<long long>(packet->dts)
            );

            av_packet_unref(packet);
            break;
        }

        av_packet_unref(packet);

        while (true) {

            result =
                avcodec_receive_frame(
                    codecContext,
                    frame
                );

            if (
                result == AVERROR(EAGAIN) ||
                result == AVERROR_EOF
            ) {
                break;
            }

            if (result < 0) {

                char errorBuffer[
                    AV_ERROR_MAX_STRING_SIZE
                ];

                av_strerror(
                    result,
                    errorBuffer,
                    sizeof(errorBuffer)
                );

                LOGI(
                    "FFmpeg: avcodec_receive_frame(): %s (%d)",
                    errorBuffer,
                    result
                );

                break;
            }

            if (decodedFrame == 0) {

                LOGI(
                    "FFmpeg: PRIMEIRO AVFrame: "
                    "format=%d width=%d height=%d "
                    "linesize=[%d,%d,%d]",
                    frame->format,
                    frame->width,
                    frame->height,
                    frame->linesize[0],
                    frame->linesize[1],
                    frame->linesize[2]
                );
            }

            if (
                decodedFrame >= frameA &&
                decodedFrame <= frameB
            ) {

                const bool isFrameA =
                    decodedFrame == frameA;

                const char* frameLabel =
                    isFrameA ? "A" : "A+1";

                if (isFrameA) {

                    foundA = true;
                    ptsA = frame->pts;

                    LOGI(
                        "FFmpeg: FRAME A encontrado: %lld pts=%lld",
                        static_cast<long long>(
                            decodedFrame
                        ),
                        static_cast<long long>(
                            frame->pts
                        )
                    );

                } else {

                    LOGI(
                        "FFmpeg: FRAME A+1 encontrado: %lld pts=%lld",
                        static_cast<long long>(
                            decodedFrame
                        ),
                        static_cast<long long>(
                            frame->pts
                        )
                    );
                }

                const int width = frame->width;
                const int height = frame->height;

                const int chromaWidth = width / 2;
                const int chromaHeight = height / 2;

                yPlane.resize(
                    width * height
                );

                uPlane.resize(
                    chromaWidth * chromaHeight
                );

                vPlane.resize(
                    chromaWidth * chromaHeight
                );

                for (int y = 0; y < height; ++y) {

                    memcpy(
                        yPlane.data() + y * width,
                        frame->data[0] + y * frame->linesize[0],
                        width
                    );
                }

                for (int y = 0; y < chromaHeight; ++y) {

                    memcpy(
                        uPlane.data() + y * chromaWidth,
                        frame->data[1] + y * frame->linesize[1],
                        chromaWidth
                    );

                    memcpy(
                        vPlane.data() + y * chromaWidth,
                        frame->data[2] + y * frame->linesize[2],
                        chromaWidth
                    );
                }

                LOGI(
                    "FFmpeg: FRAME A copiado YUV420P: "
                    "Y=%zu U=%zu V=%zu",
                    yPlane.size(),
                    uPlane.size(),
                    vPlane.size()
                );


                ssize_t inputIndex =
                    AMediaCodec_dequeueInputBuffer(
                        encoder,
                        100000
                    );

                LOGI(
                    "MediaCodec: FRAME A input buffer index = %zd",
                    inputIndex
                );

                if (inputIndex >= 0) {

                    size_t inputBufferSize = 0;

                    uint8_t* inputBuffer =
                        AMediaCodec_getInputBuffer(
                            encoder,
                            static_cast<size_t>(inputIndex),
                            &inputBufferSize
                        );

                    const size_t frameSize =
                        yPlane.size() +
                        uPlane.size() +
                        vPlane.size();

                    LOGI(
                        "MediaCodec: FRAME A buffer size=%zu "
                        "frameSize=%zu ptr=%p",
                        inputBufferSize,
                        frameSize,
                        inputBuffer
                    );

                    if (
                        inputBuffer != nullptr &&
                        inputBufferSize >= frameSize
                    ) {

                        size_t offset = 0;

                        memcpy(
                            inputBuffer + offset,
                            yPlane.data(),
                            yPlane.size()
                        );

                        offset += yPlane.size();

                        memcpy(
                            inputBuffer + offset,
                            uPlane.data(),
                            uPlane.size()
                        );

                        offset += uPlane.size();

                        memcpy(
                            inputBuffer + offset,
                            vPlane.data(),
                            vPlane.size()
                        );

                        LOGI(
                            "MediaCodec: FRAME A copiado para input buffer"
                        );

                        media_status_t queueResult =
                            AMediaCodec_queueInputBuffer(
                                encoder,
                                static_cast<size_t>(inputIndex),
                                0,
                                frameSize,
                                static_cast<int64_t>(
                                    av_rescale_q(
                                        frame->pts,
                                        videoStream->time_base,
                                        AVRational{1, 1000000}
                                    )
                                ),
                                0
                            );

                        LOGI(
                            "MediaCodec: FRAME A queueInputBuffer "
                            "result=%d pts=%lld size=%zu",
                            queueResult,
                            static_cast<long long>(frame->pts),
                            frameSize
                        );

                        AMediaCodecBufferInfo outputInfo{};


                        ssize_t outputIndex =
                            AMediaCodec_dequeueOutputBuffer(
                                encoder,
                                &outputInfo,
                                100000
                            );

                        LOGI(
                            "MediaCodec: FRAME A output buffer index = %zd "
                            "size=%d pts=%lld flags=%u",
                            outputIndex,
                            outputInfo.size,
                            static_cast<long long>(
                                outputInfo.presentationTimeUs
                            ),
                            outputInfo.flags
                        );


                        if (
                            outputIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED
                        ) {

                            LOGI(
                                "MediaCodec: FRAME A output format changed"
                            );

                            for (int attempt = 0; attempt < 10; ++attempt) {

                                outputIndex =
                                    AMediaCodec_dequeueOutputBuffer(
                                        encoder,
                                        &outputInfo,
                                        100000
                                    );

                                LOGI(
                                    "MediaCodec: FRAME A output tentativa=%d "
                                    "index=%zd size=%d pts=%lld flags=%u",
                                    attempt + 1,
                                    outputIndex,
                                    outputInfo.size,
                                    static_cast<long long>(
                                        outputInfo.presentationTimeUs
                                    ),
                                    outputInfo.flags
                                );


                                if (outputIndex >= 0) {

                                    if (
                                        outputInfo.flags &
                                        AMEDIACODEC_BUFFER_FLAG_CODEC_CONFIG
                                    ) {

                                        size_t codecConfigSize = 0;

                                        uint8_t* codecConfigBuffer =
                                            AMediaCodec_getOutputBuffer(
                                                encoder,
                                                static_cast<size_t>(outputIndex),
                                                &codecConfigSize
                                            );

                                        if (
                                            codecConfigBuffer != nullptr &&
                                            outputInfo.size > 0
                                        ) {

                                            codecConfig.assign(
                                                codecConfigBuffer,
                                                codecConfigBuffer + outputInfo.size
                                            );

                                            avcC =
                                                h264AnnexBToAvcC(
                                                    codecConfig
                                                );

                                            if (!avcC.empty()) {

                                                LOGI(
                                                    "H264: avcC primeiros bytes: "
                                                    "%02X %02X %02X %02X %02X %02X %02X %02X",
                                                    avcC[0],
                                                    avcC[1],
                                                    avcC[2],
                                                    avcC[3],
                                                    avcC[4],
                                                    avcC[5],
                                                    avcC[6],
                                                    avcC[7]
                                                );
                                            }


                                            if (!avcC.empty() && !mp4Started) {

                                                const char* movPath =
                                                    "/data/data/com.rec.gpiv/cache/frameA.mov";

                                                const AVOutputFormat* testOutputFormat =
                                                    av_guess_format(
                                                        "mov",
                                                        movPath,
                                                        nullptr
                                                    );

                                                LOGI(
                                                    "MOV: av_guess_format(mov) = %p",
                                                    testOutputFormat
                                                );

                                                if (testOutputFormat) {
                                                    LOGI(
                                                        "MOV: muxer encontrado: name=%s long_name=%s",
                                                        testOutputFormat->name,
                                                        testOutputFormat->long_name
                                                            ? testOutputFormat->long_name
                                                            : "(null)"
                                                    );
                                                }

                                                int muxResult =
                                                    avformat_alloc_output_context2(
                                                        &outputFormatContext,
                                                        nullptr,
                                                        "mov",
                                                        movPath
                                                    );

                                                if (
                                                    muxResult < 0 ||
                                                    outputFormatContext == nullptr
                                                ) {

                                                    LOGI(
                                                        "MOV: avformat_alloc_output_context2() falhou: %d",
                                                        muxResult
                                                    );

                                                } else {

                                                    outputVideoStream =
                                                        avformat_new_stream(
                                                            outputFormatContext,
                                                            nullptr
                                                        );

                                                    if (!outputVideoStream) {

                                                        LOGI(
                                                            "MOV: avformat_new_stream() falhou"
                                                        );

                                                        avformat_free_context(
                                                            outputFormatContext
                                                        );

                                                        outputFormatContext = nullptr;

                                                    } else {

                                                        outputVideoStream->time_base =
                                                            AVRational{1, 30};

                                                        AVCodecParameters* outputCodecParameters =
                                                            outputVideoStream->codecpar;

                                                        outputCodecParameters->codec_type =
                                                            AVMEDIA_TYPE_VIDEO;

                                                        outputCodecParameters->codec_id =
                                                            AV_CODEC_ID_H264;

                                                        outputCodecParameters->width =
                                                            640;

                                                        outputCodecParameters->height =
                                                            1138;

                                                        outputCodecParameters->format =
                                                            AV_PIX_FMT_YUV420P;

                                                        outputCodecParameters->color_range =
                                                            AVCOL_RANGE_MPEG;

                                                        outputCodecParameters->color_space =
                                                            AVCOL_SPC_BT709;

                                                        outputCodecParameters->color_primaries =
                                                            AVCOL_PRI_BT709;

                                                        outputCodecParameters->extradata =
                                                            static_cast<uint8_t*>(
                                                                av_malloc(
                                                                    avcC.size() +
                                                                    AV_INPUT_BUFFER_PADDING_SIZE
                                                                )
                                                            );

                                                        outputCodecParameters->extradata_size =
                                                            static_cast<int>(
                                                                avcC.size()
                                                            );

                                                        memcpy(
                                                            outputCodecParameters->extradata,
                                                            avcC.data(),
                                                            avcC.size()
                                                        );

                                                        memset(
                                                            outputCodecParameters->extradata +
                                                                avcC.size(),
                                                            0,
                                                            AV_INPUT_BUFFER_PADDING_SIZE
                                                        );

                                                        outputAudioStream =
                                                            avformat_new_stream(
                                                                outputFormatContext,
                                                                nullptr
                                                            );

                                                        if (!outputAudioStream) {

                                                            LOGI(
                                                                "MOV: avformat_new_stream() áudio falhou"
                                                            );

                                                            avformat_free_context(
                                                                outputFormatContext
                                                            );

                                                            outputFormatContext = nullptr;

                                                        } else {

                                                            AVStream* inputAudioStream =
                                                                formatContext->streams[audioStreamIndex];

                                                            muxResult =
                                                                avcodec_parameters_copy(
                                                                    outputAudioStream->codecpar,
                                                                    inputAudioStream->codecpar
                                                                );

                                                            if (muxResult < 0) {

                                                                LOGI(
                                                                    "MOV: avcodec_parameters_copy() áudio falhou: %d",
                                                                    muxResult
                                                                );

                                                                avformat_free_context(
                                                                    outputFormatContext
                                                                );

                                                                outputFormatContext = nullptr;

                                                            } else {

                                                                outputAudioStream->time_base =
                                                                    inputAudioStream->time_base;

                                                                LOGI(
                                                                    "MOV: áudio configurado stream=%d "
                                                                    "codec=%s sample_rate=%d channels=%d "
                                                                    "time_base=%d/%d",
                                                                    outputAudioStream->index,
                                                                    avcodec_get_name(
                                                                        outputAudioStream
                                                                            ->codecpar
                                                                            ->codec_id
                                                                    ),
                                                                    outputAudioStream
                                                                        ->codecpar
                                                                        ->sample_rate,
                                                                    outputAudioStream
                                                                        ->codecpar
                                                                        ->ch_layout
                                                                        .nb_channels,
                                                                    outputAudioStream
                                                                        ->time_base
                                                                        .num,
                                                                    outputAudioStream
                                                                        ->time_base
                                                                        .den
                                                                );
                                                            }
                                                        }

                                                        if (
                                                            !(outputFormatContext->oformat->flags &
                                                              AVFMT_NOFILE)
                                                        ) {

                                                            muxResult =
                                                                avio_open(
                                                                    &outputFormatContext->pb,
                                                                    movPath,
                                                                    AVIO_FLAG_WRITE
                                                                );

                                                        }

                                                        if (muxResult < 0) {

                                                            LOGI(
                                                                "MOV: avio_open() falhou: %d",
                                                                muxResult
                                                            );

                                                        } else {

                                                            muxResult =
                                                                avformat_write_header(
                                                                    outputFormatContext,
                                                                    nullptr
                                                                );

                                                            if (muxResult < 0) {

                                                                char errorBuffer[
                                                                    AV_ERROR_MAX_STRING_SIZE
                                                                ];

                                                                av_strerror(
                                                                    muxResult,
                                                                    errorBuffer,
                                                                    sizeof(errorBuffer)
                                                                );

                                                                LOGI(
                                                                    "MOV: write_header() falhou: %s",
                                                                    errorBuffer
                                                                );

                                                            } else {

                                                                mp4Started = true;

                                                                LOGI(
                                                                    "MOV: muxer iniciado: %s",
                                                                    movPath
                                                                );
                                                            }
                                                        }
                                                    }
                                                }
                                            }


                                            if (codecConfig.size() >= 8) {

                                                LOGI(
                                                    "MediaCodec: CODEC_CONFIG primeiros bytes: "
                                                    "%02X %02X %02X %02X %02X %02X %02X %02X",
                                                    codecConfig[0],
                                                    codecConfig[1],
                                                    codecConfig[2],
                                                    codecConfig[3],
                                                    codecConfig[4],
                                                    codecConfig[5],
                                                    codecConfig[6],
                                                    codecConfig[7]
                                                );
                                            }

                                            LOGI(
                                                "MediaCodec: FRAME A CODEC_CONFIG "
                                                "copiado: %zu bytes",
                                                codecConfig.size()
                                            );

                                        } else {

                                            LOGI(
                                                "MediaCodec: FRAME A CODEC_CONFIG "
                                                "buffer NULL ou size=0"
                                            );
                                        }

                                        AMediaCodec_releaseOutputBuffer(
                                            encoder,
                                            static_cast<size_t>(outputIndex),
                                            false
                                        );

                                        outputIndex = -1;

                                        continue;
                                    }

                                    break;
                                }

                                if (
                                    outputIndex ==
                                    AMEDIACODEC_INFO_TRY_AGAIN_LATER
                                ) {

                                    continue;
                                }

                                break;
                            }
                        }

                        if (outputIndex >= 0) {

                            LOGI(
                                "MediaCodec: ENTROU no bloco de gravação, outputIndex=%zd",
                                outputIndex
                            );

                            size_t outputBufferSize = 0;

                            uint8_t* outputBuffer =
                                AMediaCodec_getOutputBuffer(
                                    encoder,
                                    static_cast<size_t>(outputIndex),
                                    &outputBufferSize
                                );

                            std::vector<uint8_t> encodedFrame(
                                outputBuffer,
                                outputBuffer + outputInfo.size
                            );

                            std::vector<uint8_t> avccFrame;

                            if (!encodedFrame.empty()) {
                                avccFrame =
                                    h264AnnexBToAvcc(encodedFrame);
                            }

                            if (
                                mp4Started &&
                                !avccFrame.empty()
                            ) {

                                AVPacket* outputPacket =
                                    av_packet_alloc();

                                if (outputPacket) {

                                    outputPacket->data =
                                        avccFrame.data();

                                    outputPacket->size =
                                        static_cast<int>(
                                            avccFrame.size()
                                        );

                                    outputPacket->stream_index =
                                        outputVideoStream->index;

                                    outputPacket->pts =
                                        encodedFrameIndex;

                                    outputPacket->dts =
                                        encodedFrameIndex;

                                    outputPacket->duration =
                                        1;

                                    av_packet_rescale_ts(
                                        outputPacket,
                                        AVRational{1, 30},
                                        outputVideoStream->time_base
                                    );

                                    int packetSize =
                                        outputPacket->size;

                                    int64_t packetPts =
                                        outputPacket->pts;

                                    int64_t packetDts =
                                        outputPacket->dts;

                                    int writeResult =
                                        av_interleaved_write_frame(
                                            outputFormatContext,
                                            outputPacket
                                        );

                                    LOGI(
                                        "MOV: frame=%lld size=%d pts=%lld dts=%lld write=%d",
                                        static_cast<long long>(
                                            encodedFrameIndex
                                        ),
                                        packetSize,
                                        static_cast<long long>(
                                            packetPts
                                        ),
                                        static_cast<long long>(
                                            packetDts
                                        ),
                                        writeResult
                                    );

                                    if (writeResult < 0) {

                                        char errorBuffer[
                                            AV_ERROR_MAX_STRING_SIZE
                                        ];

                                        av_strerror(
                                            writeResult,
                                            errorBuffer,
                                            sizeof(errorBuffer)
                                        );

                                        LOGI(
                                            "MOV: ERRO write_frame: %s",
                                            errorBuffer
                                        );
                                    }

                                    av_packet_free(
                                        &outputPacket
                                    );

                                    ++encodedFrameIndex;
                                }
                            }

                            FILE* outputFile =
                                fopen(outputPath, "ab");

                            if (outputFile) {

                                size_t writtenConfig = 0;

                                if (isFrameA) {

                                    writtenConfig =
                                        fwrite(
                                            codecConfig.data(),
                                            1,
                                            codecConfig.size(),
                                            outputFile
                                        );
                                }

                                size_t writtenFrame =
                                    fwrite(
                                        encodedFrame.data(),
                                        1,
                                        encodedFrame.size(),
                                        outputFile
                                    );

                                size_t written =
                                    writtenConfig + writtenFrame;

                                fclose(outputFile);

                                LOGI(
                                    "MediaCodec: FRAME A H264 salvo: "
                                    "%zu bytes (config=%zu frame=%zu) em %s",
                                    written,
                                    writtenConfig,
                                    writtenFrame,
                                    outputPath
                                );

                            } else {

                                LOGI(
                                    "MediaCodec: erro ao abrir %s para escrita",
                                    outputPath
                                );
                            }

                            LOGI(
                                "MediaCodec: FRAME A H264 copiado para memória: %zu bytes",
                                encodedFrame.size()
                            );

                            if (encodedFrame.size() >= 8) {

                                LOGI(
                                    "MediaCodec: FRAME A primeiros bytes: "
                                    "%02X %02X %02X %02X %02X %02X %02X %02X",
                                    encodedFrame[0],
                                    encodedFrame[1],
                                    encodedFrame[2],
                                    encodedFrame[3],
                                    encodedFrame[4],
                                    encodedFrame[5],
                                    encodedFrame[6],
                                    encodedFrame[7]
                                );
                            }

                            LOGI(
                                "MediaCodec: FRAME A H264 VIDEO "
                                "bufferSize=%zu ptr=%p encodedSize=%d "
                                "pts=%lld flags=%u",
                                outputBufferSize,
                                outputBuffer,
                                outputInfo.size,
                                static_cast<long long>(
                                    outputInfo.presentationTimeUs
                                ),
                                outputInfo.flags
                            );

                            AMediaCodec_releaseOutputBuffer(
                                encoder,
                                static_cast<size_t>(outputIndex),
                                false
                            );

                            LOGI(
                                "MediaCodec: FRAME A output buffer liberado"
                            );

                            if (
                                decodedFrame == frameB
                            ) {

                                AMediaCodecBufferInfo finalOutputInfo{};

                                ssize_t finalOutputIndex =
                                    AMediaCodec_dequeueOutputBuffer(
                                        encoder,
                                        &finalOutputInfo,
                                        100000
                                    );

                                LOGI(
                                    "MediaCodec: DRAIN B outputIndex=%zd "
                                    "size=%d pts=%lld flags=%u",
                                    finalOutputIndex,
                                    finalOutputInfo.size,
                                    static_cast<long long>(
                                        finalOutputInfo.presentationTimeUs
                                    ),
                                    finalOutputInfo.flags
                                );

                                if (finalOutputIndex >= 0) {

                                    size_t finalBufferSize = 0;

                                    uint8_t* finalBuffer =
                                        AMediaCodec_getOutputBuffer(
                                            encoder,
                                            static_cast<size_t>(
                                                finalOutputIndex
                                            ),
                                            &finalBufferSize
                                        );

                                    if (
                                        finalBuffer != nullptr &&
                                        finalOutputInfo.size > 0
                                    ) {

                                        std::vector<uint8_t> finalEncodedFrame(
                                            finalBuffer,
                                            finalBuffer +
                                                finalOutputInfo.size
                                        );

                                        std::vector<uint8_t> finalAvccFrame =
                                            h264AnnexBToAvcc(
                                                finalEncodedFrame
                                            );

                                        if (
                                            mp4Started &&
                                            !finalAvccFrame.empty()
                                        ) {

                                            AVPacket* finalPacket =
                                                av_packet_alloc();

                                            if (finalPacket) {

                                                finalPacket->data =
                                                    finalAvccFrame.data();

                                                finalPacket->size =
                                                    static_cast<int>(
                                                        finalAvccFrame.size()
                                                    );

                                                finalPacket->stream_index =
                                                    outputVideoStream->index;

                                                finalPacket->pts =
                                                    encodedFrameIndex;

                                                finalPacket->dts =
                                                    encodedFrameIndex;

                                                finalPacket->duration = 1;

                                                av_packet_rescale_ts(
                                                    finalPacket,
                                                    AVRational{1, 30},
                                                    outputVideoStream->time_base
                                                );

                                                int finalWriteResult =
                                                    av_interleaved_write_frame(
                                                        outputFormatContext,
                                                        finalPacket
                                                    );

                                                LOGI(
                                                    "MOV: DRAIN frame=%lld "
                                                    "size=%d pts=%lld dts=%lld write=%d",
                                                    static_cast<long long>(
                                                        encodedFrameIndex
                                                    ),
                                                    finalPacket->size,
                                                    static_cast<long long>(
                                                        finalPacket->pts
                                                    ),
                                                    static_cast<long long>(
                                                        finalPacket->dts
                                                    ),
                                                    finalWriteResult
                                                );

                                                av_packet_free(
                                                    &finalPacket
                                                );

                                                if (
                                                    finalWriteResult >= 0
                                                ) {
                                                    ++encodedFrameIndex;
                                                }
                                            }
                                        }
                                    }

                                    AMediaCodec_releaseOutputBuffer(
                                        encoder,
                                        static_cast<size_t>(
                                            finalOutputIndex
                                        ),
                                        false
                                    );
                                }
                            }

                        }

                    } else {

                        LOGI(
                            "MediaCodec: FRAME A buffer insuficiente "
                            "ou ponteiro NULL"
                        );
                    }
                }

            }


            if (
                decodedFrame >= frameA &&
                decodedFrame <= frameB
            ) {
                framesInCut++;
            }

            if (decodedFrame == frameB) {

                foundB = true;
                ptsB = frame->pts;

                if (
                    audioStreamIndex >= 0
                ) {

                    AVRational videoTimeBase =
                        videoStream->time_base;

                    AVRational audioTimeBase =
                        formatContext
                            ->streams[audioStreamIndex]
                            ->time_base;

                    audioStartPts =
                        av_rescale_q(
                            ptsA,
                            videoTimeBase,
                            audioTimeBase
                        );

                    audioEndPts =
                        av_rescale_q(
                            ptsB,
                            videoTimeBase,
                            audioTimeBase
                        );

                    LOGI(
                        "FFmpeg: AUDIO corte pts=%lld -> %lld",
                        static_cast<long long>(
                            audioStartPts
                        ),
                        static_cast<long long>(
                            audioEndPts
                        )
                    );
                }

                LOGI(
                    "FFmpeg: frames no corte = %lld",
                    static_cast<long long>(
                        framesInCut
                    )
                );

                LOGI(
                    "FFmpeg: FRAME B encontrado: %lld pts=%lld",
                    static_cast<long long>(
                        decodedFrame
                    ),
                    static_cast<long long>(
                        frame->pts
                    )
                );

                break;
            }


            ++decodedFrame;
        }
    }

    if (
        foundB &&
        audioStreamIndex >= 0 &&
        audioEndPts != AV_NOPTS_VALUE
    ) {

        while (!audioEndFound) {

            result =
                av_read_frame(
                    formatContext,
                    packet
                );

            if (result < 0) {
                break;
            }

            if (
                packet->stream_index
                == audioStreamIndex
            ) {

                if (
                    packet->pts != AV_NOPTS_VALUE &&
                    packet->duration > 0
                ) {

                    int64_t packetEndPts =
                        packet->pts +
                        packet->duration;

                    if (
                        packet->pts < audioEndPts &&
                        packetEndPts > audioEndPts
                    ) {

                        AVPacket* audioPacket =
                            av_packet_alloc();

                        if (audioPacket) {

                            if (
                                av_packet_ref(
                                    audioPacket,
                                    packet
                                ) == 0
                            ) {

                                audioPackets.push_back(
                                    audioPacket
                                );

                                audioEndFound = true;

                                LOGI(
                                    "FFmpeg: AAC pacote final capturado "
                                    "pts=%lld duration=%lld end=%lld",
                                    static_cast<long long>(
                                        packet->pts
                                    ),
                                    static_cast<long long>(
                                        packet->duration
                                    ),
                                    static_cast<long long>(
                                        packetEndPts
                                    )
                                );

                            } else {

                                av_packet_free(
                                    &audioPacket
                                );
                            }
                        }
                    }
                }
            }

            av_packet_unref(packet);
        }
    }


    LOGI(
        "FFmpeg: total de pacotes AAC armazenados=%zu",
        audioPackets.size()
    );

    if (!audioPackets.empty()) {

        LOGI(
            "FFmpeg: AAC primeiro pts=%lld dts=%lld duration=%lld",
            static_cast<long long>(
                audioPackets.front()->pts
            ),
            static_cast<long long>(
                audioPackets.front()->dts
            ),
            static_cast<long long>(
                audioPackets.front()->duration
            )
        );

        LOGI(
            "FFmpeg: AAC último pts=%lld dts=%lld duration=%lld",
            static_cast<long long>(
                audioPackets.back()->pts
            ),
            static_cast<long long>(
                audioPackets.back()->dts
            ),
            static_cast<long long>(
                audioPackets.back()->duration
            )
        );
    }

    LOGI(
        "FFmpeg: testCutFrames() -> resultado A=%s B=%s frames_decodificados=%lld",
        foundA ? "OK" : "NÃO",
        foundB ? "OK" : "NÃO",
        static_cast<long long>(decodedFrame)
    );

    jlongArray resultArray =
        env->NewLongArray(6);

    if (resultArray) {

        jlong values[6] = {
            frameA,
            ptsA,
            frameB,
            ptsB,
            videoStream->time_base.num,
            videoStream->time_base.den
        };

        env->SetLongArrayRegion(
            resultArray,
            0,
            6,
            values
        );
    }

    if (
        mp4Started &&
        outputFormatContext &&
        outputAudioStream &&
        audioStartPts != AV_NOPTS_VALUE &&
        audioEndPts != AV_NOPTS_VALUE
    ) {

        int audioPacketsWritten = 0;

        for (
            AVPacket* audioPacket : audioPackets
        ) {

            if (!audioPacket) {
                continue;
            }

            if (
                audioPacket->pts == AV_NOPTS_VALUE ||
                audioPacket->duration <= 0
            ) {
                continue;
            }

            int64_t packetStart =
                audioPacket->pts;

            int64_t packetEnd =
                audioPacket->pts +
                audioPacket->duration;

            if (
                packetEnd <= audioStartPts ||
                packetStart >= audioEndPts
            ) {
                continue;
            }

            AVPacket* outputAudioPacket =
                av_packet_alloc();

            if (!outputAudioPacket) {
                LOGI(
                    "MOV: falha ao alocar pacote AAC"
                );
                continue;
            }

            int copyResult =
                av_packet_ref(
                    outputAudioPacket,
                    audioPacket
                );

            if (copyResult < 0) {

                LOGI(
                    "MOV: av_packet_ref() AAC falhou: %d",
                    copyResult
                );

                av_packet_free(
                    &outputAudioPacket
                );

                continue;
            }

            outputAudioPacket->stream_index =
                outputAudioStream->index;

            outputAudioPacket->pts =
                av_rescale_q(
                    packetStart - audioStartPts,
                    formatContext
                        ->streams[audioStreamIndex]
                        ->time_base,
                    outputAudioStream->time_base
                );

            outputAudioPacket->dts =
                av_rescale_q(
                    audioPacket->dts != AV_NOPTS_VALUE
                        ? audioPacket->dts - audioStartPts
                        : packetStart - audioStartPts,
                    formatContext
                        ->streams[audioStreamIndex]
                        ->time_base,
                    outputAudioStream->time_base
                );

            outputAudioPacket->duration =
                audioPacket->duration;

            int writeResult =
                av_interleaved_write_frame(
                    outputFormatContext,
                    outputAudioPacket
                );

            LOGI(
                "MOV: AAC packet pts=%lld dts=%lld "
                "duration=%lld write=%d",
                static_cast<long long>(
                    outputAudioPacket->pts
                ),
                static_cast<long long>(
                    outputAudioPacket->dts
                ),
                static_cast<long long>(
                    outputAudioPacket->duration
                ),
                writeResult
            );

            if (writeResult < 0) {

                char errorBuffer[
                    AV_ERROR_MAX_STRING_SIZE
                ];

                av_strerror(
                    writeResult,
                    errorBuffer,
                    sizeof(errorBuffer)
                );

                LOGI(
                    "MOV: AAC write falhou: %s",
                    errorBuffer
                );

            } else {

                ++audioPacketsWritten;
            }

            av_packet_free(
                &outputAudioPacket
            );
        }

        LOGI(
            "MOV: AAC packets escritos=%d",
            audioPacketsWritten
        );
    }

    for (
        AVPacket* audioPacket : audioPackets
    ) {

        av_packet_free(
            &audioPacket
        );
    }

    audioPackets.clear();


    if (
        mp4Started &&
        outputFormatContext
    ) {

        int trailerResult =
            av_write_trailer(
                outputFormatContext
            );

        if (trailerResult < 0) {

            char errorBuffer[
                AV_ERROR_MAX_STRING_SIZE
            ];

            av_strerror(
                trailerResult,
                errorBuffer,
                sizeof(errorBuffer)
            );

            LOGI(
                "MOV: av_write_trailer() falhou: %s",
                errorBuffer
            );

        } else {

            LOGI(
                "MOV: av_write_trailer() OK"
            );
        }

        mp4Started = false;
    }

    if (
        outputFormatContext &&
        !(outputFormatContext->oformat->flags & AVFMT_NOFILE)
    ) {

        int closeResult =
            avio_closep(
                &outputFormatContext->pb
            );

        LOGI(
            "MOV: avio_closep() -> %d",
            closeResult
        );
    }

    if (outputFormatContext) {

        avformat_free_context(
            outputFormatContext
        );

        outputFormatContext = nullptr;

        LOGI(
            "MOV: avformat_free_context() OK"
        );
    }

    av_packet_free(
        &packet
    );

    av_frame_free(
        &frame
    );

    avcodec_free_context(
        &codecContext
    );

    avformat_close_input(
        &formatContext
    );

    if (ioContext) {
        av_freep(
            &ioContext->buffer
        );
        avio_context_free(
            &ioContext
        );
    }

    close(sourceFd);

    LOGI(
        "FFmpeg: testCutFrames() -> concluído"
    );

    if (encoder) {

        AMediaCodec_stop(
            encoder
        );

        LOGI(
            "MediaCodec: stop() OK"
        );

        AMediaCodec_delete(
            encoder
        );

        LOGI(
            "MediaCodec: delete() OK"
        );
    }

    return resultArray;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeProbeFd(
    JNIEnv* env,
    jobject thiz,
    jint fd
)
{
    if (fd < 0) {

        LOGI(
            "FFmpeg: probeFd() -> fd inválido: %d",
            fd
        );

        return;
    }

    int probeFd =
        dup(fd);

    if (probeFd < 0) {

        LOGI(
            "FFmpeg: probeFd() -> dup() falhou"
        );

        return;
    }

    LOGI(
        "FFmpeg: probeFd() -> fd original=%d, cópia=%d",
        fd,
        probeFd
    );

    unsigned char* ioBuffer =
        static_cast<unsigned char*>(
            av_malloc(32768)
        );

    if (!ioBuffer) {
        LOGI(
            "FFmpeg: probeFd() -> av_malloc() falhou"
        );
        close(probeFd);
        return;
    }

    AVIOContext* ioContext =
        avio_alloc_context(
            ioBuffer,
            32768,
            0,
            &probeFd,

            [](void* opaque, uint8_t* buffer, int bufferSize) -> int {

                int* fdPtr =
                    static_cast<int*>(opaque);

                if (!fdPtr || *fdPtr < 0) {
                    return AVERROR(EINVAL);
                }

                ssize_t bytesRead =
                    read(*fdPtr, buffer, static_cast<size_t>(bufferSize));

                if (bytesRead < 0) {
                    LOGI(
                        "FFmpeg: AVIO READ ERRO fd=%d errno=%d",
                        *fdPtr,
                        errno
                    );
                    return AVERROR(errno);
                }

                if (bytesRead == 0) {
                    LOGI(
                        "FFmpeg: AVIO READ EOF fd=%d",
                        *fdPtr
                    );
                    return AVERROR_EOF;
                }

                return static_cast<int>(bytesRead);


            },

            nullptr,

            [](void* opaque, int64_t offset, int whence) -> int64_t {
                int* fdPtr = static_cast<int*>(opaque);

                if (!fdPtr || *fdPtr < 0) {
                    return AVERROR(EINVAL);
                }

                if (whence == AVSEEK_SIZE) {
                    off_t current = lseek(*fdPtr, 0, SEEK_CUR);
                    off_t end = lseek(*fdPtr, 0, SEEK_END);

                    if (end < 0) {
                        return AVERROR(errno);
                    }

                    if (lseek(*fdPtr, current, SEEK_SET) < 0) {
                        return AVERROR(errno);
                    }

                    return static_cast<int64_t>(end);
                }

                int seekWhence = whence & 0xFFFF;

                off_t result =
                    lseek(
                        *fdPtr,
                        static_cast<off_t>(offset),
                        seekWhence
                    );

                if (result < 0) {
                    LOGI(
                        "FFmpeg: AVIO SEEK ERRO offset=%lld whence=0x%x errno=%d",
                        static_cast<long long>(offset),
                        whence,
                        errno
                    );

                    return AVERROR(errno);
                }

                return static_cast<int64_t>(result);
            }
        );

    if (!ioContext) {
        LOGI(
            "FFmpeg: probeFd() -> avio_alloc_context() falhou"
        );
        av_free(ioBuffer);
        close(probeFd);
        return;
    }

    AVFormatContext* formatContext =
        avformat_alloc_context();

    if (!formatContext) {

        LOGI(
            "FFmpeg: probeFd() -> avformat_alloc_context() falhou"
        );

        av_freep(
            &ioContext->buffer
        );

        avio_context_free(
            &ioContext
        );

        close(probeFd);

        return;
    }

    formatContext->pb =
        ioContext;

    formatContext->flags |=
        AVFMT_FLAG_CUSTOM_IO;

    int result =
        avformat_open_input(
            &formatContext,
            nullptr,
            nullptr,
            nullptr
        );

    if (result < 0) {

        char errorBuffer[
            AV_ERROR_MAX_STRING_SIZE
        ];

        av_strerror(
            result,
            errorBuffer,
            sizeof(errorBuffer)
        );

        LOGI(
            "FFmpeg: avformat_open_input() falhou: %s",
            errorBuffer
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );

            avio_context_free(
                &ioContext
            );
        }

        close(probeFd);

        return;
    }

    result =
        avformat_find_stream_info(
            formatContext,
            nullptr
        );

    if (result < 0) {

        char errorBuffer[
            AV_ERROR_MAX_STRING_SIZE
        ];

        av_strerror(
            result,
            errorBuffer,
            sizeof(errorBuffer)
        );

        LOGI(
            "FFmpeg: avformat_find_stream_info() falhou: %s",
            errorBuffer
        );

        avformat_close_input(
            &formatContext
        );

        if (ioContext) {
            av_freep(
                &ioContext->buffer
            );

            avio_context_free(
                &ioContext
            );
        }

        close(probeFd);

        return;
    }

    LOGI(
        "FFmpeg: formato=%s, streams=%u, duração=%lld us",
        formatContext->iformat
            ? formatContext->iformat->name
            : "unknown",
        formatContext->nb_streams,
        static_cast<long long>(
            formatContext->duration
        )
    );

    for (
        unsigned int i = 0;
        i < formatContext->nb_streams;
        ++i
    ) {

        AVStream* stream =
            formatContext->streams[i];

        if (
            stream->codecpar->codec_type
            == AVMEDIA_TYPE_VIDEO
        ) {

            LOGI(
                "FFmpeg: vídeo stream=%u codec=%s",
                i,
                avcodec_get_name(
                    stream->codecpar->codec_id
                )
            );

            LOGI(
                "FFmpeg: vídeo %dx%d",
                stream->codecpar->width,
                stream->codecpar->height
            );

            LOGI(
                "FFmpeg: FPS=%d/%d",
                stream->avg_frame_rate.num,
                stream->avg_frame_rate.den
            );
        }

        if (
            stream->codecpar->codec_type
            == AVMEDIA_TYPE_AUDIO
        ) {

            LOGI(
                "FFmpeg: áudio stream=%u codec=%s",
                i,
                avcodec_get_name(
                    stream->codecpar->codec_id
                )
            );
        }
    }

    avformat_close_input(
        &formatContext
    );

    if (ioContext) {
        av_freep(
            &ioContext->buffer
        );

        avio_context_free(
            &ioContext
        );
    }

    close(probeFd);

    LOGI(
        "FFmpeg: probeFd() concluído"
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


extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeSetSurface(
    JNIEnv* env,
    jobject thiz,
    jlong handle,
    jobject surface
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context) {

        LOGI(
            "JNI: nativeSetSurface() -> contexto inválido"
        );

        return;
    }

    ANativeWindow* window = nullptr;

    if (surface) {

        LOGI(
            "JNI: nativeSetSurface() -> Surface recebida"
        );

        window =
            ANativeWindow_fromSurface(
                env,
                surface
            );

        if (!window) {

            LOGI(
                "JNI: ANativeWindow_fromSurface() falhou"
            );

            return;
        }

        LOGI(
            "JNI: ANativeWindow adquirido: %p",
            static_cast<void*>(window)
        );

    } else {

        LOGI(
            "JNI: nativeSetSurface() -> Surface NULL"
        );
    }


    /* ========================================================
     * ENTREGA AO MpvContext
     * ======================================================== */

    LOGI(
        "JNI: enviando ANativeWindow ao MpvContext: %p",
        static_cast<void*>(window)
    );

    mpv_context_set_surface(
        context,
        window
    );


    /* ========================================================
     * LIBERA A REFERÊNCIA TEMPORÁRIA
     * ======================================================== */

    if (window) {

        LOGI(
            "JNI: liberando referência temporária: %p",
            static_cast<void*>(window)
        );

        ANativeWindow_release(
            window
        );
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeRender(
    JNIEnv* env,
    jobject thiz,
    jlong handle
)
{
    MpvContext* context =
        reinterpret_cast<MpvContext*>(handle);

    if (!context) {
        __android_log_print(
            ANDROID_LOG_ERROR,
            "GPIV_NATIVE",
            "JNI: nativeRender() -> contexto inválido"
        );

        return JNI_FALSE;
    }

    bool rendered =
        mpv_context_render(context);

    return rendered
        ? JNI_TRUE
        : JNI_FALSE;
}
