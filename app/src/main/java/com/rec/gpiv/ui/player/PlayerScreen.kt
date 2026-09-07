package com.rec.gpiv.ui.player

import android.os.Handler
import android.os.Looper

import android.view.SurfaceHolder
import android.view.SurfaceView

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

import androidx.compose.ui.viewinterop.AndroidView

import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.SEEK_SECONDS


@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    mpvNative: MpvNative,

    onOpenVideo: () -> Unit,
    onOpenFolder: () -> Unit,

    onPreviousFile: () -> Unit,
    onFirstFile: () -> Unit,
    onLastFile: () -> Unit,
    onJumpFilesBackward: () -> Unit,
    onJumpFilesForward: () -> Unit,

    onNextFile: () -> Unit,

    onFrameBackward: () -> Unit,
    onFrameForward: () -> Unit,
    onScreenshot: () -> Unit,

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
         * ZOOM/PAN
         * ----------------------------------------------------
         */

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp)
                .pointerInput(Unit) {

                    awaitEachGesture {

                        awaitFirstDown(
                            requireUnconsumed = false
                        )

                        var previousCentroid = Offset.Zero
                        var previousDistance = 0f
                        var trackingTwoPointers = false

                        while (true) {

                            val event = awaitPointerEvent()

                            val pressedPointers =
                                event.changes.filter {
                                    it.pressed
                                }

                            /*
                             * ------------------------------------------------
                             * DOIS DEDOS
                             * ------------------------------------------------
                             */

                            if (pressedPointers.size >= 2) {

                                val first =
                                    pressedPointers[0].position

                                val second =
                                    pressedPointers[1].position

                                val centroid =
                                    Offset(
                                        x = (first.x + second.x) / 2f,
                                        y = (first.y + second.y) / 2f
                                    )

                                val distance =
                                    hypot(
                                        second.x - first.x,
                                        second.y - first.y
                                    )

                                /*
                                 * Primeiro evento com dois dedos:
                                 *
                                 * apenas estabelece a posição inicial.
                                 */

                                if (!trackingTwoPointers) {

                                    previousCentroid = centroid
                                    previousDistance = distance

                                    trackingTwoPointers = true

                                } else {

                                    /*
                                     * ----------------------------------------
                                     * PAN
                                     * ----------------------------------------
                                     */

                                    val pan =
                                        centroid - previousCentroid

                                    if (
                                        pan.x != 0f ||
                                        pan.y != 0f
                                    ) {

                                        val panX =
                                            pan.x /
                                                size.width.toFloat()

                                        val panY =
                                            pan.y /
                                                size.height.toFloat()

                                        mpvNative.pan(
                                            panX.toDouble(),
                                            panY.toDouble()
                                        )
                                    }


                                    /*
                                     * ----------------------------------------
                                     * ZOOM
                                     * ----------------------------------------
                                     */

                                    if (previousDistance > 0f) {

                                        val zoomFactor =
                                            distance /
                                                previousDistance

                                        if (
                                            zoomFactor > 0f &&
                                            zoomFactor != 1f
                                        ) {

                                            val zoomAmount =
                                                kotlin.math.ln(
                                                    zoomFactor.toDouble()
                                                ) /
                                                kotlin.math.ln(2.0)

                                            mpvNative.changeZoom(
                                                zoomAmount
                                            )
                                        }
                                    }


                                    previousCentroid =
                                        centroid

                                    previousDistance =
                                        distance
                                }

                            } else {

                                /*
                                 * Menos de dois dedos.
                                 *
                                 * Reinicia a referência para que,
                                 * quando o segundo dedo entrar,
                                 * comecemos uma nova medição.
                                 */

                                trackingTwoPointers = false
                            }


                            /*
                             * Todos os dedos foram retirados.
                             */

                            if (
                                pressedPointers.isEmpty()
                            ) {
                                break
                            }
                        }
                    }
                }
        )

        /*
         * ----------------------------------------------------
         * ÁREA DE SWIPE
         * ----------------------------------------------------
         *
         * Somente os 80dp inferiores da tela respondem.
         *
         * Um swipe curto para cima:
         *
         * escondido -> mostra
         * mostrado  -> esconde
         *
         * A distância é acumulada durante o gesto.
         *
         * Um único swipe pode alternar o menu apenas uma vez.
         */

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .pointerInput(Unit) {

                    var accumulatedDrag = 0f
                    var gestureHandled = false

                    detectVerticalDragGestures(

                        onDragStart = {
                            accumulatedDrag = 0f
                            gestureHandled = false
                        },

                        onVerticalDrag = { _, dragAmount ->

                            if (gestureHandled) {
                                return@detectVerticalDragGestures
                            }

                            /*
                             * Para cima = valor negativo.
                             *
                             * Acumulamos somente movimentos
                             * para cima.
                             */

                            if (dragAmount < 0f) {

                                accumulatedDrag += -dragAmount

                                /*
                                 * Aproximadamente 20dp de swipe
                                 * já são suficientes.
                                 */
                                if (accumulatedDrag >= 20.dp.toPx()) {

                                    controlsVisible =
                                        !controlsVisible

                                    gestureHandled = true

                                    println(
                                        "PlayerScreen: " +
                                            "swipe up -> " +
                                            "controlsVisible=" +
                                            controlsVisible
                                    )
                                }
                            }
                        },

                        onDragEnd = {
                            accumulatedDrag = 0f
                            gestureHandled = false
                        },

                        onDragCancel = {
                            accumulatedDrag = 0f
                            gestureHandled = false
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
                    Arrangement.spacedBy(6.dp)
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
                            onSeekBackward(SEEK_SECONDS)
                        }
                    ) {
                        Text(
                            text = "-5s",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = onFrameBackward
                    ) {
                        Text(
                            text = "FRAME -",
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
                        onClick = onFrameForward
                    ) {
                        Text(
                            text = "FRAME +",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = {
                            onSeekForward(SEEK_SECONDS)
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
                        Arrangement.spacedBy(6.dp)
                ) {

                    CompactButton(
                        onClick = onFirstFile
                    ) {
                        Text(
                            text = "PRIMEIRO",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = onJumpFilesBackward
                    ) {
                        Text(
                            text = "-10",
                            fontSize = 9.sp
                        )
                    }

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
                        onClick = onMute
                    ) {

                        Text(
                            text = "${uiState.volume}%",
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

                    CompactButton(
                        onClick = onJumpFilesForward
                    ) {
                        Text(
                            text = "+10",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = onLastFile
                    ) {
                        Text(
                            text = "ÚLTIMO",
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
                      horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                      verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                        text = "%.2f s".format(uiState.position),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = "/ %.2f s".format(uiState.duration),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text =
                            if (uiState.duration > 0.0) {
                                "/ %.1f %%".format(
                                    (uiState.position / uiState.duration) * 100.0
                                )
                            } else {
                                "/ 0.0 %"
                            },
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text =
                            if (uiState.loading) {
                                "|  Carregando..."
                            } else if (uiState.playing) {
                                "|  ▶"
                            } else {
                                "|  ⏸"
                            },
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Text(
                        text = "pasta",
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { onOpenFolder() }
                    )
                    Text(
                        text = "save",
                        fontSize = 14.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .clickable { onScreenshot() }
                    )
                }

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

            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black.copy(alpha = 0.8f),
                contentColor = Color.White
            ),

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
