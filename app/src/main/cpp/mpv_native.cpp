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

#include <string>
#include <unistd.h>
#include <errno.h>

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

    bool foundA = false;
    bool foundB = false;

    int64_t ptsA = AV_NOPTS_VALUE;
    int64_t ptsB = AV_NOPTS_VALUE;

    int videoPacketCount = 0;

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

            if (decodedFrame == frameA) {

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
            }

            if (decodedFrame == frameB) {

                foundB = true;
                ptsB = frame->pts;

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

    LOGI(
        "FFmpeg: testCutFrames() -> resultado A=%s B=%s frames_decodificados=%lld",
        foundA ? "OK" : "NÃO",
        foundB ? "OK" : "NÃO",
        static_cast<long long>(decodedFrame)
    );

    jlongArray resultArray =
        env->NewLongArray(4);

    if (resultArray) {

        jlong values[4] = {
            frameA,
            ptsA,
            frameB,
            ptsB
        };

        env->SetLongArrayRegion(
            resultArray,
            0,
            4,
            values
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
