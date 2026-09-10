#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

#include "mpv_context.h"
#include "mpv_context_stream.h"

extern "C" {
#include <libavcodec/jni.h>
}

#include <string>

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
