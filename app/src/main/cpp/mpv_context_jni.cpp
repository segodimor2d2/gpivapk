#include "mpv_context_jni.h"

#include <android/log.h>
#include <jni.h>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print( \
        ANDROID_LOG_INFO, \
        LOG_TAG, \
        __VA_ARGS__ \
    )


void mpv_context_jni_release_reference(
    MpvContext* context
)
{
    if (!context)
        return;

    if (
        !context->javaVm ||
        !context->nativeObject
    ) {
        return;
    }

    JNIEnv* env = nullptr;

    jint status =
        context->javaVm->GetEnv(
            reinterpret_cast<void**>(&env),
            JNI_VERSION_1_6
        );

    bool attached = false;

    if (status == JNI_EDETACHED) {
        if (
            context->javaVm->AttachCurrentThread(
                &env,
                nullptr
            ) != JNI_OK
        ) {
            return;
        }

        attached = true;
    }

    if (env) {
        env->DeleteGlobalRef(
            context->nativeObject
        );
    }

    context->nativeObject =
        nullptr;

    if (attached) {
        context->javaVm->DetachCurrentThread();
    }

    LOGI(
        "JNI: referência global liberada"
    );
}
