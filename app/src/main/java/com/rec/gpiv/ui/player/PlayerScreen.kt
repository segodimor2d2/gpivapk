package com.rec.gpiv.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.MpvNative


@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    mpvNative: MpvNative,

    onOpenVideo: () -> Unit,
    onOpenFolder: () -> Unit,
    onOpenThumbnails: () -> Unit,
    onCloseThumbnails: () -> Unit,
    onCompareThumbnails: (Int) -> Unit,
    onFinishAhp: () -> Unit,
    onToggleThumbnailSelection: (android.net.Uri) -> Unit,
    onStartSelectedAhp: () -> Unit,
    onRenameSelectedThumbnails: () -> Unit,

    onPreviousFile: () -> Unit,
    onFirstFile: () -> Unit,
    onLastFile: () -> Unit,
    onJumpFilesBackward: () -> Unit,
    onJumpFilesForward: () -> Unit,

    onNextFile: () -> Unit,

    onFrameBackward: (Int) -> Unit,
    onFrameForward: (Int) -> Unit,
    onSetMarkerA: () -> Unit,
    onSetMarkerB: () -> Unit,
    onTestCut: () -> Unit,
    onSeekBackward: (Double) -> Unit,
    onSeekForward: (Double) -> Unit,
    onSeekTo: (Double) -> Unit,
    onDisableABLoop: () -> Unit,
    onSaveFileTag: (String) -> Unit,
    onMakeTags: () -> Unit,
    onTagsEnabledChange: (Boolean) -> Unit,
    onTestMoveTags: () -> Unit,
    onDeleteFile: (Boolean) -> Unit,

    onScreenshot: () -> Unit,
    onSetScreenshotMethod: (String) -> Unit,

    onRotate: () -> Unit,

    onTogglePlayPause: () -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onMute: () -> Unit,

    onResetVideoAdjustments: () -> Unit,

    onSurfaceReady: () -> Unit,

    modifier: Modifier = Modifier
) {

    /*
     * --------------------------------------------------------
     * CONTROLES
     * --------------------------------------------------------
     *
     * false = painel escondido
     * true  = painel visível
     */

    var controlsVisible by remember {
        mutableStateOf(true)
    }

    val gestureToolState = remember {
        mutableStateOf(GestureTool.NONE)
    }

    fun selectFilter(tool: GestureTool, isEnabled: () -> Boolean, toggle: () -> Unit) {
        if (gestureToolState.value == tool) {
            toggle()
            if (!isEnabled()) gestureToolState.value = GestureTool.NONE
        } else {
            if (!isEnabled()) toggle()
            if (isEnabled()) gestureToolState.value = tool
        }
    }

    /*
     * --------------------------------------------------------
     * TELA INTEIRA
     * --------------------------------------------------------
     *
     * O vídeo ocupa toda a área.
     *
     * Os controles ficam sobrepostos ao vídeo.
     */

    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {

        /*
         * ----------------------------------------------------
         * VÍDEO
         * ----------------------------------------------------
         */

        PlayerSurface(
            mpvNative = mpvNative,
            onSurfaceReady = onSurfaceReady,
            modifier = Modifier.fillMaxSize()
        )


        /*
         * ----------------------------------------------------
         * ZOOM/PAN
         * ----------------------------------------------------
         */
        PlayerGestures(
            mpvNative = mpvNative,
            gestureTool = gestureToolState.value,
            onDoubleTap = {
                // nextControlRow()
            },

            onTwoFingerTap = {
                controlsVisible = !controlsVisible
            },

            onSeekBackward = onSeekBackward,
            onSeekForward = onSeekForward,
            position = uiState.position,
            duration = uiState.duration,
            onSeekTo = onSeekTo,
        )

        if (controlsVisible) {
            PlayerControlsOverlay(
                uiState = uiState,
                edgeDetectionEnabled = mpvNative.edgeDetectionEnabled,
                noiseReductionEnabled = mpvNative.noiseReductionEnabled,
                sharpnessEnabled = mpvNative.sharpnessEnabled,
                blurEnabled = mpvNative.blurEnabled,
                debandEnabled = mpvNative.debandEnabled,
                gestureToolState = gestureToolState,
                onOpenVideo = onOpenVideo,
                onOpenFolder = onOpenFolder,
                onOpenThumbnails = onOpenThumbnails,
                onPreviousFile = onPreviousFile,
                onFirstFile = onFirstFile,
                onLastFile = onLastFile,
                onJumpFilesBackward = onJumpFilesBackward,
                onJumpFilesForward = onJumpFilesForward,
                onNextFile = onNextFile,
                onFrameBackward = onFrameBackward,
                onFrameForward = onFrameForward,
                onSetMarkerA = onSetMarkerA,
                onSetMarkerB = onSetMarkerB,
                onTestCut = onTestCut,
                onSeekBackward = onSeekBackward,
                onSeekForward = onSeekForward,
                onDisableABLoop = onDisableABLoop,
                onSaveFileTag = onSaveFileTag,
                onMakeTags = onMakeTags,
                onTagsEnabledChange = onTagsEnabledChange,
                onTestMoveTags = onTestMoveTags,
                onDeleteFile = onDeleteFile,
                onScreenshot = onScreenshot,
                onSetScreenshotMethod = onSetScreenshotMethod,
                onRotate = onRotate,
                onTogglePlayPause = onTogglePlayPause,
                onVolumeDown = onVolumeDown,
                onVolumeUp = onVolumeUp,
                onMute = onMute,
                onToggleNegative = mpvNative::toggleNegative,
                onToggleVerticalMirror = mpvNative::toggleVerticalMirror,
                onToggleHorizontalMirror = mpvNative::toggleHorizontalMirror,
                onToggleNoiseReduction = {
                    selectFilter(GestureTool.NOISE_REDUCTION,
                        { mpvNative.noiseReductionEnabled }, mpvNative::toggleNoiseReduction)
                },
                onToggleSharpness = {
                    selectFilter(GestureTool.SHARPNESS,
                        { mpvNative.sharpnessEnabled }, mpvNative::toggleSharpness)
                },
                onToggleBlur = {
                    selectFilter(GestureTool.BLUR,
                        { mpvNative.blurEnabled }, mpvNative::toggleBlur)
                },
                onToggleDeband = {
                    selectFilter(GestureTool.DEBAND,
                        { mpvNative.debandEnabled }, mpvNative::toggleDeband)
                },
                onToggleEdgeDetection = {
                    selectFilter(GestureTool.EDGE_DETECTION,
                        { mpvNative.edgeDetectionEnabled }, mpvNative::toggleEdgeDetection)
                },
                onResetVideoAdjustments = {
                    onResetVideoAdjustments()
                    gestureToolState.value = GestureTool.NONE
                }
            )
        }
        if (uiState.galleryVisible) {
            ThumbnailGallery(uiState, onCloseThumbnails, onCompareThumbnails, onFinishAhp,
                onToggleThumbnailSelection, onStartSelectedAhp, onRenameSelectedThumbnails)
        }
    }
}
