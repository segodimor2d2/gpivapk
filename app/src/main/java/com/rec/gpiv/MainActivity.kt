package com.rec.gpiv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rec.gpiv.ui.player.PlayerScreen
import com.rec.gpiv.ui.theme.GpivTheme
import com.rec.gpiv.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    private val openVideoLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let(viewModel::onVideoSelected)
    }

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let(::persistAndLoadFolder)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            GpivTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    PlayerScreen(
                        uiState = uiState,
                        mpvNative = viewModel.mpvNative,
                        onOpenVideo = {
                            openVideoLauncher.launch(arrayOf("*/*"))
                        },
                        onOpenFolder = {
                            openFolderLauncher.launch(null)
                        },
                        onPreviousFile = viewModel::previousFile,
                        onFirstFile = viewModel::firstFile,
                        onLastFile = viewModel::lastFile,
                        onJumpFilesBackward = viewModel::jumpFilesBackward,
                        onJumpFilesForward = viewModel::jumpFilesForward,
                        onNextFile = viewModel::nextFile,
                        onFrameBackward = viewModel::frameBackward,
                        onFrameForward = viewModel::frameForward,
                        onSetMarkerA = viewModel::setMarkerA,
                        onSetMarkerB = viewModel::setMarkerB,
                        onSaveFileTag = viewModel::testSaveFileTag,
                        onMakeTags = viewModel::makeTagFolders,
                        onTestMoveTags = viewModel::testMoveTags,
                        onDeleteFile = viewModel::deleteFile,
                        onTagsEnabledChange = viewModel::setTagsEnabled,
                        onTestCut = viewModel::testCurrentCut,
                        onDisableABLoop = viewModel::disableABLoop,
                        onScreenshot = viewModel::screenshot,
                        onSetScreenshotMethod = viewModel::setScreenshotMethod,
                        onRotate = viewModel.mpvNative::rotateClockwise,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        onSeekBackward = viewModel::seekBackward,
                        onSeekForward = viewModel::seekForward,
                        onSeekTo = viewModel::seekTo,
                        onVolumeDown = viewModel::volumeDown,
                        onVolumeUp = viewModel::volumeUp,
                        onMute = viewModel::mute,
                        onResetVideoAdjustments = viewModel::resetVideoAdjustments,
                        onSurfaceReady = viewModel::onSurfaceReady,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let(viewModel::handleIncomingUri)
        }
    }

    private fun persistAndLoadFolder(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION

        runCatching {
            contentResolver.takePersistableUriPermission(uri, flags)
            viewModel.onFolderSelected(uri)
        }.onFailure { error ->
            println("MainActivity: erro ao selecionar pasta: ${error.message}")
        }
    }
}
