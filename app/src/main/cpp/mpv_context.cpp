#include "mpv_context.h"

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
 * OPENGL / EGL
 * ============================================================ */

/*
 * mpv chama esta função quando precisa obter o endereço
 * de uma função OpenGL.
 *
 * No Android usamos eglGetProcAddress().
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

    return address;
}


/* ============================================================
 * EGL
 * ============================================================ */

static void log_egl_error(
    const char* operation
)
{
    EGLint error = eglGetError();

    LOGE(
        "EGL: %s falhou: 0x%04x",
        operation,
        error
    );
}


static void destroy_egl(
    MpvContext* context
)
{
    if (!context)
        return;

    /*
     * --------------------------------------------------------
     * Remover o contexto EGL de current
     * --------------------------------------------------------
     */

    if (
        context->eglDisplay != EGL_NO_DISPLAY
    ) {

        eglMakeCurrent(
            context->eglDisplay,
            EGL_NO_SURFACE,
            EGL_NO_SURFACE,
            EGL_NO_CONTEXT
        );
    }

    /*
     * --------------------------------------------------------
     * EGLSurface
     * --------------------------------------------------------
     */

    if (
        context->eglSurface != EGL_NO_SURFACE &&
        context->eglDisplay != EGL_NO_DISPLAY
    ) {

        eglDestroySurface(
            context->eglDisplay,
            context->eglSurface
        );

        context->eglSurface =
            EGL_NO_SURFACE;
    }


    /*
     * --------------------------------------------------------
     * EGLContext
     * --------------------------------------------------------
     */

    if (
        context->eglContext != EGL_NO_CONTEXT &&
        context->eglDisplay != EGL_NO_DISPLAY
    ) {

        eglDestroyContext(
            context->eglDisplay,
            context->eglContext
        );

        context->eglContext =
            EGL_NO_CONTEXT;
    }


    /*
     * --------------------------------------------------------
     * EGLDisplay
     * --------------------------------------------------------
     */

    if (
        context->eglDisplay != EGL_NO_DISPLAY
    ) {

        eglTerminate(
            context->eglDisplay
        );

        context->eglDisplay =
            EGL_NO_DISPLAY;
    }


    context->eglConfig =
        nullptr;

    context->eglInitialized =
        false;


    LOGI(
        "EGL: destruído"
    );
}


