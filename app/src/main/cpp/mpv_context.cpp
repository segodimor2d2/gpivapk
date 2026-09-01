#include "mpv_context.h"

#include <android/log.h>

#include <EGL/egl.h>

#include <mpv/render_gl.h>

#include <new>


#define LOG_TAG "GPIV_NATIVE"

#define LOGI(...) \
    __android_log_print( \
        ANDROID_LOG_INFO, \
        LOG_TAG, \
        __VA_ARGS__ \
    )


/* ============================================================
 * IDENTIFICADORES DAS PROPRIEDADES OBSERVADAS
 * ============================================================ */

#define PROPERTY_TIME_POS   1
#define PROPERTY_DURATION   2
#define PROPERTY_PAUSE      3
#define PROPERTY_FILENAME   4


/* ============================================================
 * OPENGL / EGL
 * ============================================================ */

/*
 * mpv chama esta função quando precisa obter o endereço
 * de uma função OpenGL.
 *
 * No Android usamos eglGetProcAddress().
 *
 * IMPORTANTE:
 *
 * Neste estágio ainda não estamos criando/renderizando
 * nenhum frame.
 *
 * Estamos apenas fornecendo ao mpv o mecanismo necessário
 * para inicializar o backend OpenGL do render context.
 */
static void* get_proc_address(
    void* /* ctx */,
    const char* name
)
{
    if (!name) {
        return nullptr;
    }


    void* address =
        reinterpret_cast<void*>(
            eglGetProcAddress(name)
        );


    LOGI(
        "OpenGL: get_proc_address(%s) -> %p",
        name,
        address
    );


    return address;
}


/* ============================================================
 * EVENT LOOP
 * ============================================================ */

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


        LOGI(
            "event loop: event_id=%d name=%s",
            event->event_id,
            mpv_event_name(event->event_id)
        );


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


            JNIEnv* env = nullptr;

            bool attached = false;


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

                attached = true;
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


                    jstring value = nullptr;


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

                JNIEnv* env = nullptr;

                bool attached = false;


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

                    attached = true;
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

                JNIEnv* env = nullptr;

                bool attached = false;


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

                    attached = true;
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


/* ============================================================
 * CRIAÇÃO
 * ============================================================ */

MpvContext* mpv_context_create()
{
    MpvContext* context =
        new (std::nothrow) MpvContext;


    if (!context) {

        LOGI(
            "MpvContext: falha ao alocar contexto"
        );

        return nullptr;
    }


    context->mpv =
        mpv_create();


    if (!context->mpv) {

        LOGI(
            "MpvContext: mpv_create() falhou"
        );

        delete context;

        return nullptr;
    }


    context->renderContext =
        nullptr;


    context->eventLoopRunning =
        false;


    context->javaVm =
        nullptr;


    context->nativeObject =
        nullptr;


    context->onDoubleProperty =
        nullptr;


    context->onBooleanProperty =
        nullptr;


    context->onStringProperty =
        nullptr;


    context->onLoadingChanged =
        nullptr;


    context->window =
        nullptr;


    context->surfaceAvailable =
        false;


    LOGI(
        "MpvContext: criado sem Surface"
    );


    LOGI(
        "MpvContext: renderContext=null"
    );


    return context;
}


/* ============================================================
 * INICIALIZAÇÃO
 * ============================================================ */

