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
                "Não foi possível criar o contexto libmpv"
            )
        }

        nativeInitialize(nativeHandle)

        val version = nativeGetVersion()

        println(
            "MpvNative: native version = $version"
        )
    }

    fun getVersion(): String {
        return nativeGetVersion()
    }

    fun load(uri: String) {
        checkInitialized()

        nativeLoad(uri)
    }

    fun loadLocalPath(path: String) {
        checkInitialized()

        nativeLoadLocalPath(
            nativeHandle,
            path
        )
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

        nativeSetPause(
            nativeHandle,
            false
        )
    }

    fun pause() {
        checkInitialized()

        nativeSetPause(
            nativeHandle,
            true
        )
    }

    fun setPause(paused: Boolean) {
        checkInitialized()

        nativeSetPause(
            nativeHandle,
            paused
        )
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

    private external fun nativeDestroy(
        handle: Long
    )

    private external fun nativeGetVersion(): String

    private external fun nativeLoad(
        uri: String
    )

    private external fun nativeLoadLocalPath(
        handle: Long,
        path: String
    )

    private external fun nativeLoadFd(
        handle: Long,
        fd: Int
    )

    private external fun nativeSetPause(
        handle: Long,
        paused: Boolean
    )

    private external fun nativeSeekForward(
        handle: Long,
        seconds: Double
    )
}
