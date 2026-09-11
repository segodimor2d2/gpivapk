package com.rec.gpiv.player

import android.net.Uri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class FakeVideoPlayer : VideoPlayer {

    override val events: Flow<PlayerEvent> =
        emptyFlow()

    override fun initialize(
        screenshotDirectory: String
    ) {
        println("FakeVideoPlayer: initialize()")
    }

    override fun load(uri: Uri) {
        println("FakeVideoPlayer: load($uri)")
    }

    override fun play() {
        println("FakeVideoPlayer: play()")
    }

    override fun pause() {
        println("FakeVideoPlayer: pause()")
    }

    override fun seekForward(seconds: Double) {
        println("FakeVideoPlayer: seekForward($seconds)")
    }

    override fun seekBackward(seconds: Double) {
        println("FakeVideoPlayer: seekBackward($seconds)")
    }

    override fun seekTo(position: Double) {
        println("FakeVideoPlayer: seekTo($position)")
    }

    override fun setVolume(volume: Double) {
        println("FakeVideoPlayer: setVolume($volume)")
    }

    override fun mute() {
        println("FakeVideoPlayer: mute()")
    }

    override fun frameForward(frames: Int) {
        println("FakeVideoPlayer: frameForward($frames)")
    }

    override fun frameBackward(frames: Int) {
        println("FakeVideoPlayer: frameBackward($frames)")
    }

    override fun screenshot() {
        println("FakeVideoPlayer: screenshot()")
    }

    override fun screenshotMpv() {
        println("FakeVideoPlayer: screenshotMpv()")
    }

    override fun release() {
        println("FakeVideoPlayer: release()")
    }
}
