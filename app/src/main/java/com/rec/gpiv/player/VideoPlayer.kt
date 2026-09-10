package com.rec.gpiv.player

import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface VideoPlayer {

    val events: Flow<PlayerEvent>

    fun initialize(
        screenshotDirectory: String
    )

    fun load(uri: Uri)

    fun play()

    fun pause()

    fun seekForward(seconds: Double)

    fun seekBackward(seconds: Double)

    fun setVolume(volume: Double)

    fun mute()

    fun frameForward(frames: Int)
    fun frameBackward(frames: Int)

    fun screenshot()

    fun screenshotMpv()

    fun release()
}