int mpv_context_initialize(
    MpvContext* context
)
{
    if (
        !context ||
        !context->mpv
    ) {

        return -1;
    }


    /* ========================================================
     * CONFIGURAÇÃO DO MPV
     * ======================================================== */

    /*
     * Estamos usando o render API do libmpv.
     *
     * Portanto o VO deve ser "libmpv".
     */
    LOGI(
        "mpv: configurando vo=libmpv"
    );


    int status =
        mpv_set_option_string(
            context->mpv,
            "vo",
            "libmpv"
        );


    if (status < 0) {

        LOGI(
            "mpv_set_option_string(vo=libmpv) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    /* ========================================================
     * INICIALIZA MPV
     * ======================================================== */

    LOGI(
        "mpv: mpv_initialize()"
    );


    status =
        mpv_initialize(
            context->mpv
        );


    if (status < 0) {

        LOGI(
            "mpv_initialize() falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    LOGI(
        "mpv: inicializado"
    );


    /* ========================================================
     * PARÂMETROS OPENGL
     * ======================================================== */

    /*
     * mpv_opengl_init_params é definido em:
     *
     *     <mpv/render_gl.h>
     *
     * e não em render.h.
     */
    mpv_opengl_init_params glInitParams{};


    glInitParams.get_proc_address =
        get_proc_address;


    glInitParams.get_proc_address_ctx =
        nullptr;


    LOGI(
        "OpenGL: parâmetros preparados"
    );


    /* ========================================================
     * PARÂMETROS DO RENDER CONTEXT
     * ======================================================== */

    mpv_render_param renderParams[] = {

        {
            MPV_RENDER_PARAM_API_TYPE,
            const_cast<char*>(
                MPV_RENDER_API_TYPE_OPENGL
            )
        },

        {
            MPV_RENDER_PARAM_OPENGL_INIT_PARAMS,
            &glInitParams
        },

        {
            MPV_RENDER_PARAM_INVALID,
            nullptr
        }
    };


    /* ========================================================
     * CRIA RENDER CONTEXT
     * ======================================================== */

    LOGI(
        "mpv: criando mpv_render_context"
    );


    status =
        mpv_render_context_create(
            &context->renderContext,
            context->mpv,
            renderParams
        );


    if (status < 0) {

        LOGI(
            "mpv_render_context_create() falhou: %s",
            mpv_error_string(status)
        );


        context->renderContext =
            nullptr;


        return status;
    }


    LOGI(
        "mpv: mpv_render_context criado: %p",
        static_cast<void*>(
            context->renderContext
        )
    );


    /* ========================================================
     * OBSERVA PROPRIEDADES
     * ======================================================== */

    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_TIME_POS,
            "time-pos",
            MPV_FORMAT_DOUBLE
        );


    if (status < 0) {

        LOGI(
            "mpv_observe_property(time-pos) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_DURATION,
            "duration",
            MPV_FORMAT_DOUBLE
        );


    if (status < 0) {

        LOGI(
            "mpv_observe_property(duration) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_PAUSE,
            "pause",
            MPV_FORMAT_FLAG
        );


    if (status < 0) {

        LOGI(
            "mpv_observe_property(pause) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    status =
        mpv_observe_property(
            context->mpv,
            PROPERTY_FILENAME,
            "filename",
            MPV_FORMAT_STRING
        );


    if (status < 0) {

        LOGI(
            "mpv_observe_property(filename) "
            "falhou: %s",
            mpv_error_string(status)
        );

        return status;
    }


    /* ========================================================
     * INICIA EVENT LOOP
     * ======================================================== */

    context->eventLoopRunning =
        true;


    context->eventThread =
        std::thread(
            mpv_event_loop,
            context
        );


    LOGI(
        "MpvContext: inicialização concluída"
    );


    return 0;
}


/* ============================================================
 * SURFACE ANDROID
 * ============================================================ */

void mpv_context_set_surface(
    MpvContext* context,
    ANativeWindow* window
)
{
    if (!context) {
        return;
    }


    /* ========================================================
     * LIBERA SURFACE ANTERIOR
     * ======================================================== */

    if (context->window) {

        LOGI(
            "Surface: liberando ANativeWindow anterior: %p",
            static_cast<void*>(
                context->window
            )
        );


        ANativeWindow_release(
            context->window
        );


        context->window =
            nullptr;
    }


    context->surfaceAvailable =
        false;


    /* ========================================================
     * INSTALA NOVA SURFACE
     * ======================================================== */

    if (window) {

        /*
         * Adquire nossa própria referência.
         */
        ANativeWindow_acquire(
            window
        );


        context->window =
            window;


        context->surfaceAvailable =
            true;


        LOGI(
            "Surface: ANativeWindow instalada: %p",
            static_cast<void*>(
                context->window
            )
        );


        LOGI(
            "Surface: surfaceAvailable=true"
        );

    } else {

        LOGI(
            "Surface: removida"
        );


        LOGI(
            "Surface: surfaceAvailable=false"
        );
    }
}


/* ============================================================
 * DESTRUIÇÃO
 * ============================================================ */

void mpv_context_destroy(
    MpvContext* context
)
{
    if (!context) {
        return;
    }


    /* ========================================================
     * EVENT LOOP
     * ======================================================== */

    if (context->mpv) {

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
    }


    /* ========================================================
     * MPV RENDER CONTEXT
     * ======================================================== */

    if (context->renderContext) {

        LOGI(
            "mpv: destruindo mpv_render_context: %p",
            static_cast<void*>(
                context->renderContext
            )
        );


        mpv_render_context_free(
            context->renderContext
        );


        context->renderContext =
            nullptr;


        LOGI(
            "mpv: mpv_render_context destruído"
        );
    }


    /* ========================================================
     * MPV
     * ======================================================== */

    if (context->mpv) {

        LOGI(
            "mpv: destruindo mpv"
        );


        mpv_terminate_destroy(
            context->mpv
        );


        context->mpv =
            nullptr;
    }


    /* ========================================================
     * REFERÊNCIA GLOBAL KOTLIN
     * ======================================================== */

    if (
        context->javaVm &&
        context->nativeObject
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
                ) == JNI_OK
            ) {

                attached =
                    true;
            }
        }


        if (env) {

            env->DeleteGlobalRef(
                context->nativeObject
            );
        }


        if (attached) {

            context->javaVm->DetachCurrentThread();
        }


        context->nativeObject =
            nullptr;
    }


    /* ========================================================
     * SURFACE ANDROID
     * ======================================================== */

    if (context->window) {

        LOGI(
            "Surface: liberando ANativeWindow no destroy: %p",
            static_cast<void*>(
                context->window
            )
        );


        ANativeWindow_release(
            context->window
        );


        context->window =
            nullptr;
    }


    context->surfaceAvailable =
        false;


    LOGI(
        "MpvContext: destruído"
    );


    delete context;
}
