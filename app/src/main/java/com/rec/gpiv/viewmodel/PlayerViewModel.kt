package com.rec.gpiv.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.FileList
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.MpvPlayer
import com.rec.gpiv.player.PlayerEvent
import com.rec.gpiv.player.VideoPlayer
import com.rec.gpiv.player.SEEK_SECONDS
import com.rec.gpiv.player.FILE_JUMP
import com.rec.gpiv.player.MAX_VOLUME
import com.rec.gpiv.player.VOLUME_STEP

import com.rec.gpiv.player.BRIGHTNESS_STEP
import com.rec.gpiv.player.CONTRAST_STEP
import com.rec.gpiv.player.GAMMA_STEP
import com.rec.gpiv.player.SATURATION_STEP

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import android.content.ContentResolver

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    val mpvNative =
        MpvNative()

    private var volumeBeforeMute = 100.0

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

        player.initialize(
            application.cacheDir.absolutePath
        )
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

    fun firstFile() {

        val file =
            fileList.first()

        if (file == null) {
            println(
                "PlayerViewModel: lista vazia"
            )
            return
        }

        println(
            "PlayerViewModel: primeiro arquivo = " +
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

    fun lastFile() {

        val file =
            fileList.last()

        if (file == null) {
            println(
                "PlayerViewModel: lista vazia"
            )
            return
        }

        println(
            "PlayerViewModel: último arquivo = " +
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

    fun jumpFiles(offset: Int) {

        val file =
            fileList.jump(offset)

        if (file == null) {
            println(
                "PlayerViewModel: não foi possível pular arquivos"
            )
            return
        }

        println(
            "PlayerViewModel: salto de " +
                offset +
                " arquivos -> " +
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

    fun jumpFilesForward() {
        jumpFiles(FILE_JUMP)
    }

    fun jumpFilesBackward() {
        jumpFiles(-FILE_JUMP)
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

    fun screenshot() {

        println(
            "PlayerViewModel: screenshot()"
        )

        val cacheDir =
            getApplication<Application>()
                .cacheDir

        val treeUri =
            fileList.getCurrentTreeUri()

        if (treeUri == null) {

            println(
                "PlayerViewModel: nenhuma pasta SAF selecionada"
            )

            return
        }

        /*
         * Guarda os screenshots existentes antes
         * de disparar o novo screenshot.
         */
        val existingFiles =
            cacheDir
                .listFiles()
                ?.filter {
                    it.name.startsWith("mpv-shot") &&
                    it.name.endsWith(".jpg")
                }
                ?.map {
                    it.name
                }
                ?.toSet()
                ?: emptySet()

        /*
         * Dispara o screenshot no mpv.
         *
         * O arquivo será criado assincronamente
         * dentro do cacheDir.
         */
        player.screenshot()

        viewModelScope.launch {

            val screenshotFile =
                waitForNewScreenshot(
                    cacheDir,
                    existingFiles
                )

            if (screenshotFile == null) {

                println(
                    "PlayerViewModel: screenshot não apareceu no cache"
                )

                return@launch
            }

            println(
                "PlayerViewModel: screenshot encontrado = " +
                    screenshotFile.absolutePath
            )

            copyScreenshotToTree(
                screenshotFile,
                treeUri
            )
        }
    }

    private suspend fun waitForNewScreenshot(
        cacheDir: File,
        existingFiles: Set<String>
    ): File? {

        repeat(50) {

            val file =
                cacheDir
                    .listFiles()
                    ?.firstOrNull {

                        it.isFile &&
                        it.name.startsWith("mpv-shot") &&
                        it.name.endsWith(".jpg") &&
                        it.name !in existingFiles
                    }

            if (file != null) {

                /*
                 * O mpv pode ter criado o arquivo,
                 * mas ainda estar escrevendo nele.
                 *
                 * Esperamos o tamanho estabilizar.
                 */

                var previousSize =
                    file.length()

                repeat(10) {

                    delay(100)

                    val currentSize =
                        file.length()

                    if (
                        currentSize > 0L &&
                        currentSize == previousSize
                    ) {

                        return file
                    }

                    previousSize =
                        currentSize
                }
            }

            delay(100)
        }

        return null
    }


    private fun findNextScreenshotName(
        resolver: ContentResolver,
        parentDocumentUri: Uri,
        baseName: String
    ): String {

        val childrenUri =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                parentDocumentUri,
                DocumentsContract.getDocumentId(
                    parentDocumentUri
                )
            )

        val usedNumbers =
            mutableSetOf<Int>()

        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->

            val nameColumn =
                cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )

            while (cursor.moveToNext()) {

                if (nameColumn < 0)
                    continue

                val name =
                    cursor.getString(nameColumn)

                val prefix =
                    baseName

                if (!name.startsWith(prefix))
                    continue

                if (!name.endsWith(".jpg"))
                    continue

                val numberText =
                    name
                        .removePrefix(prefix)
                        .removeSuffix(".jpg")

                val number =
                    numberText.toIntOrNull()

                if (number != null) {
                    usedNumbers.add(number)
                }
            }
        }

        var number = 1

        while (number in usedNumbers) {
            number++
        }

        return String.format(
            "%s%03d.jpg",
            baseName,
            number
        )
    }

    private suspend fun copyScreenshotToTree(
        screenshotFile: File,
        treeUri: Uri
    ) {

        withContext(Dispatchers.IO) {

            try {

                val resolver =
                    getApplication<Application>()
                        .contentResolver

                /*
                 * ------------------------------------------------
                 * CONVERTER treeUri -> documentUri DA PASTA
                 * ------------------------------------------------
                 */

                val treeDocumentId =
                    DocumentsContract.getTreeDocumentId(
                        treeUri
                    )

                val parentDocumentUri =
                    DocumentsContract.buildDocumentUriUsingTree(
                        treeUri,
                        treeDocumentId
                    )

                println(
                    "PlayerViewModel: pasta destino = " +
                        parentDocumentUri
                )


                /*
                 * ------------------------------------------------
                 * NOME DO SCREENSHOT
                 * ------------------------------------------------
                 */

                val originalName =
                    _uiState.value.filename
                        ?: "screenshot"

                val baseName =
                    originalName
                        .substringBeforeLast(
                            ".",
                            originalName
                        )

                val destinationName =
                    findNextScreenshotName(
                        resolver,
                        parentDocumentUri,
                        baseName
                    )

                println(
                    "PlayerViewModel: criando arquivo SAF = " +
                        destinationName
                )


                /*
                 * ------------------------------------------------
                 * CRIAR ARQUIVO NA PASTA
                 * ------------------------------------------------
                 */

                val destinationUri =
                    DocumentsContract.createDocument(
                        resolver,
                        parentDocumentUri,
                        "image/jpeg",
                        destinationName
                    )

                if (destinationUri == null) {

                    println(
                        "PlayerViewModel: não foi possível criar " +
                            "arquivo SAF"
                    )

                    return@withContext
                }


                println(
                    "PlayerViewModel: destino = " +
                        destinationUri
                )


                /*
                 * ------------------------------------------------
                 * COPIAR JPEG
                 * ------------------------------------------------
                 */

                resolver
                    .openOutputStream(
                        destinationUri
                    )
                    ?.use { output ->

                        screenshotFile
                            .inputStream()
                            .use { input ->

                                input.copyTo(
                                    output
                                )
                            }
                    }
                    ?: run {

                        println(
                            "PlayerViewModel: não foi possível " +
                                "abrir outputStream"
                        )

                        return@withContext
                    }


                /*
                 * ------------------------------------------------
                 * SUCESSO
                 * ------------------------------------------------
                 */

                println(
                    "PlayerViewModel: screenshot copiado " +
                        "com sucesso para a pasta SAF"
                )

                println(
                    "PlayerViewModel: arquivo = " +
                        destinationName
                )

                println(
                    "PlayerViewModel: tamanho = " +
                        screenshotFile.length() +
                        " bytes"
                )

            } catch (e: Exception) {

                println(
                    "PlayerViewModel: erro ao copiar screenshot: " +
                        e.message
                )
            }
        }
    }

    fun volumeUp(
        amount: Double = 5.0
    ) {

        val state =
            _uiState.value

        val newVolume =
            minOf(
                state.volume + amount,
                MAX_VOLUME
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


  fun brightnessUp() {

      val state =
          _uiState.value

      val newValue =
          minOf(
              state.brightness + BRIGHTNESS_STEP,
              100.0
          )

      mpvNative.changeBrightness(
          BRIGHTNESS_STEP
      )

      _uiState.value =
          state.copy(
              brightness = newValue
          )
  }

  fun brightnessDown() {

      val state =
          _uiState.value

      val newValue =
          maxOf(
              state.brightness - BRIGHTNESS_STEP,
              -100.0
          )

      mpvNative.changeBrightness(
          -BRIGHTNESS_STEP
      )

      _uiState.value =
          state.copy(
              brightness = newValue
          )
  }


    fun contrastUp() {

        val state =
            _uiState.value

        val newValue =
            minOf(
                state.contrast + CONTRAST_STEP,
                100.0
            )

        mpvNative.changeContrast(
            CONTRAST_STEP
        )

        _uiState.value =
            state.copy(
                contrast = newValue
            )
    }

    fun contrastDown() {

        val state =
            _uiState.value

        val newValue =
            maxOf(
                state.contrast - CONTRAST_STEP,
                -100.0
            )

        mpvNative.changeContrast(
            -CONTRAST_STEP
        )

        _uiState.value =
            state.copy(
                contrast = newValue
            )
    }

    fun gammaUp() {

        val state =
            _uiState.value

        val newValue =
            minOf(
                state.gamma + GAMMA_STEP,
                100.0
            )

        mpvNative.changeGamma(
            GAMMA_STEP
        )

        _uiState.value =
            state.copy(
                gamma = newValue
            )
    }

    fun gammaDown() {

        val state =
            _uiState.value

        val newValue =
            maxOf(
                state.gamma - GAMMA_STEP,
                -100.0
            )

        mpvNative.changeGamma(
            -GAMMA_STEP
        )

        _uiState.value =
            state.copy(
                gamma = newValue
            )
    }

    fun saturationUp() {

        val state =
            _uiState.value

        val newValue =
            minOf(
                state.saturation + SATURATION_STEP,
                100.0
            )

        mpvNative.changeSaturation(
            SATURATION_STEP
        )

        _uiState.value =
            state.copy(
                saturation = newValue
            )
    }

    fun saturationDown() {

        val state =
            _uiState.value

        val newValue =
            maxOf(
                state.saturation - SATURATION_STEP,
                -100.0
            )

        mpvNative.changeSaturation(
            -SATURATION_STEP
        )

        _uiState.value =
            state.copy(
                saturation = newValue
            )
    }


    fun resetVideoAdjustments() {

        val state =
            _uiState.value

        if (state.brightness != 0.0) {
            mpvNative.changeBrightness(
                -state.brightness
            )
        }

        if (state.contrast != 0.0) {
            mpvNative.changeContrast(
                -state.contrast
            )
        }

        if (state.gamma != 0.0) {
            mpvNative.changeGamma(
                -state.gamma
            )
        }

        if (state.saturation != 0.0) {
            mpvNative.changeSaturation(
                -state.saturation
            )
        }

        mpvNative.resetView()

        _uiState.value =
            state.copy(
                brightness = 0.0,
                contrast = 0.0,
                gamma = 0.0,
                saturation = 0.0
            )
    }

    fun mute() {

        val state =
            _uiState.value

        if (!state.muted) {

            volumeBeforeMute =
                state.volume

            player.mute()

            _uiState.value =
                state.copy(
                    volume = 0.0,
                    muted = true
                )

        } else {

            player.mute()

            player.setVolume(
                volumeBeforeMute
            )

            _uiState.value =
                state.copy(
                    volume = volumeBeforeMute,
                    muted = false
                )
        }
    }

    override fun onCleared() {

        player.release()

        super.onCleared()
    }
}
