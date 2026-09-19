#include <jni.h>
#include <android/log.h>
#include <android/native_window_jni.h>

#include "mpv_context.h"

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

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
