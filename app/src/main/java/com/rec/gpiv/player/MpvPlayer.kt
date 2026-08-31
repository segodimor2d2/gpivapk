package com.rec.gpiv.player

class MpvPlayer : VideoPlayer {

    private val native = MpvNative()

    override fun initialize() {
        println("MpvPlayer: initialize()")

        native.initialize()

        val version = native.getVersion()

        println("MpvPlayer: native version = $version")
    }

    override fun load(filename: String) {
        println("MpvPlayer: load($filename)")
        native.load(filename)
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
