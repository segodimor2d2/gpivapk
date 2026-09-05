package com.rec.gpiv.player

import android.content.ContentResolver
import android.net.Uri
import android.view.Surface
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MpvPlayer(
    contentResolver: ContentResolver,
    private val native: MpvNative
) : VideoPlayer {

    private val videoSource =
        VideoSource(contentResolver)

    private val _events =
        MutableSharedFlow<PlayerEvent>(
            extraBufferCapacity = 64
        )

    override val events =
        _events.asSharedFlow()

    /*
     * Último estado real de reprodução informado pelo mpv.
     *
     * true  = tocando
     * false = pausado
     */
    private var lastKnownPlaying = false

    /*
     * Estado que deverá ser aplicado ao próximo
     * vídeo depois que ele terminar de carregar.
     *
     * null = nenhum estado pendente.
     */
    private var pendingPlaybackState: Boolean? = null

    override fun initialize() {

        println("MpvPlayer: initialize()")

        setupNativeListeners()

        native.initialize()

        val version = native.getVersion()

        println(
            "MpvPlayer: native version = $version"
        )
    }

    fun setSurface(surface: Surface?) {

        println(
            "MpvPlayer: setSurface($surface)"
        )

        native.setSurface(surface)
    }

    fun loadLocalTestPath(path: String) {

        println(
            "MpvPlayer: loadLocalTestPath($path)"
        )

        native.loadLocalPath(path)
    }

    override fun load(uri: Uri) {

        println(
            "MpvPlayer: load($uri)"
        )

        /*
         * Preserva o estado de reprodução atual.
         *
         * Se o vídeo anterior estava tocando,
         * o novo deverá tocar.
         *
         * Se estava pausado,
         * o novo deverá permanecer pausado.
         */
        pendingPlaybackState =
            lastKnownPlaying

        println(
            "MpvPlayer: estado anterior = " +
                "playing=$lastKnownPlaying"
        )

        val opened =
            videoSource.open(uri)

        if (!opened) {

            println(
                "MpvPlayer: não foi possível abrir o Uri"
            )

            pendingPlaybackState = null

            return
        }

        val fd =
            videoSource.getFileDescriptor()

        println(
            "MpvPlayer: file descriptor = $fd"
        )

        if (fd != null) {

            native.loadFd(fd)
        }

        native.load(uri.toString())
    }

    override fun play() {

        println(
            "MpvPlayer: play()"
        )

        native.setPause(false)
    }

    override fun pause() {

        println(
            "MpvPlayer: pause()"
        )

        native.setPause(true)
    }

    override fun seekForward(seconds: Double) {

        println(
            "MpvPlayer: seekForward($seconds)"
        )

        native.seekForward(seconds)
    }

    override fun seekBackward(seconds: Double) {

        println(
            "MpvPlayer: seekBackward($seconds)"
        )

        native.seekBackward(seconds)
    }

    override fun setVolume(volume: Double) {

        println(
            "MpvPlayer: setVolume($volume)"
        )

        native.setVolume(volume)
    }

    override fun mute() {

        println(
            "MpvPlayer: mute()"
        )

        native.mute()
    }

    override fun frameForward() {

        println(
            "MpvPlayer: frameForward()"
        )

        native.frameForward()
    }

    override fun frameBackward() {

        println(
            "MpvPlayer: frameBackward()"
        )

        native.frameBackward()
    }

    override fun screenshot() {

        println(
            "MpvPlayer: screenshot()"
        )

        native.screenshot()
    }

    override fun release() {

        println(
            "MpvPlayer: release()"
        )

        videoSource.close()

        native.release()
    }

    private fun setupNativeListeners() {

        native.setPropertyListener { name, value ->

            when (name) {

                "time-pos" -> {

                    _events.tryEmit(
                        PlayerEvent.TimePositionChanged(
                            position = value
                        )
                    )
                }

                "duration" -> {

                    _events.tryEmit(
                        PlayerEvent.DurationChanged(
                            duration = value
                        )
                    )
                }
            }
        }

        native.setBooleanPropertyListener { name, value ->

            println(
                "MpvPlayer: boolean property " +
                    "$name = $value"
            )

            when (name) {

                "pause" -> {

                    /*
                     * Este é o estado real do mpv.
                     *
                     * pause=true  -> playing=false
                     * pause=false -> playing=true
                     */
                    lastKnownPlaying =
                        !value

                    println(
                        "MpvPlayer: estado real = " +
                            "playing=$lastKnownPlaying"
                    )

                    _events.tryEmit(
                        PlayerEvent.PauseChanged(
                            paused = value
                        )
                    )
                }
            }
        }

        native.setStringPropertyListener { name, value ->

            println(
                "MpvPlayer: string property " +
                    "$name = $value"
            )

            when (name) {

                "filename" -> {

                    _events.tryEmit(
                        PlayerEvent.FilenameChanged(
                            filename = value
                        )
                    )
                }
            }
        }

        native.setLoadingListener { loading ->

            println(
                "MpvPlayer: loading = $loading"
            )

            /*
             * FILE_LOADED significa que o novo arquivo
             * terminou de carregar.
             *
             * Agora podemos restaurar com segurança
             * o estado de reprodução anterior.
             */
            if (!loading) {

                val playbackState =
                    pendingPlaybackState

                if (playbackState != null) {

                    println(
                        "MpvPlayer: restaurando estado " +
                            "playing=$playbackState"
                    )

                    native.setPause(
                        !playbackState
                    )

                    pendingPlaybackState =
                        null
                }
            }

            _events.tryEmit(
                PlayerEvent.LoadingChanged(
                    loading = loading
                )
            )
        }
    }
}
