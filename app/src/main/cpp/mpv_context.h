#ifndef GPIV_MPV_CONTEXT_H
#define GPIV_MPV_CONTEXT_H

#include <mpv/client.h>

#include <jni.h>

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
};

MpvContext* mpv_context_create();

int mpv_context_initialize(
    MpvContext* context
);

void mpv_context_destroy(
    MpvContext* context
);

#endif
