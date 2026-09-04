#include "mpv_context.h"
#include "mpv_context_egl.h"

#include <cstring>

#include <android/log.h>

#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <mpv/render_gl.h>
#include <new>


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


/* ============================================================
 * IDENTIFICADORES DAS PROPRIEDADES OBSERVADAS
 * ============================================================ */

#define PROPERTY_TIME_POS   1
#define PROPERTY_DURATION   2
#define PROPERTY_PAUSE      3
#define PROPERTY_FILENAME   4



/* ============================================================
 * RENDER
 * ============================================================ */

/*
 * Executa uma renderização do mpv na EGLSurface atual.
 *
 * O framebuffer padrão da EGLSurface é usado como destino.
 *
 * O FLIP_Y é necessário para manter a orientação correta
 * do vídeo no Android.
 */

static bool render_mpv(
    MpvContext* context,
    mpv_render_param* params
)
{
    if (!context)
        return false;

    if (!context->renderContext)
        return false;

    if (!params)
        return false;

    mpv_render_context_render(
        context->renderContext,
        params
    );

    return true;
}

static void report_mpv_swap(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->renderContext)
        return;

    mpv_render_context_report_swap(
        context->renderContext
    );
}

static bool render(
    MpvContext* context
)
{
    if (!context) {

        LOGE(
            "Render: context == null"
        );

        return false;
    }


    if (!context->renderContext) {

        LOGE(
            "Render: renderContext == null"
        );

        return false;
    }


    if (!context->eglInitialized) {

        LOGE(
            "Render: EGL não inicializado"
        );

        return false;
    }


    if (!context->window) {

        LOGE(
            "Render: window == null"
        );

        return false;
    }


    /*
     * ========================================================
     * TAMANHO DA SURFACE
     * ========================================================
     */

    int width =
        ANativeWindow_getWidth(
            context->window
        );


    int height =
        ANativeWindow_getHeight(
            context->window
        );

    if (
        width <= 0 ||
        height <= 0
    ) {

        LOGE(
            "Render: tamanho inválido"
        );

        return false;
    }


    /*
     * ========================================================
     * OPENGL FBO
     * ========================================================
     *
     * FBO 0 = framebuffer padrão da EGLSurface.
     */

    mpv_opengl_fbo fbo = {
        .fbo = 0,
        .w = width,
        .h = height,
        .internal_format = 0
    };

    int flip_y = 1;

    /*
     * ========================================================
     * PARÂMETROS DO RENDER
     * ========================================================
     */

    mpv_render_param params[] = {

        {
            MPV_RENDER_PARAM_OPENGL_FBO,
            &fbo
        },

        {
            MPV_RENDER_PARAM_FLIP_Y,
            &flip_y
        },

        {
            MPV_RENDER_PARAM_INVALID,
            nullptr
        }
    };


    /*
     * ========================================================
     * MPV RENDER
     * ========================================================
     */

    if (!render_mpv(context, params))
    {
        return false;
    }

    /*
     * ========================================================
     * SWAP BUFFERS
     * ========================================================
     */

    if (!mpv_context_egl_swap(context))
        return false;

    report_mpv_swap(context);

    return true;
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


    context->mpv = mpv_create();


    mpv_request_log_messages(
        context->mpv,
        "info"
    );


    if (!context->mpv) {

        LOGI(
            "MpvContext: mpv_create() falhou"
        );

        delete context;

        return nullptr;
    }


    context->renderContext = nullptr;


    context->eglDisplay =
        EGL_NO_DISPLAY;


    context->eglConfig =
        nullptr;


    context->eglContext =
        EGL_NO_CONTEXT;


    context->eglSurface =
        EGL_NO_SURFACE;


    context->eglInitialized =
        false;

    context->eventLoopRunning = false;

    context->javaVm = nullptr;

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


static bool configure_mpv(
    MpvContext* context
)
{
    if (!context || !context->mpv)
        return false;

    /*
     * Neste projeto usamos o render API do libmpv.
     *
     * Portanto não queremos que o mpv tente selecionar
     * automaticamente uma saída de vídeo como mediacodec_embed
     * antes de existir uma Surface.
     */

    int status =
        mpv_set_option_string(
            context->mpv,
            "vo",
            "libmpv"
        );

    if (status < 0) {
        LOGI(
            "mpv_set_option_string(vo) falhou: %s",
            mpv_error_string(status)
        );

        return false;
    }

    return true;
}


static bool initialize_mpv(
    MpvContext* context
)
{
    if (!context || !context->mpv)
        return false;

    LOGI(
        "mpv: mpv_initialize()"
    );

    int status =
        mpv_initialize(
            context->mpv
        );

    if (status < 0) {
        LOGI(
            "mpv_initialize() falhou: %s",
            mpv_error_string(status)
        );

        return false;
    }

    LOGI(
        "mpv: inicializado"
    );

    return true;
}


static int observe_mpv_properties(
    MpvContext* context
)
{
    if (!context || !context->mpv)
        return -1;

    int status =
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

    return 0;
}


static bool start_event_loop(
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
     * CONFIGURA MPV
     * ======================================================== */

    if (!configure_mpv(context))
        return -1;


    /* ========================================================
     * INICIALIZA MPV
     * ======================================================== */

    if (!initialize_mpv(context))
        return -1;


    /* ========================================================
     * OBSERVA PROPRIEDADES
     * ======================================================== */

    int status =
        observe_mpv_properties(context);

    if (status < 0)
        return status;


    /* ========================================================
     * INICIA EVENT LOOP
     * ======================================================== */

    if (!start_event_loop(context))
        return -1;


    LOGI(
        "MpvContext: inicialização concluída"
    );


    return 0;
}

static void remove_current_surface(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->window)
        return;

    LOGI(
        "MpvContext: removendo Surface anterior"
    );

    /*
     * A Surface Android pode ser destruída e recriada
     * durante a vida do player.
     *
     * Não destruímos o mpv_render_context nem o EGLContext.
     * Apenas removemos a EGLSurface associada à janela anterior.
     */

    mpv_context_egl_destroy_surface(context);

    ANativeWindow_release(
        context->window
    );

    context->window = nullptr;
    context->surfaceAvailable = false;
}

