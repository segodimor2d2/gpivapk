package com.rec.gpiv.player

import android.content.ContentResolver
import android.net.Uri

class MpvPlayer(
    contentResolver: ContentResolver
) : VideoPlayer {

    private val native = MpvNative()

    private val videoSource =
        AndroidVideoSource(contentResolver)

    override fun initialize() {
        println("MpvPlayer: initialize()")

        native.initialize()

        val version = native.getVersion()

        println("MpvPlayer: native version = $version")
    }

    override fun load(uri: Uri) {
        println("MpvPlayer: load($uri)")

        val fileDescriptor =
            videoSource.open(uri)

        if (fileDescriptor == null) {
            println("MpvPlayer: não foi possível abrir o Uri")
            return
        }

        println(
            "MpvPlayer: file descriptor = " +
                fileDescriptor.fd
        )

        native.load(uri.toString())

        fileDescriptor.close()
    }

    override fun play() {
        println("MpvPlayer: play()")
        native.play()
    }

    override fun pause() {
        println("MpvPlayer: pause()")
        native.pause()
    }

    override fun seekForward(seconds: Double) {
        println("MpvPlayer: seekForward($seconds)")
        native.seekForward(seconds)
    }

    override fun seekBackward(seconds: Double) {
        println("MpvPlayer: seekBackward($seconds)")
        native.seekBackward(seconds)
    }

    override fun setVolume(volume: Double) {
        println("MpvPlayer: setVolume($volume)")
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
        native.release()
    }
}
