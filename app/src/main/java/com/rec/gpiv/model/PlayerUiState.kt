package com.rec.gpiv.model

data class PlayerUiState(
    val playing: Boolean = false,
    val filename: String? = null,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val volume: Double = 100.0,
    val muted: Boolean = false,
    val loading: Boolean = false
)
