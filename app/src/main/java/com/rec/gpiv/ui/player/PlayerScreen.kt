package com.rec.gpiv.ui.player

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

import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset

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
    onSetScreenshotMethod: (String) -> Unit,

    onRotate: () -> Unit,

    onTogglePlayPause: () -> Unit,
    onSeekBackward: (Double) -> Unit,
    onSeekForward: (Double) -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onMute: () -> Unit,

    onBrightnessDown: () -> Unit,
    onBrightnessUp: () -> Unit,
    onContrastDown: () -> Unit,
    onContrastUp: () -> Unit,
    onGammaDown: () -> Unit,
    onGammaUp: () -> Unit,
    onSaturationDown: () -> Unit,
    onSaturationUp: () -> Unit,
    onResetVideoAdjustments: () -> Unit,

    onSurfaceReady: () -> Unit,

    modifier: Modifier = Modifier
) {

    println(
        "PlayerScreen: uiState.playing = ${uiState.playing}"
    )

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

    var controlRow by remember {
        mutableStateOf(0)
    }

    fun nextControlRow() {

        controlRow =
            (controlRow + 1) % 2
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
            onDoubleTap = {
                controlsVisible = !controlsVisible
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

                if (controlRow == 1) {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        /*
                         * --------------------------------------------
                         * VOLUME
                         * --------------------------------------------
                         */

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "volume ${uiState.volume}",
                                onClick = onMute
                            )

                            CompactButton(
                                text = "-",
                                onClick = onVolumeDown
                            )


                            CompactButton(
                                text = "+",
                                onClick = onVolumeUp
                            )
                        }

                        /*
                         * --------------------------------------------
                         * CONTRAST
                         * --------------------------------------------
                         */

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {


                            CompactButton(
                                text = "contrast ${uiState.contrast}",
                                onClick = {}
                            )

                            CompactButton(
                                text = "-",
                                onClick = onContrastDown
                            )

                            CompactButton(
                                text = "+",
                                onClick = onContrastUp
                            )
                        }

                        /*
                         * --------------------------------------------
                         * GAMMA
                         * --------------------------------------------
                         */

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "gamma ${uiState.gamma}",
                                onClick = {}
                            )

                            CompactButton(
                                text = "-",
                                onClick = onGammaDown
                            )

                            CompactButton(
                                text = "+",
                                onClick = onGammaUp
                            )
                        }

                        /*
                         * --------------------------------------------
                         * SATURATION
                         * --------------------------------------------
                         */

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "saturation ${uiState.saturation}",
                                onClick = {}
                            )

                            CompactButton(
                                text = "-",
                                onClick = onSaturationDown
                            )

                            CompactButton(
                                text = "+",
                                onClick = onSaturationUp
                            )
                        }

                        /*
                         * --------------------------------------------
                         * BRIGHTNESS
                         * --------------------------------------------
                         */

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "brightness ${uiState.brightness}",
                                onClick = {}
                            )

                            CompactButton(
                                text = "-",
                                onClick = onBrightnessDown
                            )

                            CompactButton(
                                text = "+",
                                onClick = onBrightnessUp
                            )
                        }

                    }
                }


                /*
                 * --------------------------------------------
                 * REPRODUÇÃO
                 * --------------------------------------------
                 */

                if (controlRow == 0) {

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "+10",
                                onClick = onJumpFilesForward
                            )

                            CompactButton(
                                text = "-10",
                                onClick = onJumpFilesBackward
                            )

                            CompactButton(
                                text = "A",
                                onClick = onFirstFile
                            )

                            CompactButton(
                                text = "Z",
                                onClick = onLastFile
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "<<",
                                onClick = {
                                    onSeekBackward(SEEK_SECONDS)
                                }
                            )

                            CompactButton(
                                text = "<",
                                onClick = onFrameBackward
                            )

                            CompactButton(
                                text = ">",
                                onClick = onFrameForward
                            )

                            CompactButton(
                                text = ">>",
                                onClick = {
                                    onSeekForward(SEEK_SECONDS)
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(
                                    10.dp,
                                    Alignment.CenterHorizontally
                                )
                        ) {

                            CompactButton(
                                text = "N",
                                onClick = onNextFile
                            )

                            CompactButton(
                                text = "P",
                                onClick = onPreviousFile
                            )

                        }
                    }
                }


                /*
                 * --------------------------------------------
                 * STATUS
                 * --------------------------------------------
                 */

                Row(
                    modifier = Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy( 10.dp, Alignment.CenterHorizontally),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {


                    StatusText(
                        text = "R",
                        modifier = Modifier
                            .clickable {
                                onResetVideoAdjustments()
                            }
                    )


                    StatusText(
                        text =
                            "%.2f".format(
                                uiState.position
                            )
                    )

                    StatusText(
                        text =
                            if (uiState.playing) { "▶" }
                            else { "⏸" },
                        modifier =
                            Modifier.clickable {
                                println( "PlayerScreen: CLIQUE PLAY/PAUSE")
                                onTogglePlayPause()
                            }
                    )

                    StatusText(
                        text =
                            "%.2f".format(
                                uiState.duration
                            )
                    )


                    StatusText(
                        text = "PP",
                        modifier =
                            Modifier.clickable {
                                onOpenFolder()
                            }
                    )

                    StatusText(
                        text =
                            if (uiState.duration > 0.0) {
                                "%.1f%%".format(
                                    (uiState.position /
                                        uiState.duration) * 100.0
                                )

                            } else { "0.0%" }
                    )

                    StatusText(
                        text = "↻",
                        modifier =
                            Modifier.clickable {
                                onRotate()
                            }
                    )




                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),

                    horizontalArrangement = Arrangement.Center,

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    StatusText(
                        text = "#",
                        modifier =
                            Modifier.clickable {
                                nextControlRow()
                            }
                    )

                    StatusText(
                        text = uiState.filename ?: "++++++",
                        modifier = Modifier
                            .clickable {
                                onOpenVideo()
                            }
                    )

                    StatusText(
                        text = "S",
                        modifier = Modifier
                            .clickable {
                                onSetScreenshotMethod("FRAMEBUFFER")
                                onScreenshot()
                            }
                    )

                    StatusText(
                        text = "SS",
                        modifier = Modifier
                            .clickable {
                                onSetScreenshotMethod("MPV")
                                onScreenshot()
                            }
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
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    CompositionLocalProvider(
        LocalMinimumInteractiveComponentSize
            provides 0.dp
    ) {

        Button(
            onClick = onClick,

            modifier = modifier
                .height(28.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor =
                    Color.Black.copy(alpha = 0.0f),
                contentColor =
                    Color.White
            ),

            contentPadding =
                PaddingValues(
                    horizontal = 8.dp,
                    vertical = 0.dp
                )
        ) {

            Text(
                text = text,
                fontSize = 16.sp,

                style = LocalTextStyle.current.copy(
                    shadow = Shadow(
                        Color.Black.copy(alpha = 1.0f),
                        offset = Offset(0f, 0f),
                        blurRadius = 8f
                    )
                )

            )
        }
    }
}

@Composable
private fun StatusText(
    text: String,
    modifier: Modifier = Modifier
) {

    Text(
        text = text,
        fontSize = 16.sp,
        color = Color.White,
        maxLines = 1,

        style = LocalTextStyle.current.copy(
            shadow = Shadow(
                Color.Black.copy(alpha = 1.0f),
                offset = Offset(0f, 0f),
                blurRadius = 8f
            )
        ),

        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.0f),
            )
            .padding(
                horizontal = 8.dp,
                vertical = 5.dp
            )
    )
}
