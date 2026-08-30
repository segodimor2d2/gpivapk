package com.rec.gpiv.player

class MpvPlayer : VideoPlayer {

    override fun load(filename: String) {
        println("MpvPlayer: load($filename)")
    }

    override fun play() {
        println("MpvPlayer: play()")
    }

    override fun pause() {
        println("MpvPlayer: pause()")
    }

    override fun seekForward(seconds: Double) {
        println("MpvPlayer: seekForward($seconds)")
    }

    override fun seekBackward(seconds: Double) {
        println("MpvPlayer: seekBackward($seconds)")
    }

    override fun setVolume(volume: Double) {
        println("MpvPlayer: setVolume($volume)")
    }

    override fun mute() {
        println("MpvPlayer: mute()")
    }

    override fun frameForward() {
        println("MpvPlayer: frameForward()")
    }

    override fun frameBackward() {
        println("MpvPlayer: frameBackward()")
    }

    override fun screenshot() {
        println("MpvPlayer: screenshot()")
    }

    override fun release() {
        println("MpvPlayer: release()")
    }
}
