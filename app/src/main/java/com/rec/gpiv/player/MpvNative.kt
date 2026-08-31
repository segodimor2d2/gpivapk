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
        nativeLoad(filename)
    }

    fun play() {
        nativePlay()
    }

    fun pause() {
        println("MpvNative: pause()")
    }

    fun seekForward(seconds: Double) {
        nativeSeekForward(seconds)
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

    private external fun nativePlay()

    private external fun nativeSeekForward(seconds: Double)

    private external fun nativeLoad(filename: String)

}
