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

    private var hasLoadedVideo = false


    /*
     * Estado que deverá ser aplicado ao próximo
     * vídeo depois que ele terminar de carregar.
     *
     * null = nenhum estado pendente.
     */
    private var pendingPlaybackState: Boolean? = null

    override fun initialize(
        screenshotDirectory: String
    ) {

        println("MpvPlayer: initialize()")

        setupNativeListeners()

        native.initialize(
            screenshotDirectory
        )

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
            if (hasLoadedVideo) {
                lastKnownPlaying
            } else {
                false
            }

        hasLoadedVideo = true

        println(
            "MpvPlayer: estado anterior = " +
                "playing=$lastKnownPlaying " +
                "loaded=$hasLoadedVideo " +
                "pending=$pendingPlaybackState"
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

    override fun screenshotMpv() {

        println(
            "MpvPlayer: screenshotMpv()"
        )

        native.screenshotMpv()
    }

    override fun release() {

        println(
            "MpvPlayer: release()"
        )

        videoSource.close()

        native.release()
    }

    fun changeBrightness(amount: Double) {
        println(
            "MpvPlayer: changeBrightness($amount)"
        )
        native.changeBrightness(amount)
    }

    fun changeContrast(amount: Double) {
        println(
            "MpvPlayer: changeContrast($amount)"
        )
        native.changeContrast(amount)
    }

    fun changeGamma(amount: Double) {
        println(
            "MpvPlayer: changeGamma($amount)"
        )
        native.changeGamma(amount)
    }

    fun changeSaturation(amount: Double) {
        println(
            "MpvPlayer: changeSaturation($amount)"
        )
        native.changeSaturation(amount)
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

                println(
                    "pending=$pendingPlaybackState " +
                    "playbackState=$playbackState"
                )

                if (playbackState != null) {

                    println(
                        "MpvPlayer: restaurando estado " +
                            "playing=$playbackState"
                    )

                    native.setPause(
                        !playbackState
                    )

                    lastKnownPlaying =
                        playbackState

                    _events.tryEmit(
                        PlayerEvent.PauseChanged(
                            paused = !playbackState
                        )
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