static bool acquire_surface(
    MpvContext* context,
    ANativeWindow* window
)
{
    if (!context || !window)
        return false;

    ANativeWindow_acquire(
        window
    );

    context->window =
        window;

    context->surfaceAvailable =
        true;

    LOGI(
        "MpvContext: ANativeWindow adquirido: %p",
        context->window
    );

    return true;
}


static bool setup_egl_for_surface(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (
        context->eglDisplay == EGL_NO_DISPLAY ||
        context->eglContext == EGL_NO_CONTEXT
    )
    {
        LOGI(
            "MpvContext: EGL ainda não existe, "
            "criando EGL completo"
        );

        if (!mpv_context_egl_create(context))
        {
            LOGE(
                "MpvContext: falha ao criar EGL"
            );

            return false;
        }

        LOGI(
            "MpvContext: EGL inicializado com sucesso"
        );
    }
    else
    {
        LOGI(
            "MpvContext: reutilizando EGL existente"
        );

        if (!mpv_context_egl_create_surface(context))
        {
            LOGE(
                "MpvContext: falha ao recriar EGLSurface"
            );

            return false;
        }

        LOGI(
            "MpvContext: EGLSurface recriada com sucesso"
        );
    }

    return true;
}


static bool create_mpv_render_context(
    MpvContext* context
)
{
    if (!context)
        return false;

    LOGI(
        "MpvContext: criando mpv_render_context"
    );

    mpv_opengl_init_params gl_init_params = {
        .get_proc_address =
            [](void* /* ctx */, const char* name) {
                return mpv_context_egl_get_proc_address(name);
            },
        .get_proc_address_ctx = context
    };

    mpv_render_param params[] = {
        {
            MPV_RENDER_PARAM_API_TYPE,

            const_cast<char*>(
                MPV_RENDER_API_TYPE_OPENGL
            )
        },

        {
            MPV_RENDER_PARAM_OPENGL_INIT_PARAMS,
            &gl_init_params
        },

        {
            MPV_RENDER_PARAM_INVALID,
            nullptr
        }
    };

    int status =
        mpv_render_context_create(
            &context->renderContext,
            context->mpv,
            params
        );

    if (status < 0)
    {
        LOGE(
            "MpvContext: "
            "mpv_render_context_create() falhou: %s",
            mpv_error_string(status)
        );

        context->renderContext = nullptr;

        return false;
    }

    LOGI(
        "MpvContext: mpv_render_context criado: %p",
        context->renderContext
    );

    return true;
}

