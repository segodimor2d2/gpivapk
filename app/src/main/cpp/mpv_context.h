#ifndef GPIV_MPV_CONTEXT_H
#define GPIV_MPV_CONTEXT_H

#include <mpv/client.h>

#include <jni.h>
#include <android/native_window.h>

#include <atomic>
#include <thread>

struct MpvContext {

    mpv_handle* mpv;

    std::atomic<bool> eventLoopRunning;

    std::thread eventThread;

    JavaVM* javaVm;

    jobject nativeObject;

    jmethodID onDoubleProperty;

    jmethodID onBooleanProperty;

    jmethodID onStringProperty;

    jmethodID onLoadingChanged;

    /*
     * Surface Android recebida pelo player.
     *
     * O MpvContext mantém sua própria referência
     * enquanto a Surface estiver disponível.
     */
    ANativeWindow* window;

    /*
     * Indica se existe atualmente uma Surface
     * instalada no contexto.
     *
     * Neste estágio é apenas estado de lifecycle.
     * Ainda não participa da renderização.
     */
    bool surfaceAvailable;
};

MpvContext* mpv_context_create();

int mpv_context_initialize(
    MpvContext* context
);

void mpv_context_destroy(
    MpvContext* context
);

void mpv_context_set_surface(
    MpvContext* context,
    ANativeWindow* window
);

#endif
