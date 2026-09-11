package com.rec.gpiv

import android.provider.DocumentsContract

import android.net.Uri
import android.content.Context

import android.content.Intent
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

    private val folderPreferences by lazy {
        getSharedPreferences(
            "gpiv_preferences",
            Context.MODE_PRIVATE
        )
    }

    private fun hasPersistedPermission(uri: Uri): Boolean {
        return contentResolver.persistedUriPermissions.any {
            it.uri == uri &&
            it.isReadPermission
        }
    }

    private fun getValidSavedFolderUris(): List<Uri> {

        return getSavedFolderUris()
            .filter { uri ->
                hasPersistedPermission(uri)
            }
    }

    private fun getSavedFolderUris(): MutableList<Uri> {

        val saved =
            folderPreferences.getStringSet(
                "folder_uris",
                emptySet()
            ) ?: emptySet()

        return saved
            .map(Uri::parse)
            .toMutableList()
    }

    private fun loadSavedFolder(uri: Uri): Boolean {

        if (!hasPersistedPermission(uri)) {
            return false
        }

        println(
            "MainActivity: reutilizando pasta autorizada = $uri"
        )

        viewModel.loadFolder(uri)

        return true
    }

    private fun handleIncomingIntent(intent: Intent) {

        if (intent.action != Intent.ACTION_VIEW) {
            return
        }

        val uri = intent.data ?: return

        println(
            "MainActivity: arquivo recebido externamente = $uri"
        )

        println(
            "MainActivity: MIME type = ${intent.type}"
        )

        viewModel.load(uri)
    }


    /*
     * ========================================================
     * ABRIR ARQUIVO
     * ========================================================
     *
     * Continua exatamente como antes.
     *
     * O usuário escolhe um arquivo e o PlayerViewModel
     * continua responsável por carregá-lo.
     */

    private val openVideoLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {

                println(
                    "MainActivity: arquivo selecionado = $uri"
                )

                val documentId =
                    DocumentsContract.getDocumentId(uri)

                println(
                    "MainActivity: VIDEO URI = $uri"
                )

                println(
                    "MainActivity: VIDEO DOCUMENT ID = $documentId"
                )

                val savedFolders =
                    getValidSavedFolderUris()

                val matchingTreeUri =
                    savedFolders.firstOrNull { treeUri ->

                        val treeDocumentId =
                            DocumentsContract.getTreeDocumentId(treeUri)

                        documentId == treeDocumentId ||
                            documentId.startsWith("$treeDocumentId/")
                    }

                println(
                    "MainActivity: pasta correspondente = $matchingTreeUri"
                )


                savedFolders.forEach { treeUri ->

                    val treeDocumentId =
                        DocumentsContract.getTreeDocumentId(treeUri)

                    println(
                        "MainActivity: TREE URI = $treeUri"
                    )

                    println(
                        "MainActivity: TREE DOCUMENT ID = $treeDocumentId"
                    )
                }

                viewModel.load(uri)

                if (matchingTreeUri != null) {

                    println(
                        "MainActivity: carregando FileList da pasta = $matchingTreeUri"
                    )

                    viewModel.loadFolder(matchingTreeUri)
                }
            }
        }


    /*
     * ========================================================
     * ABRIR PASTA
     * ========================================================
     *
     * Neste momento o launcher apenas obtém a permissão
     * da pasta.
     *
     * Ainda não estamos usando a pasta para montar a FileList.
     * Isso será feito no próximo passo.
     */

    private val openFolderLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->

            if (uri != null) {

                println(
                    "MainActivity: pasta selecionada = $uri"
                )

                /*
                 * O Android informa através do Intent quais
                 * permissões podem ser persistidas.
                 *
                 * Neste callback usamos as permissões READ/WRITE
                 * realmente concedidas pelo sistema.
                 */

                val flags =
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION


                try {

                    contentResolver.takePersistableUriPermission(
                        uri,
                        flags
                    )

                    println(
                        "MainActivity: permissão da pasta persistida"
                    )

                    val folderUris =
                        getSavedFolderUris()

                    folderUris.add(uri)

                    folderPreferences
                        .edit()
                        .putStringSet(
                            "folder_uris",
                            folderUris
                                .map(Uri::toString)
                                .toSet()
                        )
                        .apply()

                    viewModel.loadFolder(uri)
                    viewModel.firstFile()

                } catch (e: Exception) {

                    println(
                        "MainActivity: erro ao persistir " +
                            "permissão da pasta: ${e.message}"
                    )
                }
            }
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )


        /*
         * ====================================================
         * EDGE TO EDGE
         * ====================================================
         */

        enableEdgeToEdge()

        handleIncomingIntent(intent)

        /*
         * ====================================================
         * COMPOSE
         * ====================================================
         */

        setContent {

            GpivTheme {

                Scaffold(
                    modifier =
                        Modifier.fillMaxSize()
                ) { innerPadding ->


                    /*
                     * ========================================
                     * UI STATE
                     * ========================================
                     */

                    val uiState by
                        viewModel.uiState
                            .collectAsStateWithLifecycle()


                    /*
                     * ========================================
                     * PLAYER SCREEN
                     * ========================================
                     *
                     * O PlayerScreen continua exatamente
                     * como estava.
                     */

                    PlayerScreen(
                        uiState = uiState,

                        mpvNative =
                            viewModel.mpvNative,

                        onOpenVideo = {
                            openVideoLauncher.launch(
                              arrayOf("*/*")
                            )
                        },

                        onOpenFolder = {

                            val savedFolders =
                                getValidSavedFolderUris()

                            println(
                                "MainActivity: pastas autorizadas = ${savedFolders.size}"
                            )

                            savedFolders.forEach { uri ->
                                println(
                                    "MainActivity: pasta autorizada: $uri"
                                )
                            }

                            openFolderLauncher.launch(null)
                        },

                        onPreviousFile =
                            viewModel::previousFile,


                        onFirstFile =
                            viewModel::firstFile,

                        onLastFile =
                            viewModel::lastFile,

                        onJumpFilesBackward =
                            viewModel::jumpFilesBackward,

                        onJumpFilesForward =
                            viewModel::jumpFilesForward,
                        onNextFile =
                            viewModel::nextFile,

                        onFrameBackward =
                            viewModel::frameBackward,

                        onFrameForward =
                            viewModel::frameForward,

                        onScreenshot = viewModel::screenshot,

                        onSetScreenshotMethod = viewModel::setScreenshotMethod,

                        onRotate = viewModel.mpvNative::rotateClockwise,

                        onTogglePlayPause =
                            viewModel::togglePlayPause,

                        onSeekBackward =
                            viewModel::seekBackward,

                        onSeekForward =
                            viewModel::seekForward,

                        onSeekTo = viewModel::seekTo,

                        onVolumeDown =
                            viewModel::volumeDown,

                        onVolumeUp =
                            viewModel::volumeUp,

                        onMute =
                            viewModel::mute,

                        onBrightnessDown =
                            viewModel::brightnessDown,

                        onBrightnessUp =
                            viewModel::brightnessUp,

                        onContrastDown =
                            viewModel::contrastDown,

                        onContrastUp =
                            viewModel::contrastUp,

                        onGammaDown =
                            viewModel::gammaDown,

                        onGammaUp =
                            viewModel::gammaUp,

                        onSaturationDown =
                            viewModel::saturationDown,

                        onSaturationUp =
                            viewModel::saturationUp,

                        onResetVideoAdjustments =
                            viewModel::resetVideoAdjustments,

                        onSurfaceReady =
                            viewModel::onSurfaceReady,

                        modifier =
                            Modifier.padding(
                                innerPadding
                            )
                    )
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }
}
