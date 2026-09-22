package com.rec.gpiv.viewmodel

import com.rec.gpiv.player.VideoPlayer

data class FrameMarker(
    val frame: Long,
    val position: Double
)

/** Mantém os marcadores A/B e controla o loop entre eles. */
class PlayerMarkerController(
    private val player: VideoPlayer
) {
    private var markerA: FrameMarker? = null
    private var markerB: FrameMarker? = null
    private var loopEnabled = false
    private var seekingToA = false

    fun setA(marker: FrameMarker) { markerA = marker }
    fun setB(marker: FrameMarker) { markerB = marker }
    fun markerA(): FrameMarker? = markerA
    fun markerB(): FrameMarker? = markerB

    fun markersForCut(): Pair<FrameMarker, FrameMarker>? {
        val a = markerA ?: return null
        val b = markerB ?: return null
        return if (a.frame <= b.frame) a to b else b to a
    }

    fun startPlayback() {
        if (markerA != null && markerB != null && markerA!!.position < markerB!!.position) {
            loopEnabled = true
        }
        player.play()
    }

    /** Retorna true quando atingiu B e emitiu o seek para A. */
    fun onPositionChanged(position: Double): Boolean {
        if (seekingToA && markerB != null && position < markerB!!.position) {
            seekingToA = false
        }
        if (loopEnabled && !seekingToA && markerA != null && markerB != null &&
            position >= markerB!!.position
        ) {
            seekingToA = true
            player.seekTo(markerA!!.position)
            return true
        }
        return false
    }

    fun enableLoop() {
        if (markerA != null && markerB != null) loopEnabled = true
    }

    fun clear() {
        markerA = null
        markerB = null
        loopEnabled = false
        seekingToA = false
    }
}
