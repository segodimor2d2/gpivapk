package com.rec.gpiv.player

import android.net.Uri

interface VideoPlayer {

    fun initialize()

    fun load(uri: Uri)

    fun play()

    fun pause()

    fun seekForward(seconds: Double)

    fun seekBackward(seconds: Double)

    fun setVolume(volume: Double)

    fun mute()

    fun frameForward()

    fun frameBackward()

    fun screenshot()

    fun release()
}
