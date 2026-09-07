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


    env->DeleteLocalRef(clazz);


    /* ========================================================
     * VALIDA CALLBACKS
     * ======================================================== */

    if (
        !context->onDoubleProperty ||
        !context->onBooleanProperty ||
        !context->onStringProperty ||
        !context->onLoadingChanged
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
Java_com_rec_gpiv_player_MpvNative_nativeFrameForward(
    JNIEnv* env,
    jobject thiz,
    jlong handle
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

    const char* command[] = {
        "frame-step",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(frame-step) -> %d",
        status
    );
}


extern "C"
JNIEXPORT void JNICALL
Java_com_rec_gpiv_player_MpvNative_nativeFrameBackward(
    JNIEnv* env,
    jobject thiz,
    jlong handle
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

    const char* command[] = {
        "frame-back-step",
        nullptr
    };

    int status =
        mpv_command(
            context->mpv,
            command
        );

    LOGI(
        "JNI: mpv_command(frame-back-step) -> %d",
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
