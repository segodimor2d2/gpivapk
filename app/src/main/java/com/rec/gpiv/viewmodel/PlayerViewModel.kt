package com.rec.gpiv.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.MpvPlayer
import com.rec.gpiv.player.VideoPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val player: VideoPlayer = MpvPlayer()

    private val _uiState = MutableStateFlow(PlayerUiState())

    val uiState: StateFlow<PlayerUiState> =
        _uiState.asStateFlow()

    private var playbackJob: Job? = null

    init {
        player.initialize()
    }

    fun load(uri: Uri) {

        val filename = getFileName(uri)

        _uiState.value = _uiState.value.copy(
            filename = filename,
            loading = true,
            playing = false,
            position = 0.0,
            duration = 0.0
        )

        try {
            player.load(uri)

            _uiState.value = _uiState.value.copy(
                loading = false
            )

        } catch (e: Exception) {

            _uiState.value = _uiState.value.copy(
                loading = false,
                playing = false
            )

            println(
                "PlayerViewModel: erro ao carregar vídeo: ${e.message}"
            )
        }
    }

    private fun getFileName(uri: Uri): String {

        val resolver = getApplication<Application>().contentResolver

        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->

            if (cursor.moveToFirst()) {

                val nameIndex =
                    cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                    )

                if (nameIndex >= 0) {

                    val name = cursor.getString(nameIndex)

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        }

        return uri.lastPathSegment ?: "Nenhum arquivo"
    }

    fun togglePlayPause() {
        if (_uiState.value.playing) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {

        if (playbackJob?.isActive == true) {
            return
        }

        player.play()

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

        player.pause()

        _uiState.value = _uiState.value.copy(
            playing = false
        )

        playbackJob?.cancel()
        playbackJob = null
    }

    fun seekForward(seconds: Double) {

        player.seekForward(seconds)

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

        player.seekBackward(seconds)

        val state = _uiState.value

        val newPosition = maxOf(
            state.position - seconds,
            0.0
        )

        _uiState.value = state.copy(
            position = newPosition
        )
    }

    fun volumeUp(amount: Double = 5.0) {

        val state = _uiState.value

        val newVolume = minOf(
            state.volume + amount,
            100.0
        )

        player.setVolume(newVolume)

        _uiState.value = state.copy(
            volume = newVolume
        )
    }

    fun volumeDown(amount: Double = 5.0) {

        val state = _uiState.value

        val newVolume = maxOf(
            state.volume - amount,
            0.0
        )

        player.setVolume(newVolume)

        _uiState.value = state.copy(
            volume = newVolume
        )
    }

    fun mute() {

        player.mute()

        _uiState.value = _uiState.value.copy(
            volume = 0.0
        )
    }

    override fun onCleared() {

        playbackJob?.cancel()

        player.release()

        super.onCleared()
    }
}
