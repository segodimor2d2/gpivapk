#include "mpv_context_events.h"

#include <cstring>

#include <android/log.h>

#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print( \
        ANDROID_LOG_INFO, \
        LOG_TAG, \
        __VA_ARGS__ \
    )

#define LOGW(...) \
    __android_log_print( \
        ANDROID_LOG_WARN, \
        "GPIV_NATIVE", \
        __VA_ARGS__ \
    )

#define LOGE(...) \
    __android_log_print( \
        ANDROID_LOG_ERROR, \
        LOG_TAG, \
        __VA_ARGS__ \
    )


static void mpv_event_loop(
    MpvContext* context
)
{
    LOGI(
        "event loop: thread iniciado"
    );


    while (
        context->eventLoopRunning.load()
    ) {

        mpv_event* event =
            mpv_wait_event(
                context->mpv,
                -1.0
            );


        if (!event) {
            continue;
        }

        /* ====================================================
         * PROPERTY CHANGE
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_PROPERTY_CHANGE
        ) {

            mpv_event_property* property =
                static_cast<mpv_event_property*>(
                    event->data
                );


            if (!property) {
                continue;
            }


            if (!property->name) {
                continue;
            }


            /*
             * Só fazemos callback JNI se houver
             * objeto Java/Kotlin associado.
             */

            if (
                !context->javaVm ||
                !context->nativeObject
            ) {

                continue;
            }


            JNIEnv* env =
                nullptr;


            bool attached =
                false;


            if (
                context->javaVm->GetEnv(
                    reinterpret_cast<void**>(&env),
                    JNI_VERSION_1_6
                ) != JNI_OK
            ) {

                if (
                    context->javaVm->AttachCurrentThread(
                        &env,
                        nullptr
                    ) != JNI_OK
                ) {

                    LOGI(
                        "JNI: AttachCurrentThread() falhou"
                    );

                    continue;
                }


                attached =
                    true;
            }


            /* =================================================
             * DOUBLE
             * ================================================= */

            if (
                property->format ==
                MPV_FORMAT_DOUBLE
            ) {

                if (
                    property->data &&
                    context->onDoubleProperty
                ) {

                    double value =
                        *static_cast<double*>(
                            property->data
                        );


                    jstring name =
                        env->NewStringUTF(
                            property->name
                        );


                    env->CallVoidMethod(
                        context->nativeObject,
                        context->onDoubleProperty,
                        name,
                        static_cast<jdouble>(
                            value
                        )
                    );


                    env->DeleteLocalRef(
                        name
                    );
                }
            }


            /* =================================================
             * FLAG
             * ================================================= */

            else if (
                property->format ==
                MPV_FORMAT_FLAG
            ) {

                if (
                    property->data &&
                    context->onBooleanProperty
                ) {

                    int value =
                        *static_cast<int*>(
                            property->data
                        );


                    jstring name =
                        env->NewStringUTF(
                            property->name
                        );


                    env->CallVoidMethod(
                        context->nativeObject,
                        context->onBooleanProperty,
                        name,
                        static_cast<jboolean>(
                            value
                                ? JNI_TRUE
                                : JNI_FALSE
                        )
                    );


                    env->DeleteLocalRef(
                        name
                    );
                }
            }


            /* =================================================
             * STRING
             * ================================================= */

            else if (
                property->format ==
                MPV_FORMAT_STRING
            ) {

                if (
                    context->onStringProperty
                ) {

                    jstring name =
                        env->NewStringUTF(
                            property->name
                        );


                    jstring value =
                        nullptr;


                    if (property->data) {

                        char* stringValue =
                            *static_cast<char**>(
                                property->data
                            );


                        if (stringValue) {

                            value =
                                env->NewStringUTF(
                                    stringValue
                                );
                        }
                    }


                    env->CallVoidMethod(
                        context->nativeObject,
                        context->onStringProperty,
                        name,
                        value
                    );


                    env->DeleteLocalRef(
                        name
                    );


                    if (value) {

                        env->DeleteLocalRef(
                            value
                        );
                    }
                }
            }


            /* =================================================
             * EXCEPTION JNI
             * ================================================= */

            if (
                env->ExceptionCheck()
            ) {

                LOGI(
                    "JNI: exceção durante callback"
                );


                env->ExceptionDescribe();

                env->ExceptionClear();
            }


            if (attached) {

                context->javaVm->DetachCurrentThread();
            }
        }


        /* ====================================================
         * START FILE
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_START_FILE
        ) {

            LOGI(
                "START_FILE: loading=true"
            );


            if (
                context->javaVm &&
                context->nativeObject &&
                context->onLoadingChanged
            ) {

                JNIEnv* env =
                    nullptr;


                bool attached =
                    false;


                if (
                    context->javaVm->GetEnv(
                        reinterpret_cast<void**>(&env),
                        JNI_VERSION_1_6
                    ) != JNI_OK
                ) {

                    if (
                        context->javaVm->AttachCurrentThread(
                            &env,
                            nullptr
                        ) != JNI_OK
                    ) {

                        LOGI(
                            "JNI: AttachCurrentThread() falhou"
                        );

                        continue;
                    }


                    attached =
                        true;
                }


                env->CallVoidMethod(
                    context->nativeObject,
                    context->onLoadingChanged,
                    static_cast<jboolean>(
                        JNI_TRUE
                    )
                );


                if (
                    env->ExceptionCheck()
                ) {

                    LOGI(
                        "JNI: exceção em "
                        "onNativeLoadingChanged(true)"
                    );


                    env->ExceptionDescribe();

                    env->ExceptionClear();
                }


                if (attached) {

                    context->javaVm->DetachCurrentThread();
                }
            }
        }


        /* ====================================================
         * FILE LOADED
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_FILE_LOADED
        ) {

            LOGI(
                "FILE_LOADED: loading=false"
            );


            if (
                context->javaVm &&
                context->nativeObject &&
                context->onLoadingChanged
            ) {

                JNIEnv* env =
                    nullptr;


                bool attached =
                    false;


                if (
                    context->javaVm->GetEnv(
                        reinterpret_cast<void**>(&env),
                        JNI_VERSION_1_6
                    ) != JNI_OK
                ) {

                    if (
                        context->javaVm->AttachCurrentThread(
                            &env,
                            nullptr
                        ) != JNI_OK
                    ) {

                        LOGI(
                            "JNI: AttachCurrentThread() falhou"
                        );

                        continue;
                    }


                    attached =
                        true;
                }


                env->CallVoidMethod(
                    context->nativeObject,
                    context->onLoadingChanged,
                    static_cast<jboolean>(
                        JNI_FALSE
                    )
                );


                if (
                    env->ExceptionCheck()
                ) {

                    LOGI(
                        "JNI: exceção em "
                        "onNativeLoadingChanged(false)"
                    );


                    env->ExceptionDescribe();

                    env->ExceptionClear();
                }


                if (attached) {

                    context->javaVm->DetachCurrentThread();
                }
            }
        }


        /* ====================================================
         * LOG MESSAGE
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_LOG_MESSAGE
        ) {

            mpv_event_log_message* logMessage =
                static_cast<mpv_event_log_message*>(
                    event->data
                );

            if (logMessage) {

                if (
                    logMessage->level &&
                    strcmp(logMessage->level, "error") == 0
                ) {

                    LOGE(
                        "MPV_LOG [%s] %s: %s",
                        logMessage->prefix
                            ? logMessage->prefix
                            : "?",
                        logMessage->level,
                        logMessage->text
                            ? logMessage->text
                            : ""
                    );

                } else if (
                    logMessage->level &&
                    strcmp(logMessage->level, "warn") == 0
                ) {

                    LOGW(
                        "MPV_LOG [%s] %s: %s",
                        logMessage->prefix
                            ? logMessage->prefix
                            : "?",
                        logMessage->level,
                        logMessage->text
                            ? logMessage->text
                            : ""
                    );
                }
            }
        }


        /* ====================================================
         * END FILE
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_END_FILE
        ) {

            mpv_event_end_file* endFile =
                static_cast<mpv_event_end_file*>(
                    event->data
                );


            if (endFile) {

                LOGI(
                    "END_FILE: reason=%d error=%d",
                    endFile->reason,
                    endFile->error
                );


                LOGI(
                    "END_FILE: error_string=%s",
                    mpv_error_string(
                        endFile->error
                    )
                );
            }
        }


        /* ====================================================
         * SHUTDOWN
         * ==================================================== */

        if (
            event->event_id ==
            MPV_EVENT_SHUTDOWN
        ) {

            break;
        }
    }


    LOGI(
        "event loop: thread finalizado"
    );
}


bool mpv_context_events_start(
    MpvContext* context
)
{
    if (!context || !context->mpv)
        return false;

    context->eventLoopRunning =
        true;

    context->eventThread =
        std::thread(
            mpv_event_loop,
            context
        );

    return true;
}


void mpv_context_events_stop(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->mpv)
        return;

    context->eventLoopRunning =
        false;

    mpv_wakeup(
        context->mpv
    );

    if (
        context->eventThread.joinable()
    ) {
        context->eventThread.join();
    }

    LOGI(
        "MpvContext: event loop parado"
    );
}
