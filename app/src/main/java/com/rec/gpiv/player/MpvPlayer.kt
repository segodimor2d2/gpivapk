package com.rec.gpiv.player

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class MpvPlayer(
    contentResolver: ContentResolver
) : VideoPlayer {

    private val native = MpvNative()

    private val videoSource =
        VideoSource(contentResolver)

    private val _events =
        MutableSharedFlow<PlayerEvent>(
            extraBufferCapacity = 64
        )

    override val events =
        _events.asSharedFlow()

    override fun initialize() {

        println("MpvPlayer: initialize()")

        setupNativeListeners()

        native.initialize()

        val version = native.getVersion()

        println(
            "MpvPlayer: native version = $version"
        )

        loadLocalTestPath(
            "/data/user/0/com.rec.gpiv/files/test.mp4"
        )
    }

    fun loadLocalTestPath(path: String) {
        println(
            "MpvPlayer: loadLocalTestPath($path)"
        )

        native.loadLocalPath(path)
    }

    override fun load(uri: Uri) {
        println("MpvPlayer: load($uri)")

        val opened = videoSource.open(uri)

        if (!opened) {
            println(
                "MpvPlayer: não foi possível abrir o Uri"
            )
            return
        }

        val fd = videoSource.getFileDescriptor()

        println(
            "MpvPlayer: file descriptor = $fd"
        )

        if (fd != null) {
            native.loadFd(fd)
        }

        native.load(uri.toString())
    }

    override fun play() {
        println("MpvPlayer: play()")
        native.setPause(false)
    }

    override fun pause() {
        println("MpvPlayer: pause()")
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
        println("MpvPlayer: mute()")
        native.mute()
    }

    override fun frameForward() {
        println("MpvPlayer: frameForward()")
        native.frameForward()
    }

    override fun frameBackward() {
        println("MpvPlayer: frameBackward()")
        native.frameBackward()
    }

    override fun screenshot() {
        println("MpvPlayer: screenshot()")
        native.screenshot()
    }

    override fun release() {
        println("MpvPlayer: release()")

        videoSource.close()

        native.release()
    }

    private fun setupNativeListeners() {

        native.setPropertyListener { name, value ->

            println(
                "MpvPlayer: double property " +
                    "$name = $value"
            )

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
    }
}
