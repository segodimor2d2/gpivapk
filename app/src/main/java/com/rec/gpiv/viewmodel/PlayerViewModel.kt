package com.rec.gpiv.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUiState(
    val playing: Boolean = false,
    val filename: String? = "video.mp4",
    val position: Double = 37.5,
    val duration: Double = 252.0
)

class PlayerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())

    val uiState: StateFlow<PlayerUiState> =
        _uiState.asStateFlow()

    private var playbackJob: Job? = null

    fun togglePlayPause() {
        if (_uiState.value.playing) {
            pause()
        } else {
            play()
        }
    }


    fun seekForward(seconds: Double) {
        val state = _uiState.value

        val newPosition = minOf(
            state.position + seconds,
            state.duration
        )

        _uiState.value = state.copy(
            position = newPosition
        )
    }

    fun seekBackward(seconds: Double) {
        val state = _uiState.value

        val newPosition = maxOf(
            state.position - seconds,
            0.0
        )

        _uiState.value = state.copy(
            position = newPosition
        )
    }

    private fun play() {
        if (playbackJob?.isActive == true) {
            return
        }

        _uiState.value = _uiState.value.copy(
            playing = true
        )

        playbackJob = viewModelScope.launch {
            while (_uiState.value.playing) {

                delay(100)

                val currentState = _uiState.value
                val newPosition = currentState.position + 0.1

                if (newPosition >= currentState.duration) {
                    _uiState.value = currentState.copy(
                        position = currentState.duration,
                        playing = false
                    )

                    break
                }

                _uiState.value = currentState.copy(
                    position = newPosition
                )
            }
        }
    }

    private fun pause() {
        _uiState.value = _uiState.value.copy(
            playing = false
        )

        playbackJob?.cancel()
        playbackJob = null
    }

    override fun onCleared() {
        playbackJob?.cancel()
        super.onCleared()
    }
}
