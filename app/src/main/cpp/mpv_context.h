#ifndef GPIV_MPV_CONTEXT_H
#define GPIV_MPV_CONTEXT_H

#include <mpv/client.h>
#include <mpv/render.h>

#include <jni.h>
#include <android/native_window.h>

#include <atomic>
#include <thread>


struct MpvContext {

    /*
     * ============================================================
     * MPV
     * ============================================================
     */

    /*
     * Instância principal do mpv.
     */
    mpv_handle* mpv;


    /*
     * Contexto de renderização do mpv.
     *
     * Neste estágio:
     *
     * - é criado em mpv_context_initialize()
     * - ainda não renderizamos nenhum frame
     */
    mpv_render_context* renderContext;


    /*
     * ============================================================
     * EVENT LOOP
     * ============================================================
     */

    std::atomic<bool> eventLoopRunning;

    std::thread eventThread;


    /*
     * ============================================================
     * JNI
     * ============================================================
     */

    JavaVM* javaVm;

    jobject nativeObject;

    jmethodID onDoubleProperty;

    jmethodID onBooleanProperty;

    jmethodID onStringProperty;

    jmethodID onLoadingChanged;


    /*
     * ============================================================
     * SURFACE ANDROID
     * ============================================================
     */

    /*
     * ANativeWindow atualmente instalada.
     *
     * O contexto mantém sua própria referência.
     */
    ANativeWindow* window;


    /*
     * Indica se existe atualmente uma Surface
     * instalada no contexto.
     */
    bool surfaceAvailable;
};


/*
 * ============================================================
 * API
 * ============================================================
 */

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
