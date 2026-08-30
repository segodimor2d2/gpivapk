package com.rec.gpiv.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rec.gpiv.viewmodel.PlayerUiState

@Composable
fun PlayerScreen(
    uiState: PlayerUiState,
    onTogglePlayPause: () -> Unit,
    onSeekBackward: (Double) -> Unit,
    onSeekForward: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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
            text = if (uiState.playing) {
                "Estado: Reproduzindo"
            } else {
                "Estado: Pausado"
            }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

    }
}
