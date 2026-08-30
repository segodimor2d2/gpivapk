package com.rec.gpiv.player

interface VideoPlayer {

    fun load(filename: String)

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
