package com.rec.gpiv.player

sealed interface PlayerEvent {

    data class TimePositionChanged(
        val position: Double
    ) : PlayerEvent

    data class DurationChanged(
        val duration: Double
    ) : PlayerEvent

    data class PauseChanged(
        val paused: Boolean
    ) : PlayerEvent

    data class FilenameChanged(
        val filename: String?
    ) : PlayerEvent
}
