package com.rec.gpiv

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

                viewModel.load(uri)
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

                    viewModel.loadFolder(uri)

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

                        onRotate = viewModel.mpvNative::rotateClockwise,

                        onTogglePlayPause =
                            viewModel::togglePlayPause,

                        onSeekBackward =
                            viewModel::seekBackward,

                        onSeekForward =
                            viewModel::seekForward,

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