static void release_surface(
    MpvContext* context
)
{
    if (!context)
        return;

    if (context->window) {
        ANativeWindow_release(
            context->window
        );

        context->window = nullptr;
    }

    context->surfaceAvailable = false;
}

/* ============================================================
 * SURFACE ANDROID
 * ============================================================ */

void mpv_context_set_surface(
    MpvContext* context,
    ANativeWindow* window
)
{
    if (!context)
        return;


    LOGI(
        "MpvContext: set_surface()"
    );

    /*
     * ========================================================
     * REMOVER SURFACE ANTERIOR
     * ========================================================
     */

    remove_current_surface(context);

    /*
     * ========================================================
     * NENHUMA SURFACE
     * ========================================================
     */

    if (!window)
    {
        LOGI(
            "MpvContext: Surface removida"
        );

        return;
    }


    /*
     * ========================================================
     * ADQUIRIR ANativeWindow
     * ========================================================
     */

    if (!acquire_surface(context, window))
    {
        LOGE(
            "MpvContext: falha ao adquirir ANativeWindow"
        );

        return;
    }

    /*
     * ========================================================
     * EGL
     * ========================================================
     */

    if (!setup_egl_for_surface(context))
    {
        release_surface(context);
        return;
    }

    if (context->renderContext)
    {
        LOGI(
            "MpvContext: reutilizando mpv_render_context"
        );

        return;
    }

    if (!create_mpv_render_context(context))
    {
        mpv_context_egl_destroy(context);
        release_surface(context);

        return;
    }

}


static void destroy_mpv_render_context(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->renderContext)
        return;

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

static void destroy_mpv(
    MpvContext* context
)
{
    if (!context)
        return;

    if (!context->mpv)
        return;

    LOGI(
        "mpv: destruindo mpv"
    );

    mpv_terminate_destroy(
        context->mpv
    );

    context->mpv =
        nullptr;
}

static void release_jni_reference(
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

static void stop_event_loop(
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

/* ============================================================
 * DESTRUIÇÃO
 * ============================================================ */

void mpv_context_destroy(
    MpvContext* context
)
{
    if (!context)
        return;

    /* ========================================================
     * EVENT LOOP
     * ======================================================== */

    stop_event_loop(context);

    /* ========================================================
     * MPV RENDER CONTEXT
     * ======================================================== */

    destroy_mpv_render_context(context);

    /* ========================================================
     * MPV
     * ======================================================== */

    destroy_mpv(context);

    /* ========================================================
     * REFERÊNCIA GLOBAL KOTLIN
     * ======================================================== */

    release_jni_reference(context);

    /* ========================================================
     * EGL
     * ======================================================== */

    mpv_context_egl_destroy(context);

    /* ========================================================
     * ANDROID SURFACE
     * ======================================================== */

    release_surface(context);

    LOGI(
        "MpvContext: destruído"
    );

    delete context;
}

bool mpv_context_render(MpvContext* context)
{
    if (!context)
        return false;

    return render(context);
}
