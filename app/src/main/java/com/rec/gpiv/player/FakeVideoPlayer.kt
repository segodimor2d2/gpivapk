package com.rec.gpiv.player

import android.net.Uri

class FakeVideoPlayer : VideoPlayer {

    override fun initialize() {
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

    override fun setVolume(volume: Double) {
        println("FakeVideoPlayer: setVolume($volume)")
    }

    override fun mute() {
        println("FakeVideoPlayer: mute()")
    }

    override fun frameForward() {
        println("FakeVideoPlayer: frameForward()")
    }

    override fun frameBackward() {
        println("FakeVideoPlayer: frameBackward()")
    }

    override fun screenshot() {
        println("FakeVideoPlayer: screenshot()")
    }

    override fun release() {
        println("FakeVideoPlayer: release()")
    }
}
