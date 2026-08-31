package com.rec.gpiv.player

class MpvNative {

    companion object {
        init {
            System.loadLibrary("gpiv_native")
        }
    }

    fun initialize() {
        nativeInitialize()
    }

    fun getVersion(): String {
        return nativeGetVersion()
    }

    fun load(filename: String) {
        println("MpvNative: load($filename)")
    }

    fun play() {
        println("MpvNative: play()")
    }

    fun pause() {
        println("MpvNative: pause()")
    }

    fun seekForward(seconds: Double) {
        println("MpvNative: seekForward($seconds)")
    }

    fun seekBackward(seconds: Double) {
        println("MpvNative: seekBackward($seconds)")
    }

    fun setVolume(volume: Double) {
        println("MpvNative: setVolume($volume)")
    }

    fun mute() {
        println("MpvNative: mute()")
    }

    fun frameForward() {
        println("MpvNative: frameForward()")
    }

    fun frameBackward() {
        println("MpvNative: frameBackward()")
    }

    fun screenshot() {
        println("MpvNative: screenshot()")
    }

    fun release() {
        println("MpvNative: release()")
    }

    private external fun nativeInitialize()

    private external fun nativeGetVersion(): String
}
