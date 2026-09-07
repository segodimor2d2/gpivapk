package com.rec.gpiv.ui.player

import android.os.Handler
import android.os.Looper

import android.view.SurfaceHolder
import android.view.SurfaceView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.viewinterop.AndroidView

import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.MpvNative


@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    mpvNative: MpvNative,

    onOpenVideo: () -> Unit,
    onOpenFolder: () -> Unit,

    onPreviousFile: () -> Unit,
    onNextFile: () -> Unit,

    onTogglePlayPause: () -> Unit,
    onSeekBackward: (Double) -> Unit,
    onSeekForward: (Double) -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onMute: () -> Unit,
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
        mutableStateOf(false)
    }


    /*
     * --------------------------------------------------------
     * RENDER LOOP
     * --------------------------------------------------------
     */

    var renderLoopRunning by remember {
        mutableStateOf(false)
    }

    val renderHandler = remember(mpvNative) {
        Handler(
            Looper.getMainLooper()
        )
    }

    val renderRunnable = remember(mpvNative) {

        object : Runnable {

            override fun run() {

                if (!renderLoopRunning) {
                    return
                }

                mpvNative.render()

                renderHandler.postDelayed(
                    this,
                    16L
                )
            }
        }
    }


    DisposableEffect(mpvNative) {

        onDispose {

            renderLoopRunning = false

            renderHandler.removeCallbacks(
                renderRunnable
            )
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
        modifier = modifier.fillMaxSize()
    ) {


        /*
         * ----------------------------------------------------
         * VÍDEO
         * ----------------------------------------------------
         */

        AndroidView(
            factory = { context ->

                SurfaceView(context).apply {

                    holder.addCallback(
                        object : SurfaceHolder.Callback {

                            override fun surfaceCreated(
                                holder: SurfaceHolder
                            ) {

                                println(
                                    "PlayerScreen: surfaceCreated()"
                                )

                                mpvNative.setSurface(
                                    holder.surface
                                )

                                if (!renderLoopRunning) {

                                    renderLoopRunning = true

                                    renderHandler.post(
                                        renderRunnable
                                    )
                                }

                                onSurfaceReady()
                            }


                            override fun surfaceChanged(
                                holder: SurfaceHolder,
                                format: Int,
                                width: Int,
                                height: Int
                            ) {

                                println(
                                    "PlayerScreen: surfaceChanged: " +
                                        "${width}x${height}"
                                )
                            }


                            override fun surfaceDestroyed(
                                holder: SurfaceHolder
                            ) {

                                println(
                                    "PlayerScreen: surfaceDestroyed()"
                                )

                                renderLoopRunning = false

                                renderHandler.removeCallbacks(
                                    renderRunnable
                                )

                                mpvNative.setSurface(
                                    null
                                )
                            }
                        }
                    )
                }
            },

            modifier = Modifier.fillMaxSize()
        )


        /*
         * ----------------------------------------------------
         * ÁREA DE TOQUE
         * ----------------------------------------------------
         *
         * Dois toques:
         *
         * escondido -> mostra
         * mostrado  -> esconde
         *
         * Não fazemos nada no toque simples.
         */

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {

                    detectTapGestures(

                        onDoubleTap = {

                            controlsVisible =
                                !controlsVisible

                            println(
                                "PlayerScreen: " +
                                    "controlsVisible=" +
                                    controlsVisible
                            )
                        }
                    )
                }
        )


        /*
         * ----------------------------------------------------
         * PAINEL DE CONTROLES
         * ----------------------------------------------------
         */

        if (controlsVisible) {

            Column(

                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Color.Black.copy(
                            alpha = 0.0f
                        )
                    )
                    .navigationBarsPadding()
                    .padding(1.dp),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {

                /*
                 * --------------------------------------------
                 * REPRODUÇÃO
                 * --------------------------------------------
                 */

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {


                    CompactButton(
                        onClick = onPreviousFile
                    ) {

                        Text(
                            text = "ANTERIOR",
                            fontSize = 9.sp
                        )
                    }



                    CompactButton(
                        onClick = {
                            onSeekBackward(5.0)
                        }
                    ) {

                        Text(
                            text = "-5s",
                            fontSize = 9.sp
                        )
                    }


                    CompactButton(
                        onClick = onTogglePlayPause
                    ) {

                        Text(
                            text =
                                if (uiState.playing) {
                                    "PAUSAR"
                                } else {
                                    "PLAY"
                                },
                            fontSize = 9.sp
                        )
                    }


                    CompactButton(
                        onClick = {
                            onSeekForward(5.0)
                        }
                    ) {

                        Text(
                            text = "+5s",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = onNextFile
                    ) {

                        Text(
                            text = "PRÓXIMO",
                            fontSize = 9.sp
                        )
                    }

                }


                /*
                 * --------------------------------------------
                 * VOLUME
                 * --------------------------------------------
                 */

                Row(

                    horizontalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    CompactButton(
                        onClick =
                            onVolumeDown
                    ) {

                        Text(
                            text = "VOL -",
                            fontSize = 9.sp
                        )
                    }


                    CompactButton(
                        onClick =
                            onMute
                    ) {

                        Text(
                            text = "MUTE",
                            fontSize = 9.sp
                        )
                    }


                    CompactButton(
                        onClick =
                            onVolumeUp
                    ) {

                        Text(
                            text = "VOL +",
                            fontSize = 9.sp
                        )
                    }
                }


                /*
                 * --------------------------------------------
                 * STATUS
                 * --------------------------------------------
                 */

                Row(

                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(

                        text = uiState.filename ?: "abrir",
                        fontSize = 9.sp,
                        maxLines = 1,
                        modifier = Modifier.clickable { onOpenVideo() }
                    )

                    Text(

                        text =
                            "  %.2f s".format(
                                uiState.position
                            ),

                        fontSize = 9.sp
                    )


                    Text(

                        text =
                            "  |  Vol: ${uiState.volume}%",


                        fontSize = 9.sp
                    )


                    Text(

                        text =
                            if (uiState.loading) {

                                "  |  Carregando..."

                            } else if (uiState.playing) {

                                "  |  ▶"

                            } else {

                                "  |  ⏸"
                            },

                        fontSize = 9.sp
                    )

                    Text(
                        text = "  pasta",
                        fontSize = 9.sp,
                        maxLines = 1,
                        modifier = Modifier.clickable { onOpenFolder() }
                    )

                }

            }
        }
    }
}


/*
 * ============================================================
 * BOTÃO COMPACTO
 * ============================================================
 *
 * - altura
 * - padding horizontal
 * - padding vertical
 */

@Composable
private fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    CompositionLocalProvider(

        LocalMinimumInteractiveComponentSize
            provides 0.dp

    ) {

        Button(

            onClick = onClick,

            modifier = modifier
                .height(24.dp),

            contentPadding =
                PaddingValues(
                    horizontal = 6.dp,
                    vertical = 0.dp
                )

        ) {

            content()
        }
    }
}
