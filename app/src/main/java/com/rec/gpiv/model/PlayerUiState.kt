package com.rec.gpiv.model

data class PlayerUiState(
    val playing: Boolean = false,
    val filename: String? = null,
    val fileIndex: Int = 0,
    val fileCount: Int = 0,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val volume: Double = 100.0,
    val muted: Boolean = false,
    val loading: Boolean = false,

    val brightness: Double = 0.0,
    val contrast: Double = 0.0,
    val gamma: Double = 0.0,
    val saturation: Double = 0.0,
)
