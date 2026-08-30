package com.rec.gpiv.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PlayerScreen(
    playing: Boolean,
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (playing) {
                "Estado: Reproduzindo"
            } else {
                "Estado: Pausado"
            }
        )

        Button(
            onClick = onTogglePlayPause
        ) {
            Text(
                text = if (playing) {
                    "PAUSAR"
                } else {
                    "PLAY"
                }
            )
        }
    }
}
