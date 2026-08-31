package com.rec.gpiv.player

class MpvNative {

    companion object {
        init {
            System.loadLibrary("gpiv_native")
        }
    }

    private var nativeHandle: Long = 0L

    fun initialize() {

        nativeHandle = nativeCreate()

        if (nativeHandle == 0L) {
            throw IllegalStateException(
                "Não foi possível criar o contexto nativo"
            )
        }

        nativeInitialize(nativeHandle)

        val version = nativeGetVersion()

        println(
            "MpvNative: native version = $version"
        )

        val testValue =
            nativeGetTestValue(nativeHandle)

        println(
            "MpvNative: testValue = $testValue"
        )
    }

    fun getVersion(): String {
        return nativeGetVersion()
    }

    fun load(uri: String) {
        checkInitialized()

        nativeLoad(uri)
    }

    fun loadFd(fd: Int) {
        checkInitialized()

        nativeLoadFd(
            nativeHandle,
            fd
        )
    }

    fun play() {
        checkInitialized()

        nativePlay(nativeHandle)
    }

    fun pause() {
        println("MpvNative: pause()")
    }

    fun seekForward(seconds: Double) {
        checkInitialized()

        nativeSeekForward(
            nativeHandle,
            seconds
        )
    }

    fun seekBackward(seconds: Double) {
        println(
            "MpvNative: seekBackward($seconds)"
        )
    }

    fun setVolume(volume: Double) {
        println(
            "MpvNative: setVolume($volume)"
        )
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

        if (nativeHandle != 0L) {

            nativeDestroy(
                nativeHandle
            )

            nativeHandle = 0L
        }

        println("MpvNative: release()")
    }

    private fun checkInitialized() {

        if (nativeHandle == 0L) {
            throw IllegalStateException(
                "MpvNative não foi inicializado"
            )
        }
    }

    private external fun nativeCreate(): Long

    private external fun nativeInitialize(
        handle: Long
    )

    private external fun nativeGetTestValue(
        handle: Long
    ): Int

    private external fun nativeDestroy(
        handle: Long
    )

    private external fun nativeGetVersion(): String

    private external fun nativeLoad(
        uri: String
    )

    private external fun nativeLoadFd(
        handle: Long,
        fd: Int
    )

    private external fun nativePlay(
        handle: Long
    )

    private external fun nativeSeekForward(
        handle: Long,
        seconds: Double
    )
}
