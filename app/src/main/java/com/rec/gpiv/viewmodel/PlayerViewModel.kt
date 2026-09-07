package com.rec.gpiv.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.FileList
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.MpvPlayer
import com.rec.gpiv.player.PlayerEvent
import com.rec.gpiv.player.VideoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.rec.gpiv.player.SEEK_SECONDS

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

    private val fileList =
        FileList(
            application.contentResolver
        )

    private var pickerVideoLoaded = false

    private var selectedUri: Uri? = null

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

        val filename =
            getFileName(uri)

        selectedUri = uri

        pickerVideoLoaded = true

        println(
            "PlayerViewModel: carregando arquivo = $filename"
        )

        /*
         * Se a FileList já foi carregada através de uma pasta,
         * tenta posicionar o arquivo selecionado.
         */
        if (fileList.size() > 0) {

            val found =
                fileList.setCurrent(
                    uri
                )

            println(
                "PlayerViewModel: arquivo encontrado " +
                    "na FileList = $found"
            )

            println(
                "PlayerViewModel: posição atual = " +
                    fileList.currentIndex()
            )
        }

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
                "PlayerViewModel: erro ao carregar vídeo: " +
                    e.message
            )
        }
    }

    fun loadFolder(
        uri: Uri
    ) {

        println(
            "PlayerViewModel: carregando pasta = $uri"
        )

        val loaded =
            fileList.loadFromTree(
                uri
            )

        println(
            "PlayerViewModel: FileList da pasta carregada = $loaded"
        )

        println(
            "PlayerViewModel: quantidade de arquivos = " +
                fileList.size()
        )

        println(
            "PlayerViewModel: posição atual antes = " +
                fileList.currentIndex()
        )

        val currentUri =
            selectedUri

        if (
            loaded &&
            currentUri != null
        ) {

            val found =
                fileList.setCurrent(
                    currentUri
                )

            println(
                "PlayerViewModel: arquivo selecionado " +
                    "encontrado na pasta = $found"
            )

            println(
                "PlayerViewModel: posição atual depois = " +
                    fileList.currentIndex()
            )
        }
    }

    private fun getFileName(
        uri: Uri
    ): String {

        val resolver =
            getApplication<Application>()
                .contentResolver

        resolver.query(
            uri,
            arrayOf(
                OpenableColumns.DISPLAY_NAME
            ),
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
                        cursor.getString(
                            nameIndex
                        )

                    if (!name.isNullOrBlank()) {
                        return name
                    }
                }
            }
        }

        return uri.lastPathSegment
            ?: "Nenhum arquivo"
    }

    fun nextFile() {

        val file =
            fileList.next()

        if (file == null) {

            println(
                "PlayerViewModel: já está no último arquivo"
            )

            return
        }

        println(
            "PlayerViewModel: próximo arquivo = " +
                file.name
        )

        println(
            "PlayerViewModel: currentIndex = " +
                fileList.currentIndex()
        )

        selectedUri = file.uri

        _uiState.value =
            _uiState.value.copy(
                filename = file.name,
                loading = true,
                position = 0.0,
                duration = 0.0
            )

        player.load(file.uri)
    }

    fun previousFile() {

        val file =
            fileList.previous()

        if (file == null) {

            println(
                "PlayerViewModel: já está no primeiro arquivo"
            )

            return
        }

        println(
            "PlayerViewModel: arquivo anterior = " +
                file.name
        )

        println(
            "PlayerViewModel: currentIndex = " +
                fileList.currentIndex()
        )

        selectedUri = file.uri

        _uiState.value =
            _uiState.value.copy(
                filename = file.name,
                loading = true,
                position = 0.0,
                duration = 0.0
            )

        player.load(file.uri)
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

    fun seekForward(
        seconds: Double
    ) {

        player.seekForward(
            seconds
        )
    }

    fun seekBackward(
        seconds: Double
    ) {

        player.seekBackward(
            seconds
        )
    }

    fun frameForward() {

        println(
            "PlayerViewModel: frameForward()"
        )

        player.frameForward()
    }

    fun frameBackward() {

        println(
            "PlayerViewModel: frameBackward()"
        )

        player.frameBackward()
    }

    fun volumeUp(
        amount: Double = 5.0
    ) {

        val state =
            _uiState.value

        val newVolume =
            minOf(
                state.volume + amount,
                100.0
            )

        player.setVolume(
            newVolume
        )

        _uiState.value =
            state.copy(
                volume = newVolume
            )
    }

    fun volumeDown(
        amount: Double = 5.0
    ) {

        val state =
            _uiState.value

        val newVolume =
            maxOf(
                state.volume - amount,
                0.0
            )

        player.setVolume(
            newVolume
        )

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
