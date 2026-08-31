package com.rec.gpiv.player

import android.content.ContentResolver
import android.net.Uri

class MpvPlayer(
    contentResolver: ContentResolver
) : VideoPlayer {

    private val native = MpvNative()

    private val videoSource =
        VideoSource(contentResolver)

    override fun initialize() {
        println("MpvPlayer: initialize()")

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
}
