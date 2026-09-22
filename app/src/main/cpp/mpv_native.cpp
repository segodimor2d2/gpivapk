#include <jni.h>
#include <android/log.h>

#include "mpv_context.h"

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
