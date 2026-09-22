package com.rec.gpiv.viewmodel

import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.BRIGHTNESS_STEP
import com.rec.gpiv.player.CONTRAST_STEP
import com.rec.gpiv.player.GAMMA_STEP
import com.rec.gpiv.player.MAX_VOLUME
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.SATURATION_STEP
import com.rec.gpiv.player.VideoPlayer

/** Encapsula volume, mute e ajustes de imagem do player. */
class PlayerControls(
    private val player: VideoPlayer,
    private val mpvNative: MpvNative,
    private val readState: () -> PlayerUiState,
    private val writeState: (PlayerUiState) -> Unit
) {

    private var volumeBeforeMute = 100.0

    fun volumeUp(amount: Double = 5.0) {
        val state = readState()
        val volume = minOf(state.volume + amount, MAX_VOLUME)
        player.setVolume(volume)
        writeState(state.copy(volume = volume))
    }

    fun volumeDown(amount: Double = 5.0) {
        val state = readState()
        val volume = maxOf(state.volume - amount, 0.0)
        player.setVolume(volume)
        writeState(state.copy(volume = volume))
    }

    fun mute() {
        val state = readState()
        if (!state.muted) {
            volumeBeforeMute = state.volume
            player.mute()
            writeState(state.copy(volume = 0.0, muted = true))
        } else {
            player.mute()
            player.setVolume(volumeBeforeMute)
            writeState(state.copy(volume = volumeBeforeMute, muted = false))
        }
    }

    fun brightnessUp() = changeBrightness(BRIGHTNESS_STEP)
    fun brightnessDown() = changeBrightness(-BRIGHTNESS_STEP)
    fun contrastUp() = changeContrast(CONTRAST_STEP)
    fun contrastDown() = changeContrast(-CONTRAST_STEP)
    fun gammaUp() = changeGamma(GAMMA_STEP)
    fun gammaDown() = changeGamma(-GAMMA_STEP)
    fun saturationUp() = changeSaturation(SATURATION_STEP)
    fun saturationDown() = changeSaturation(-SATURATION_STEP)

    fun resetVideoAdjustments() {
        mpvNative.resetVideoAdjustments()
        mpvNative.resetView()
        writeState(
            readState().copy(
                brightness = 0.0,
                contrast = 0.0,
                gamma = 0.0,
                saturation = 0.0
            )
        )
    }

    private fun changeBrightness(delta: Double) {
        val state = readState()
        val value = (state.brightness + delta).coerceIn(-100.0, 100.0)
        mpvNative.changeBrightness(delta)
        writeState(state.copy(brightness = value))
    }

    private fun changeContrast(delta: Double) {
        val state = readState()
        val value = (state.contrast + delta).coerceIn(-100.0, 100.0)
        mpvNative.changeContrast(delta)
        writeState(state.copy(contrast = value))
    }

    private fun changeGamma(delta: Double) {
        val state = readState()
        val value = (state.gamma + delta).coerceIn(-100.0, 100.0)
        mpvNative.changeGamma(delta)
        writeState(state.copy(gamma = value))
    }

    private fun changeSaturation(delta: Double) {
        val state = readState()
        val value = (state.saturation + delta).coerceIn(-100.0, 100.0)
        mpvNative.changeSaturation(delta)
        writeState(state.copy(saturation = value))
    }
}
