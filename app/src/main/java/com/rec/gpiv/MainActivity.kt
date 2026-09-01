package com.rec.gpiv

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

import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.ui.player.PlayerScreen
import com.rec.gpiv.ui.theme.GpivTheme
import com.rec.gpiv.viewmodel.PlayerViewModel


class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    /*
     * Uma única instância nativa do libmpv.
     *
     * Essa instância será compartilhada entre:
     *
     * MainActivity
     *      ↓
     * PlayerScreen
     *      ↓
     * Surface lifecycle
     */
    private val mpvNative =
        MpvNative()


    private val openVideoLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            if (uri != null) {
                viewModel.load(uri)
            }
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        enableEdgeToEdge()


        /*
         * Inicializa o contexto nativo antes
         * da Surface começar a ser entregue.
         */
        mpvNative.initialize()


        setContent {

            GpivTheme {

                Scaffold(
                    modifier =
                        Modifier.fillMaxSize()
                ) { innerPadding ->

                    val uiState by
                        viewModel.uiState
                            .collectAsStateWithLifecycle()


                    PlayerScreen(
                        uiState = uiState,

                        mpvNative = mpvNative,

                        onOpenVideo = {
                            openVideoLauncher.launch(
                                arrayOf("video/*")
                            )
                        },

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

        /*
         * Neste momento ainda estamos apenas
         * tratando o lifecycle da Surface.
         *
         * O release do MpvNative fica associado
         * ao lifecycle da Activity.
         */
        mpvNative.release()

        super.onDestroy()
    }
}