static bool create_egl(
    MpvContext* context
)
{
    if (!context)
        return false;


    if (!context->window) {

        LOGE(
            "EGL: não existe ANativeWindow"
        );

        return false;
    }


    /*
     * ========================================================
     * EGLDisplay
     * ========================================================
     */

    context->eglDisplay =
        eglGetDisplay(
            EGL_DEFAULT_DISPLAY
        );


    if (
        context->eglDisplay == EGL_NO_DISPLAY
    ) {

        log_egl_error(
            "eglGetDisplay"
        );

        return false;
    }


    LOGI(
        "EGL: display OK"
    );


    /*
     * ========================================================
     * EGL initialize
     * ========================================================
     */

    EGLint major = 0;
    EGLint minor = 0;


    if (
        !eglInitialize(
            context->eglDisplay,
            &major,
            &minor
        )
    ) {

        log_egl_error(
            "eglInitialize"
        );

        destroy_egl(
            context
        );

        return false;
    }


    LOGI(
        "EGL: initialized %d.%d",
        major,
        minor
    );


    /*
     * ========================================================
     * OpenGL ES API
     * ========================================================
     */

    if (
        !eglBindAPI(
            EGL_OPENGL_ES_API
        )
    ) {

        log_egl_error(
            "eglBindAPI"
        );

        destroy_egl(
            context
        );

        return false;
    }


    LOGI(
        "EGL: OpenGL ES API OK"
    );


    /*
     * ========================================================
     * EGLConfig
     * ========================================================
     */

    const EGLint configAttributes[] = {

        EGL_SURFACE_TYPE,
        EGL_WINDOW_BIT,

        EGL_RENDERABLE_TYPE,
        EGL_OPENGL_ES2_BIT,

        EGL_RED_SIZE,
        8,

        EGL_GREEN_SIZE,
        8,

        EGL_BLUE_SIZE,
        8,

        EGL_ALPHA_SIZE,
        8,

        EGL_NONE
    };


    EGLint numConfigs = 0;


    if (
        !eglChooseConfig(
            context->eglDisplay,
            configAttributes,
            &context->eglConfig,
            1,
            &numConfigs
        )
    ) {

        log_egl_error(
            "eglChooseConfig"
        );

        destroy_egl(
            context
        );

        return false;
    }


    if (numConfigs <= 0) {

        LOGE(
            "EGL: nenhum EGLConfig encontrado"
        );

        destroy_egl(
            context
        );

        return false;
    }


    LOGI(
        "EGL: config OK"
    );


    /*
     * ========================================================
     * EGLContext
     * ========================================================
     */

    const EGLint contextAttributes[] = {

        EGL_CONTEXT_CLIENT_VERSION,
        2,

        EGL_NONE
    };


    context->eglContext =
        eglCreateContext(
            context->eglDisplay,
            context->eglConfig,
            EGL_NO_CONTEXT,
            contextAttributes
        );


    if (
        context->eglContext == EGL_NO_CONTEXT
    ) {

        log_egl_error(
            "eglCreateContext"
        );

        destroy_egl(
            context
        );

        return false;
    }


    LOGI(
        "EGL: context OK"
    );


    /*
     * ========================================================
     * EGLSurface
     * ========================================================
     */

    context->eglSurface =
        eglCreateWindowSurface(
            context->eglDisplay,
            context->eglConfig,
            context->window,
            nullptr
        );


    if (
        context->eglSurface == EGL_NO_SURFACE
    ) {

        log_egl_error(
            "eglCreateWindowSurface"
        );

        destroy_egl(
            context
        );

        return false;
    }


    LOGI(
        "EGL: surface OK"
    );


    /*
     * ========================================================
     * Make current
     * ========================================================
     */

    if (
        !eglMakeCurrent(
            context->eglDisplay,
            context->eglSurface,
            context->eglSurface,
            context->eglContext
        )
    ) {

        log_egl_error(
            "eglMakeCurrent"
        );

        destroy_egl(
            context
        );

        return false;
    }


    LOGI(
        "EGL: makeCurrent OK"
    );

    context->eglInitialized =
        true;


    return true;
}


/* ============================================================
 * TESTE DE RENDERIZAÇÃO
 * ============================================================ */

/*
 * Faz uma única chamada de renderização do mpv.
 *
 * IMPORTANTE:
 *
 * Neste estágio:
 *
 *     - não carregamos vídeo;
 *     - não iniciamos reprodução;
 *     - não criamos loop de renderização;
 *     - não usamos callback de atualização.
 *
 * O objetivo é apenas provar que:
 *
 *     EGL
 *       +
 *     mpv_render_context
 *       +
 *     mpv_render_context_render()
 *       +
 *     eglSwapBuffers()
 *
 * funcionam juntos.
 */


static bool render_test(
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

    int status =
        mpv_render_context_render(
            context->renderContext,
            params
        );

    /*
     * ========================================================
     * SWAP BUFFERS
     * ========================================================
     */

      if (
          !eglSwapBuffers(
              context->eglDisplay,
              context->eglSurface
          )
      ) {

          log_egl_error(
              "eglSwapBuffers"
          );

          return false;
      }

      mpv_render_context_report_swap(
          context->renderContext
      );

      return true;
}


/*
 * ========================================================
 * CALLBACK DE UPDATE DO MPV
 * ========================================================
 */

