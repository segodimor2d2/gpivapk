#ifndef GPIV_MPV_CONTEXT_H
#define GPIV_MPV_CONTEXT_H

#include <mpv/client.h>
#include <mpv/render.h>

#include <jni.h>

#include <android/native_window.h>

#include <EGL/egl.h>

#include <atomic>
#include <thread>
#include <string>


struct MpvStreamManager;

struct MpvContext {


    /* ========================================================
     * MPV
     * ======================================================== */

    mpv_handle* mpv;

    mpv_render_context* renderContext;

    std::string screenshotDirectory;

    std::atomic<bool> screenshotRequested;

    /* ========================================================
     * EVENT LOOP
     * ======================================================== */

    std::atomic<bool> eventLoopRunning;

    std::thread eventThread;


    /* ========================================================
     * JNI
     * ======================================================== */

    JavaVM* javaVm;

    jobject nativeObject;

    jmethodID onDoubleProperty;

    jmethodID onBooleanProperty;

    jmethodID onStringProperty;

    jmethodID onLoadingChanged;
  
    jmethodID onScreenshot;

    /* ========================================================
     * ANDROID SURFACE
     * ======================================================== */

    ANativeWindow* window;

    bool surfaceAvailable;


    /* ========================================================
     * EGL
     * ======================================================== */

    EGLDisplay eglDisplay;

    EGLConfig eglConfig;

    EGLContext eglContext;

    EGLSurface eglSurface;

    bool eglInitialized;

    MpvStreamManager* streamManager;
};


/* ============================================================
 * API
 * ============================================================ */

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

void mpv_context_set_screenshot_directory(
    MpvContext* context,
    const char* directory
);

bool mpv_context_render(MpvContext* context);

#endif
