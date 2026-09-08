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
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

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
            mpvNative = mpvNative
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


                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    CompactButton(
                        onClick =
                            onResetVideoAdjustments
                    ) {
                        Text(
                            text = "RESET",
                            fontSize = 9.sp
                        )
                    }
                }

                /*
                 * --------------------------------------------
                 * CONTRAST
                 * --------------------------------------------
                 */

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    CompactButton(
                        onClick =
                            onContrastDown
                    ) {
                        Text(
                            text = "-",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = {}
                    ) {
                        Text(
                            text = "C ${uiState.contrast}",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick =
                            onContrastUp
                    ) {
                        Text(
                            text = "+",
                            fontSize = 9.sp
                        )
                    }
                }


                /*
                 * --------------------------------------------
                 * GAMMA
                 * --------------------------------------------
                 */

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    CompactButton(
                        onClick =
                            onGammaDown
                    ) {
                        Text(
                            text = "-",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = {}
                    ) {
                        Text(
                            text = "G ${uiState.gamma}",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick =
                            onGammaUp
                    ) {
                        Text(
                            text = "+",
                            fontSize = 9.sp
                        )
                    }
                }


                /*
                 * --------------------------------------------
                 * SATURATION
                 * --------------------------------------------
                 */

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    CompactButton(
                        onClick =
                            onSaturationDown
                    ) {
                        Text(
                            text = "-",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = {}
                    ) {
                        Text(
                            text = "S ${uiState.saturation}",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick =
                            onSaturationUp
                    ) {
                        Text(
                            text = "+",
                            fontSize = 9.sp
                        )
                    }
                }
                /*
                 * --------------------------------------------
                 * BRIGHTNESS
                 * --------------------------------------------
                 */

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    CompactButton(
                        onClick =
                            onBrightnessDown
                    ) {
                        Text(
                            text = "-",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = {}
                    ) {
                        Text(
                            text = "B ${uiState.brightness}",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick =
                            onBrightnessUp
                    ) {
                        Text(
                            text = "+",
                            fontSize = 9.sp
                        )
                    }
                }


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
                            text = "P",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = {
                            onSeekBackward(SEEK_SECONDS)
                        }
                    ) {
                        Text(
                            text = "<<",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = onFrameBackward
                    ) {
                        Text(
                            text = "<",
                            fontSize = 9.sp
                        )
                    }


                    CompactButton(
                        onClick = onTogglePlayPause
                    ) {

                        Text(
                            text =
                                if (uiState.playing) {
                                    "▶"
                                } else {
                                    "⏸"
                                },


                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = onFrameForward
                    ) {
                        Text(
                            text = ">",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = {
                            onSeekForward(SEEK_SECONDS)
                        }
                    ) {
                        Text(
                            text = ">>",
                            fontSize = 9.sp
                        )
                    }

                    CompactButton(
                        onClick = onNextFile
                    ) {
                        Text(
                            text = "N",
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
                            text = "A",
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
                            text = "-",
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
                            text = "+",
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
                            text = "Z",
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

                    horizontalArrangement =
                        Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    StatusText(
                        text = "S",
                        modifier =
                            Modifier.clickable {
                                onScreenshot()
                            }
                    )

                    StatusText(
                        text =
                            "%.2f/%.2f".format(
                                uiState.position,
                                uiState.duration
                            )
                    )

                    StatusText(
                        text = "R",
                        modifier =
                            Modifier.clickable {
                                onRotate()
                            }
                    )

                    StatusText(
                        text =
                            if (uiState.duration > 0.0) {
                                "%.1f %%".format(
                                    (uiState.position /
                                        uiState.duration) * 100.0
                                )

                            } else { "0.0 %" }
                    )

                    StatusText(
                        text = "PP",
                        modifier =
                            Modifier.clickable {
                                onOpenFolder()
                            }
                    )

                }

                Row(

                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusText(
                        text = uiState.filename ?: "++++++",
                        modifier = Modifier
                            .clickable {
                                onOpenVideo()
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
                containerColor = Color.Black.copy(alpha = 0.3f),
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

@Composable
private fun StatusText(
    text: String,
    modifier: Modifier = Modifier
) {

    Text(
        text = text,
        fontSize = 14.sp,
        color = Color.White,
        maxLines = 1,
        modifier = modifier
            .background(
                Color.Black.copy(alpha = 0.3f)
            )
            .padding(
                horizontal = 6.dp,
                vertical = 3.dp
            )
    )
}
