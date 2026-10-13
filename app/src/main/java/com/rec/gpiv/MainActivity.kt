package com.rec.gpiv

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let(::persistAndLoadFolder)
    }

    private var browseFiles = false
    private var browserDirectory: File = Environment.getExternalStorageDirectory()

    private fun hasStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private val storageSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (hasStorageAccess()) showStorageBrowser(browserDirectory)
        else Toast.makeText(this, "Ative o acesso a todos os arquivos para navegar pelas pastas.", Toast.LENGTH_LONG).show()
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasStorageAccess()) showStorageBrowser(browserDirectory)
        else Toast.makeText(this, "O acesso ao armazenamento não foi autorizado.", Toast.LENGTH_LONG).show()
    }

    private fun openStorageBrowser(files: Boolean) {
        browseFiles = files
        if (hasStorageAccess()) {
            showStorageBrowser(browserDirectory)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog.Builder(this)
                .setTitle("Acesso a todos os arquivos")
                .setMessage("Autorize o GPIV na próxima tela para abrir arquivos e navegar entre pastas sem autorizar cada pasta.")
                .setPositiveButton("Autorizar") { _, _ ->
                    try {
                        storageSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:$packageName")))
                    } catch (_: ActivityNotFoundException) {
                        storageSettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            storagePermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    private fun showStorageBrowser(directory: File) {
        lifecycleScope.launch {
            val entries = withContext(Dispatchers.IO) {
                directory.listFiles()?.filter { it.isDirectory || browseFiles }
                    ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            }
            if (isFinishing || isDestroyed) return@launch
            if (entries == null) {
                Toast.makeText(this@MainActivity, "Esta pasta não está acessível.", Toast.LENGTH_LONG).show()
                return@launch
            }
            browserDirectory = directory
            val roots = if (directory == Environment.getExternalStorageDirectory()) {
                getExternalFilesDirs(null).mapNotNull { it?.absolutePath?.substringBefore("/Android/") }
                    .map(::File).filter { it != directory && it.isDirectory }
            } else emptyList()
            val choices = roots + entries
            val hasParent = directory.parentFile != null && directory.absolutePath != "/storage"
            val labels = (if (hasParent) listOf("↑ Pasta anterior") else emptyList()) +
                choices.map { if (it.isDirectory) "📁 ${it.name}" else it.name }
            AlertDialog.Builder(this@MainActivity)
                .setTitle(directory.absolutePath)
                .setItems(labels.toTypedArray()) { _, index ->
                    if (hasParent && index == 0) showStorageBrowser(requireNotNull(directory.parentFile))
                    else {
                        val item = choices[index - if (hasParent) 1 else 0]
                        if (item.isDirectory) showStorageBrowser(item)
                        else viewModel.onLocalFolderSelected(directory, item)
                    }
                }
                .setPositiveButton("Abrir esta pasta") { _, _ -> viewModel.onLocalFolderSelected(directory) }
                .setNeutralButton("Pasta para tags e exportação") { _, _ -> openFolderLauncher.launch(null) }
                .setNegativeButton("Cancelar", null)
                .show()
        }
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
                            openStorageBrowser(files = true)
                        },
                        onOpenFolder = {
                            openStorageBrowser(files = false)
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
