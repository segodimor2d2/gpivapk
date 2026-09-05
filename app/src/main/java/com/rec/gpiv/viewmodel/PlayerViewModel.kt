package com.rec.gpiv.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.MpvPlayer
import com.rec.gpiv.player.PlayerEvent
import com.rec.gpiv.player.VideoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    val mpvNative =
        MpvNative()

    private val player: VideoPlayer =
        MpvPlayer(
            application.contentResolver,
            mpvNative
        )

    private var pickerVideoLoaded = false

    private val _uiState =
        MutableStateFlow(PlayerUiState())

    val uiState: StateFlow<PlayerUiState> =
        _uiState.asStateFlow()

    init {

        observePlayerEvents()

        player.initialize()
    }

    fun onSurfaceReady() {

        println(
            "PlayerViewModel: Surface pronta"
        )
    }

    private fun observePlayerEvents() {

        viewModelScope.launch {

            player.events.collect { event ->

                println(
                    "PlayerViewModel: event = $event"
                )

                when (event) {

                    is PlayerEvent.TimePositionChanged -> {

                        _uiState.value =
                            _uiState.value.copy(
                                position = event.position
                            )
                    }

                    is PlayerEvent.DurationChanged -> {

                        _uiState.value =
                            _uiState.value.copy(
                                duration = event.duration
                            )
                    }

                    is PlayerEvent.PauseChanged -> {

                        _uiState.value =
                            _uiState.value.copy(
                                playing = !event.paused
                            )
                    }

                    is PlayerEvent.FilenameChanged -> {

                        if (!pickerVideoLoaded) {

                            _uiState.value =
                                _uiState.value.copy(
                                    filename = event.filename
                                )
                        }
                    }

                    is PlayerEvent.LoadingChanged -> {

                        _uiState.value =
                            _uiState.value.copy(
                                loading = event.loading
                            )
                    }
                }
            }
        }
    }

    fun load(uri: Uri) {

        val filename = getFileName(uri)

        pickerVideoLoaded = true

        _uiState.value =
            _uiState.value.copy(
                filename = filename,
                loading = true,
                position = 0.0,
                duration = 0.0
            )

        try {

            player.load(uri)

        } catch (e: Exception) {

            _uiState.value =
                _uiState.value.copy(
                    loading = false,
                    playing = false
                )

            println(
                "PlayerViewModel: erro ao carregar vídeo: ${e.message}"
            )
        }
    }

    private fun getFileName(uri: Uri): String {

        val resolver =
            getApplication<Application>().contentResolver

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

                    val name =
                        cursor.getString(nameIndex)

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        }

        return uri.lastPathSegment
            ?: "Nenhum arquivo"
    }

    fun togglePlayPause() {

        if (_uiState.value.playing) {
            pause()
        } else {
            play()
        }
    }

    private fun play() {

        player.play()
    }

    private fun pause() {

        player.pause()
    }

    fun seekForward(seconds: Double) {

        player.seekForward(seconds)
    }

    fun seekBackward(seconds: Double) {

        player.seekBackward(seconds)
    }

    fun volumeUp(amount: Double = 5.0) {

        val state = _uiState.value

        val newVolume =
            minOf(
                state.volume + amount,
                100.0
            )

        player.setVolume(newVolume)

        _uiState.value =
            state.copy(
                volume = newVolume
            )
    }

    fun volumeDown(amount: Double = 5.0) {

        val state = _uiState.value

        val newVolume =
            maxOf(
                state.volume - amount,
                0.0
            )

        player.setVolume(newVolume)

        _uiState.value =
            state.copy(
                volume = newVolume
            )
    }

    fun mute() {

        player.mute()

        _uiState.value =
            _uiState.value.copy(
                volume = 0.0,
                muted = true
            )
    }

    override fun onCleared() {

        player.release()

        super.onCleared()
    }
}
