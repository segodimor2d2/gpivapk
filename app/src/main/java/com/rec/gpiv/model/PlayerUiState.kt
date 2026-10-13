package com.rec.gpiv.model

enum class OperationStatus { IDLE, RUNNING, SUCCESS, ERROR }

data class ThumbnailEntry(val uri: android.net.Uri, val name: String, val thumbnailPath: String? = null, val processed: Boolean = false)

data class PlayerUiState(
    val screenshotMpvStatus: OperationStatus = OperationStatus.IDLE,
    val screenshotStatus: OperationStatus = OperationStatus.IDLE,
    val makeFoldersStatus: OperationStatus = OperationStatus.IDLE,
    val moveTagsStatus: OperationStatus = OperationStatus.IDLE,
    val cutStatus: OperationStatus = OperationStatus.IDLE,
    val galleryVisible: Boolean = false,
    val thumbnails: List<ThumbnailEntry> = emptyList(),
    val playing: Boolean = false,
    val filename: String? = null,
    val fileTag: String? = null,
    val tagsEnabled: Boolean = false,
    val tags: List<String> = emptyList(),
    val movingTaggedFiles: Boolean = false,
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
