package com.rec.gpiv.model

data class PlayerUiState(
    val playing: Boolean = false,
    val filename: String? = null,
    val fileIndex: Int = 0,
    val fileCount: Int = 0,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val currentFrame: Long = 0L,
    val previousKeyframeFrame: Long? = null,
    val markerASet: Boolean = false,
    val markerBSet: Boolean = false,

    val volume: Double = 100.0,
    val muted: Boolean = false,
    val loading: Boolean = false,

    val brightness: Double = 0.0,
    val contrast: Double = 0.0,
    val gamma: Double = 0.0,
    val saturation: Double = 0.0,
)

data class FrameMarker(
    val frame: Long,
    val position: Double
)
