package com.rec.gpiv

import android.Manifest
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.provider.DocumentsContract
import com.rec.gpiv.ui.common.BlackNotice
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
import androidx.activity.SystemBarStyle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rec.gpiv.ui.player.PlayerScreen
import com.rec.gpiv.ui.browser.showTerminalFileBrowser
import com.rec.gpiv.ui.browser.TerminalFileBrowserDialog
import com.rec.gpiv.ui.theme.GpivTheme
import com.rec.gpiv.viewmodel.PlayerViewModel

class MainActivity : ComponentActivity() {

    private var notice: BlackNotice? = null
    private var browserDialog: TerminalFileBrowserDialog? = null

    private fun showNotice(message: String) {
        if (isFinishing || isDestroyed) return
        val current = notice ?: BlackNotice(findViewById(android.R.id.content)).also { notice = it }
        current.show(message)
    }

    override fun onDestroy() {
        browserDialog?.dismiss()
        browserDialog = null
        notice?.dismiss()
        super.onDestroy()
    }

    private val viewModel: PlayerViewModel by viewModels()

    private val openFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let(::persistAndLoadFolder)
    }

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
        else showNotice("Ative o acesso a todos os arquivos para navegar pelas pastas.")
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasStorageAccess()) showStorageBrowser(browserDirectory)
        else showNotice("O acesso ao armazenamento não foi autorizado.")
    }

    private fun openStorageBrowser() {
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
                directory.listFiles()?.toList()
                    ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            }
            if (isFinishing || isDestroyed) return@launch
            if (entries == null) {
                showNotice("Esta pasta não está acessível.")
                return@launch
            }
            browserDirectory = directory
            val roots = if (directory == Environment.getExternalStorageDirectory()) {
                getExternalFilesDirs(null).mapNotNull { it?.absolutePath?.substringBefore("/Android/") }
                    .map(::File).filter { it != directory && it.isDirectory }
            } else emptyList()
            browserDialog = showTerminalFileBrowser(
                context = this@MainActivity,
                directory = directory,
                entries = roots + entries,
                onOpen = { item ->
                    if (item.isDirectory) showStorageBrowser(item)
                    else viewModel.onLocalFolderSelected(directory, item)
                },
                onSelectFolder = { viewModel.onLocalFolderSelected(directory) },
                onSelectDocumentTree = { openFolderLauncher.launch(null) },
                existingDialog = browserDialog?.takeIf { it.isShowing }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        handleIncomingIntent(intent)

        setContent {
            GpivTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = androidx.compose.ui.graphics.Color.Black
                ) { innerPadding ->
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    PlayerScreen(
                        uiState = uiState,
                        mpvNative = viewModel.mpvNative,
                        onOpenVideo = {
                            openStorageBrowser()
                        },
                        onOpenFolder = {
                            openStorageBrowser()
                        },
                        onOpenThumbnails = viewModel::openThumbnailGallery,
                        onCloseThumbnails = viewModel::closeThumbnailGallery,
                        onCompareThumbnails = viewModel::compareThumbnails,
                        onFinishAhp = viewModel::finishAhp,
                        onToggleThumbnailSelection = viewModel::toggleThumbnailSelection,
                        onStartSelectedAhp = viewModel::startSelectedAhp,
                        onRenameSelectedThumbnails = viewModel::renameSelectedThumbnails,
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
                        onTestCopyCut = viewModel::testCurrentCopyCut,
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
        if (intent == null) return
        val uri = intent.data ?: return
        when (intent.action) {
            ACTION_OPEN_FOLDER -> openIncomingFolder(uri, intent.flags)
            Intent.ACTION_VIEW -> {
                val isFolder = uri.scheme == "file" && uri.path?.let { File(it).isDirectory } == true ||
                    uri.scheme == "content" && (DocumentsContract.isTreeUri(uri) ||
                        intent.type == DocumentsContract.Document.MIME_TYPE_DIR ||
                        intent.type == "resource/folder" || intent.type == "inode/directory")
                if (isFolder) openIncomingFolder(uri, intent.flags)
                else viewModel.handleIncomingUri(uri)
            }
        }
    }

    private fun openIncomingFolder(uri: Uri, intentFlags: Int) {
        val localFolder = when {
            uri.scheme == "file" -> uri.path?.let(::File)
            uri.scheme == "content" && uri.authority == "com.android.externalstorage.documents" -> {
                val id = runCatching {
                    if (uri.pathSegments.contains("document")) DocumentsContract.getDocumentId(uri)
                    else DocumentsContract.getTreeDocumentId(uri)
                }.getOrNull()
                id?.let {
                    val volume = it.substringBefore(':')
                    val root = if (volume == "primary") Environment.getExternalStorageDirectory()
                        else File("/storage/$volume")
                    File(root, it.substringAfter(':', ""))
                }
            }
            else -> null
        }
        if (localFolder?.isDirectory == true && localFolder.canRead()) {
            browserDirectory = localFolder
            viewModel.onLocalFolderSelected(localFolder)
            return
        }
        if (uri.scheme != "content" || !DocumentsContract.isTreeUri(uri)) {
            showNotice("Pasta inacessível. Envie uma URI file:// acessível ou uma URI de árvore SAF com permissão de leitura.")
            return
        }
        val flags = intentFlags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        if (intentFlags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION != 0 && flags != 0) {
            // Uma concessão temporária também permite abrir a pasta nesta sessão.
            runCatching { contentResolver.takePersistableUriPermission(uri, flags) }
        }
        runCatching {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                uri, DocumentsContract.getTreeDocumentId(uri)
            )
            requireNotNull(contentResolver.query(childrenUri,
                arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID), null, null, null)) {
                "Pasta inacessível"
            }.use { /* Verifica o acesso antes de substituir a pasta atual. */ }
            viewModel.onFolderSelected(uri)
        }
            .onFailure { showNotice("Não foi possível abrir a pasta: ${it.message}") }
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

    companion object {
        const val ACTION_OPEN_FOLDER = "com.rec.gpiv.action.OPEN_FOLDER"
    }
}
