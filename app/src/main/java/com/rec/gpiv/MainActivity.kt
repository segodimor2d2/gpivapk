package com.rec.gpiv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                        uiState = uiState,
                        onTogglePlayPause = viewModel::togglePlayPause,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
