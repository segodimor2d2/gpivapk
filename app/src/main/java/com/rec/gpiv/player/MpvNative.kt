package com.rec.gpiv.player

import android.view.Surface

class MpvNative {

    companion object {
        init {
            System.loadLibrary("gpiv_native")
        }
    }

    private var nativeHandle: Long = 0L

    private var propertyListener:
        ((String, Double) -> Unit)? = null

    private var booleanPropertyListener:
        ((String, Boolean) -> Unit)? = null

    private var stringPropertyListener:
        ((String, String?) -> Unit)? = null

    private var loadingListener:
        ((Boolean) -> Unit)? = null

    fun initialize(
        screenshotDirectory: String
    ) {

        nativeHandle = nativeCreate()

        if (nativeHandle == 0L) {
            throw IllegalStateException(
                "Não foi possível criar o contexto libmpv"
            )
        }

        nativeSetScreenshotDirectory(
            nativeHandle,
            screenshotDirectory
        )

        nativeInitialize(nativeHandle)

        val version = nativeGetVersion()

        println(
            "MpvNative: native version = $version"
        )
    }

    fun setSurface(surface: Surface?) {
        checkInitialized()

        nativeSetSurface(
            nativeHandle,
            surface
        )
    }

    fun setPropertyListener(
        listener: (String, Double) -> Unit
    ) {
        propertyListener = listener
    }

    fun setBooleanPropertyListener(
        listener: (String, Boolean) -> Unit
    ) {
        booleanPropertyListener = listener
    }

    fun setStringPropertyListener(
        listener: (String, String?) -> Unit
    ) {
        stringPropertyListener = listener
    }

    fun setLoadingListener(
        listener: (Boolean) -> Unit
    ) {
        loadingListener = listener
    }

    private fun onNativeLoadingChanged(
        loading: Boolean
    ) {
        println(
            "MpvNative: loading = $loading"
        )

        loadingListener?.invoke(
            loading
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
        checkInitialized()
        println(
            "MpvNative: seekBackward($seconds)"
        )
        nativeSeekBackward(
            nativeHandle,
            seconds
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
        checkInitialized()
        println(
            "MpvNative: frameForward()"
        )
        nativeFrameForward(
            nativeHandle
        )
    }

    fun frameBackward() {

        checkInitialized()

        println(
            "MpvNative: frameBackward()"
        )

        nativeFrameBackward(
            nativeHandle
        )
    }

    fun screenshot() {

        checkInitialized()

        println(
            "MpvNative: screenshot()"
        )

        nativeScreenshot(
            nativeHandle
        )
    }

    fun changeZoom(amount: Double) {

        checkInitialized()

        println(
            "MpvNative: changeZoom($amount)"
        )

        nativeChangeZoom(
            nativeHandle,
            amount
        )
    }

    fun pan(
        x: Double,
        y: Double
    ) {

        checkInitialized()

        println(
            "MpvNative: pan($x, $y)"
        )

        nativePan(
            nativeHandle,
            x,
            y
        )
    }

    fun resetView() {

        checkInitialized()

        println(
            "MpvNative: resetView()"
        )

        nativeResetView(
            nativeHandle
        )
    }

    fun release() {

        if (nativeHandle != 0L) {

            nativeDestroy(
                nativeHandle
            )

            nativeHandle = 0L
        }

        propertyListener = null
        booleanPropertyListener = null
        stringPropertyListener = null
        loadingListener = null

        println("MpvNative: release()")
    }

    private fun checkInitialized() {

        if (nativeHandle == 0L) {
            throw IllegalStateException(
                "MpvNative não foi inicializado"
            )
        }
    }

    /*
     * Chamados pelo código nativo através de JNI.
     */

    private fun onNativeDoubleProperty(
        name: String,
        value: Double
    ) {

        propertyListener?.invoke(
            name,
            value
        )
    }

    private fun onNativeBooleanProperty(
        name: String,
        value: Boolean
    ) {
        println(
            "MpvNative: boolean property " +
                "$name = $value"
        )

        booleanPropertyListener?.invoke(
            name,
            value
        )
    }

    private fun onNativeStringProperty(
        name: String,
        value: String?
    ) {
        println(
            "MpvNative: string property " +
                "$name = $value"
        )

        stringPropertyListener?.invoke(
            name,
            value
        )
    }

    private external fun nativeCreate(): Long

    private external fun nativeSetScreenshotDirectory(
        handle: Long,
        directory: String
    )

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

    private external fun nativeSeekBackward(
        handle: Long,
        seconds: Double
    )

    private external fun nativeFrameForward(
        handle: Long
    )

    private external fun nativeFrameBackward(
        handle: Long
    )

    private external fun nativeScreenshot(
        handle: Long
    )

    private external fun nativeSetSurface(
        handle: Long,
        surface: Surface?
    )

    private external fun nativeRender(handle: Long): Boolean

    fun render(): Boolean {
        return nativeRender(nativeHandle)
    }

    private external fun nativeChangeZoom(
        handle: Long,
        amount: Double
    )

    private external fun nativePan(
        handle: Long,
        x: Double,
        y: Double
    )

    private external fun nativeResetView(
        handle: Long
    )

}