static void mpv_update_callback(
    void* userdata
)
{
    MpvContext* context =
        static_cast<MpvContext*>(userdata);

    if (!context)
        return;

    context->renderPending.store(
        true,
        std::memory_order_release
    );
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

    context->renderPending = false;

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
     * INICIALIZA MPV
     * ======================================================== */

    LOGI(
        "mpv: mpv_initialize()"
    );


    /*
     * Neste projeto usamos o render API do libmpv.
     *
     * Portanto não queremos que o mpv tente selecionar
     * automaticamente uma saída de vídeo como mediacodec_embed
     * antes de existir uma Surface.
     */

    mpv_set_option_string(
        context->mpv,
        "vo",
        "libmpv"
    );


    int status =
        mpv_initialize(
            context->mpv
        );


    if (status < 0) {

        LOGI(
            "mpv_initialize() falhou: %s",
            mpv_error_string(
                status
            )
        );

        return -1;
    }


    LOGI(
        "mpv: inicializado"
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
            mpv_error_string(
                status
            )
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
            mpv_error_string(
                status
            )
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
            mpv_error_string(
                status
            )
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
            mpv_error_string(
                status
            )
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

    if (context->window)
    {
        LOGI(
            "MpvContext: removendo Surface anterior"
        );


        /*
         * Primeiro destruímos o renderContext.
         *
         * Ele depende do contexto OpenGL/EGL.
         */

        if (context->renderContext)
        {
            LOGI(
                "MpvContext: liberando renderContext anterior"
            );


            mpv_render_context_free(
                context->renderContext
            );


            context->renderContext = nullptr;
        }


        /*
         * Depois destruímos EGL.
         */

        destroy_egl(
            context
        );


        /*
         * Finalmente liberamos a referência
         * para a ANativeWindow.
         */

        ANativeWindow_release(
            context->window
        );


        context->window =
            nullptr;


        context->surfaceAvailable =
            false;
    }


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


    /*
     * ========================================================
     * EGL
     * ========================================================
     */

    if (!create_egl(context))
    {
        LOGE(
            "MpvContext: falha ao criar EGL"
        );


        ANativeWindow_release(
            context->window
        );


        context->window =
            nullptr;


        context->surfaceAvailable =
            false;


        return;
    }


    LOGI(
        "MpvContext: EGL inicializado com sucesso"
    );


    /*
     * ========================================================
     * MPV RENDER CONTEXT
     * ========================================================
     */

    LOGI(
        "MpvContext: criando mpv_render_context"
    );


    mpv_opengl_init_params gl_init_params = {
        .get_proc_address = get_proc_address,
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
            mpv_error_string(
                status
            )
        );


        context->renderContext = nullptr;


        destroy_egl(
            context
        );


        ANativeWindow_release(
            context->window
        );


        context->window =
            nullptr;


        context->surfaceAvailable =
            false;


        return;
    }


    LOGI(
        "MpvContext: mpv_render_context criado: %p",
        context->renderContext
    );

    /*
     * ========================================================
     * CALLBACK DE UPDATE DO MPV
     * ========================================================
     */

    LOGI(
        "MpvContext: registrando mpv_update_callback"
    );

    mpv_render_context_set_update_callback(
        context->renderContext,
        mpv_update_callback,
        context
    );

    LOGI(
        "MpvContext: mpv_update_callback registrado"
    );

    if (mpv_context_take_render_request(context)) {
        LOGI(
            "Render: teste de consumo OK"
        );
    }

    /*
     * ========================================================
     * TESTE DE RENDERIZAÇÃO
     * ========================================================
     *
     * Fazemos apenas UMA renderização.
     *
     * Ainda não existe:
     *
     *     - loop;
     *     - callback;
     *     - vídeo;
     *     - mpv_command(loadfile);
     */

    render_test(
        context
    );
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
     * DESTRUIR EGL
     * ======================================================== */

    if (
        context->eglInitialized ||
        context->eglDisplay != EGL_NO_DISPLAY
    ) {

        destroy_egl(
            context
        );
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


bool mpv_context_take_render_request(
    MpvContext* context
)
{
    if (!context)
        return false;

    bool pending =
        context->renderPending.exchange(
            false,
            std::memory_order_acq_rel
        );

    if (pending) {
        LOGI(
            "Render: solicitação de renderização consumida"
        );
    }

    return pending;
}

bool mpv_context_render_if_pending(
    MpvContext* context
)
{
    if (!context)
        return false;

    if (!mpv_context_take_render_request(context))
        return false;

    LOGI(
        "Render: solicitação pendente detectada"
    );

    render_test(context);

    return true;
}

bool mpv_context_render(MpvContext* context)
{
    if (!context)
        return false;

    return render_test(context);
}
