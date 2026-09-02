package com.rec.gpiv.ui.player

import android.os.Handler
import android.os.Looper

import android.view.SurfaceHolder
import android.view.SurfaceView

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import androidx.compose.ui.viewinterop.AndroidView

import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.MpvNative


@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    mpvNative: MpvNative,
    onOpenVideo: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekBackward: (Double) -> Unit,
    onSeekForward: (Double) -> Unit,
    onVolumeDown: () -> Unit,
    onVolumeUp: () -> Unit,
    onMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    var surfaceReady by remember {
        mutableStateOf(false)
    }

    val renderHandler = Handler(Looper.getMainLooper())

    val renderRunnable = object : Runnable {
        override fun run() {
            if (surfaceReady) {
                val rendered = mpvNative.render()

                println(
                    "PlayerScreen: render() = $rendered"
                )
            }
        }
    }

    DisposableEffect(mpvNative) {

        renderHandler.post(
            renderRunnable
        )

        onDispose {
            renderHandler.removeCallbacks(
                renderRunnable
            )
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Button(
            onClick = onOpenVideo
        ) {
            Text("ABRIR VÍDEO")
        }

        AndroidView(
            factory = { context ->

                SurfaceView(context).apply {

                    holder.addCallback(
                        object : SurfaceHolder.Callback {

                            override fun surfaceCreated(
                                holder: SurfaceHolder
                            ) {
                                println( "PlayerScreen: surfaceCreated()")
                                println( "PlayerScreen: enviando Surface para MpvNative")

                                mpvNative.setSurface( holder.surface)

                                surfaceReady = true
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
                                println( "PlayerScreen: surfaceDestroyed()")

                                println( "PlayerScreen: removendo Surface do MpvNative")

                                surfaceReady = false

                                mpvNative.setSurface( null)

                            }
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        Text(
            text = uiState.filename ?: "Nenhum arquivo"
        )

        Text(
            text = "Posição: ${uiState.position} s"
        )

        Text(
            text = "Duração: ${uiState.duration} s"
        )

        Text(
            text = "Volume: ${uiState.volume}%"
        )

        Text(
            text = if (uiState.loading) {
                "Carregando..."
            } else if (uiState.playing) {
                "Estado: Reproduzindo"
            } else {
                "Estado: Pausado"
            }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(
                vertical = 8.dp
            )
        ) {

            Button(
                onClick = {
                    onSeekBackward(5.0)
                }
            ) {
                Text("-5s")
            }

            Button(
                onClick = onTogglePlayPause
            ) {
                Text(
                    text = if (uiState.playing) {
                        "PAUSAR"
                    } else {
                        "PLAY"
                    }
                )
            }

            Button(
                onClick = {
                    onSeekForward(5.0)
                }
            ) {
                Text("+5s")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(
                bottom = 8.dp
            )
        ) {

            Button(
                onClick = onVolumeDown
            ) {
                Text("VOL -")
            }

            Button(
                onClick = onMute
            ) {
                Text("MUTE")
            }

            Button(
                onClick = onVolumeUp
            ) {
                Text("VOL +")
            }
        }
    }
}
