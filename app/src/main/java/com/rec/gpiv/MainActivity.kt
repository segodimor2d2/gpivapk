package com.rec.gpiv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rec.gpiv.ui.theme.GpivTheme
import com.rec.gpiv.viewmodel.PlayerViewModel
import androidx.compose.foundation.layout.padding

class MainActivity : ComponentActivity() {

    private val viewModel: PlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            GpivTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->

                    val uiState by viewModel.uiState
                        .collectAsStateWithLifecycle()

                    PlayerScreen(
                        playing = uiState.playing,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
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
