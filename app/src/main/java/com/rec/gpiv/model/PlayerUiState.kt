package com.rec.gpiv.model

data class PlayerUiState(
    val playing: Boolean = false,
    val filename: String? = "video.mp4",
    val position: Double = 37.5,
    val duration: Double = 252.0,
    val volume: Double = 100.0,
    val muted: Boolean = false,
    val loading: Boolean = false
)
