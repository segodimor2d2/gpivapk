package com.rec.gpiv.viewmodel
import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.player.FileItem
import com.rec.gpiv.player.FileList
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.MpvPlayer
import com.rec.gpiv.player.PlayerEvent
import com.rec.gpiv.player.VideoPlayer
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToLong

import android.content.ContentResolver
import android.util.Log

private enum class ScreenshotMethod {
    FRAMEBUFFER,
    MPV
}

data class FrameMarker(
    val frame: Long,
    val position: Double
)

data class CutFrameInfo(
    val frameA: Long,
    val ptsA: Long,
    val frameB: Long,
    val ptsB: Long,
    val timeBaseNum: Long,
    val timeBaseDen: Long
) {

    val timeA: Double
        get() =
            ptsA.toDouble() *
                timeBaseNum /
                timeBaseDen

    val timeB: Double
        get() =
            ptsB.toDouble() *
                timeBaseNum /
                timeBaseDen

    val duration: Double
        get() = timeB - timeA
}

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val tagsEnabled: Boolean
        get() = _uiState.value.tagsEnabled

    private val fileTags =
        mutableMapOf<String, String>()

    private var screenshotMethod =
            ScreenshotMethod.FRAMEBUFFER

    private var markerA: FrameMarker? = null
    private var markerB: FrameMarker? = null
    private var abLoopEnabled = false
          private var abLoopSeekingToA = false
    @Volatile
    private var keyframeCounterEnabled = false

    private var lastCutFrameInfo: CutFrameInfo? = null

    val mpvNative =
        MpvNative()

    private var videoFps = 30.0
    private var frameSyncPending = false

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

    private var surfaceReady = false

    private var pendingUri: Uri? = null

    private val _uiState =
        MutableStateFlow(PlayerUiState())

    val uiState: StateFlow<PlayerUiState> =
        _uiState.asStateFlow()

    private val lisTags = mutableListOf<String>()

    private val tagFiles = TagFileManager(
        contentResolver = application.contentResolver,
        currentTreeUri = { fileList.getCurrentTreeUri() },
        currentFile = { fileList.current() },
        currentIndex = { fileList.currentIndex() },
        tags = lisTags,
        fileTags = fileTags,
        onFileTagChanged = { tag ->
            _uiState.update { it.copy(fileTag = tag) }
        }
    )



    fun makeTagFolders() = tagFiles.makeTagFolders()
    fun testSaveFileTag(tag: String) = tagFiles.testSaveFileTag(tag)
    fun testMoveTags() = tagFiles.testMoveTags()

    init {

        observePlayerEvents()

        player.initialize(
            application.cacheDir.absolutePath
        )
    }

    fun setTagsEnabled(
        enabled: Boolean
    ) {

        if (!enabled) {

            fileTags.clear()

            _uiState.update {
                it.copy(
                    tagsEnabled = false,
                    fileTag = null
                )
            }

            return
        }

        _uiState.update {
            it.copy(
                tagsEnabled = true
            )
        }

        val treeUri =
            fileList.getCurrentTreeUri()
                ?: return

        // tagsEnabled=true sempre garante a estrutura SAF antes da leitura.
        tagFiles.ensureGpivTagsFile(treeUri)
        tagFiles.loadLisTags(treeUri)

        val currentFile =
            fileList.current()
                ?: return

        val fileTag =
            tagFiles.readFileTag(
                treeUri,
                currentFile.uri
            )

        _uiState.update {
            it.copy(
                fileTag = fileTag
            )
        }
    }

    fun onSurfaceReady() {

        println(
            "PlayerViewModel: Surface pronta"
        )

        surfaceReady = true

        val uri =
            pendingUri

        if (uri != null) {

            println(
                "PlayerViewModel: carregando URI pendente = $uri"
            )

            pendingUri = null

            player.load(uri)
        }
    }

    private fun observePlayerEvents() {

        viewModelScope.launch {

            player.events.collect { event ->

                when (event) {

                  is PlayerEvent.TimePositionChanged -> {

                      val frame =
                          (event.position * videoFps)
                              .roundToLong()

                      frameSyncPending = false

                      _uiState.value =
                          _uiState.value.copy(
                              position = event.position,
                              currentFrame = frame
                          )

                      if (
                          abLoopSeekingToA &&
                          markerB != null &&
                          event.position < markerB!!.position
                      ) {
                          /*
                           * O mpv pode informar a primeira posição após o
                           * seek alguns frames depois de A. Basta ter voltado
                           * para antes de B para liberar o próximo ciclo.
                           */
                          abLoopSeekingToA = false
                      }

                      if (
                          abLoopEnabled &&
                          !abLoopSeekingToA &&
                          markerA != null &&
                          markerB != null &&
                          event.position >= markerB!!.position
                      ) {
                          println(
                              "PlayerViewModel: B atingido, voltando para A"
                          )

                          abLoopSeekingToA = true
                          frameSyncPending = true

                          player.seekTo(markerA!!.position)
                      }
                    }

                    is PlayerEvent.FrameNumberChanged -> {
                      // Ignorado.
                    }

                    is PlayerEvent.DurationChanged -> {

                        _uiState.value =
                            _uiState.value.copy(
                                duration = event.duration
                            )
                    }

                    is PlayerEvent.VideoFpsChanged -> {
                        videoFps = event.fps

                        println(
                            "PlayerViewModel: video FPS = $videoFps"
                        )
                    }

                    is PlayerEvent.PauseChanged -> {

                        _uiState.value =
                            _uiState.value.copy(
                                playing = !event.paused
                            )

                    }

                    is PlayerEvent.FilenameChanged -> {

                        /*
                         * Quando existe uma lista de arquivos carregada,
                         * o nome oficial vem do FileItem.
                         *
                         * O mpv pode informar um filename diferente
                         * quando o vídeo foi aberto através de FD/URI.
                         * Não devemos deixar esse evento sobrescrever
                         * o nome real obtido pelo SAF.
                         */
                        if (
                            fileList.size() == 0 &&
                            !pickerVideoLoaded
                        ) {

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

        /*
         * Primeiro tentamos encontrar o arquivo na FileList
         * através do Document ID.
         *
         * Isso funciona quando a URI recebida também é uma
         * URI SAF/DocumentsContract.
         */
        var fileFromList =
            fileList
                .all()
                .firstOrNull { file ->
                    try {
                        DocumentsContract.getDocumentId(file.uri) ==
                            DocumentsContract.getDocumentId(uri)
                    } catch (e: Exception) {
                        false
                    }
                }

        /*
         * Se a URI veio de um aplicativo externo, como o
         * RS Explorer, ela pode não ser uma URI DocumentsContract.
         *
         * Nesse caso usamos o nome do arquivo.
         */
        if (fileFromList == null) {

            val externalFileName =
                uri.path
                    ?.substringAfterLast('/')
                    ?.takeIf { it.isNotBlank() }

            if (externalFileName != null) {

                println(
                    "PlayerViewModel: procurando arquivo externo = " +
                        externalFileName
                )

                fileFromList =
                    fileList
                        .all()
                        .firstOrNull { file ->
                            file.name == externalFileName
                        }
            }
        }

        /*
         * Se encontramos o arquivo na FileList, usamos a URI SAF
         * oficial criada a partir da pasta autorizada.
         *
         * Caso contrário, mantemos a URI original.
         */
        val loadUri =
            fileFromList?.uri ?: uri

        val filename =
            fileFromList?.name
                ?: tagFiles.getFileName(uri)

        println(
            "PlayerViewModel: URI recebida = $uri"
        )

        println(
            "PlayerViewModel: URI usada para carregar = $loadUri"
        )

        println(
            "PlayerViewModel: arquivo = $filename"
        )

        selectedUri =
            loadUri

        pickerVideoLoaded =
            true

        /*
         * Sincroniza o currentIndex com o arquivo encontrado
         * na FileList.
         */
        val found =
            fileList.setCurrent(loadUri)

        _uiState.value =
            _uiState.value.copy(
                filename = filename,
                fileIndex = fileList.currentIndex(),
                fileCount = fileList.size(),
                playing = false,
                loading = true,
                position = 0.0,
                duration = 0.0
            )

        if (surfaceReady) {

            player.load(loadUri)

        } else {

            pendingUri = loadUri
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

        if (loaded && tagsEnabled) {
            tagFiles.ensureGpivTagsFile(uri)
            tagFiles.loadLisTags(uri)
        }

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

            if (found) {

                val currentFile =
                    fileList.current()

                if (currentFile != null) {

                    val fileTag =
                        if (tagsEnabled) {
                            tagFiles.readFileTag(
                                uri,
                                currentFile.uri
                            )
                        } else {
                            ""
                        }

                    _uiState.value =
                        _uiState.value.copy(
                            filename = currentFile.name,
                            fileTag = fileTag,
                            fileIndex = fileList.currentIndex(),
                            fileCount = fileList.size()
                        )
                }
            }

        }
    }

    private val trashManager = FileTrashManager(
        resolver = application.contentResolver,
        currentTreeUri = { fileList.getCurrentTreeUri() }
    )

    private fun getTrashUri(): Uri? = trashManager.findTrashUri()

    fun testTrashUri() {
        println("PlayerViewModel: testTrashUri = ${getTrashUri()}")
    }

    private fun moveToTrash(uri: Uri): Boolean =
        trashManager.moveToTrash(uri)

    fun deleteFile(permanent: Boolean): Boolean {
        val uri = selectedUri ?: return false

        if (!permanent && !moveToTrash(uri)) {
            return false
        }

        if (!permanent) {
            player.pause()
            val nextFile = fileList.remove(uri)
            selectedUri = null
            pendingUri = null
            markerA = null
            markerB = null
            abLoopEnabled = false
            abLoopSeekingToA = false

            if (nextFile != null) {
                updateCurrentFileState(nextFile)
            } else {
                _uiState.value = _uiState.value.copy(
                    filename = "Nenhum arquivo",
                    fileTag = null,
                    playing = false,
                    loading = false,
                    position = 0.0,
                    duration = 0.0,
                    fileIndex = 0,
                    fileCount = 0
                )
            }
            return true
        }

        println("PlayerViewModel: apagando arquivo = $uri")
        player.pause()
        val deleted = trashManager.deletePermanently(uri)

        if (!deleted) {
            return false
        }

        val nextFile = fileList.remove(uri)
        selectedUri = null
        pendingUri = null
        markerA = null
        markerB = null
        abLoopEnabled = false
        abLoopSeekingToA = false

        if (nextFile != null) {
            updateCurrentFileState(nextFile)
        } else {
            _uiState.value = _uiState.value.copy(
                filename = "Nenhum arquivo",
                fileTag = null,
                playing = false,
                loading = false,
                position = 0.0,
                duration = 0.0,
                fileIndex = 0,
                fileCount = 0
            )
        }

        return true
    }

    private fun updateCurrentFileState(
        file: FileItem
    ) {

        val treeUri =
            fileList.getCurrentTreeUri()
                ?: return

        selectedUri =
            file.uri

        val fileTag =
            if (tagsEnabled) {
                            tagFiles.readFileTag(
                    treeUri,
                    file.uri
                )
            } else {
                ""
            }

        _uiState.value =
            _uiState.value.copy(
                filename = file.name,
                fileTag = fileTag,
                fileIndex = fileList.currentIndex(),
                fileCount = fileList.size(),
                loading = true,
                position = 0.0,
                duration = 0.0
            )

        player.load(
            file.uri
        )
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

        updateCurrentFileState(file)
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

        updateCurrentFileState(file)
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

        updateCurrentFileState(file)
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

        updateCurrentFileState(file)
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

        updateCurrentFileState(file)
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

        keyframeCounterEnabled = false

        _uiState.update {
            it.copy(previousKeyframeFrame = null)
        }

        if (
            markerA != null &&
            markerB != null &&
            markerA!!.position < markerB!!.position
        ) {
            abLoopEnabled = true

            println(
                "PlayerViewModel: A/B loop ON " +
                "A=${markerA!!.position} " +
                "B=${markerB!!.position}"
            )
        }

        player.play()
    }

    private fun pause() {

        player.pause()
    }

    fun seekBackward(seconds: Double) {
        frameSyncPending = true
        player.seekBackward(seconds)
    }

    fun seekForward(seconds: Double) {
        frameSyncPending = true
        player.seekForward(seconds)
    }

    fun seekTo(position: Double) {
        frameSyncPending = true
        player.seekTo(position)
    }

    fun setMarkerA() {
        markerA = FrameMarker(
            frame = _uiState.value.currentFrame,
            position = _uiState.value.position
        )
        _uiState.value = _uiState.value.copy(markerASet = true)
        keyframeCounterEnabled = true
        updatePreviousKeyframeCounter(
            _uiState.value.currentFrame
        )

        println(
            "PlayerViewModel: A = frame=${markerA?.frame}, position=${markerA?.position}"
        )
    }

    fun setMarkerB() {
        markerB = FrameMarker(
            frame = _uiState.value.currentFrame,
            position = _uiState.value.position
        )
        _uiState.value = _uiState.value.copy(markerBSet = true)
        keyframeCounterEnabled = true
        updatePreviousKeyframeCounter(
            _uiState.value.currentFrame
        )

        println(
            "PlayerViewModel: B = frame=${markerB?.frame}, position=${markerB?.position}"
        )
    }

    fun testCurrentCut() {
        val a = markerA
        val b = markerB

        clearABMarkers()

        if (a == null || b == null) {
            println("PlayerViewModel: A/B não definidos")
            return
        }

        val treeUri =
            fileList.getCurrentTreeUri()

        if (treeUri == null) {
            println(
                "PlayerViewModel: nenhuma pasta SAF selecionada"
            )
            return
        }

        val start =
            if (a.frame <= b.frame) a else b

        val end =
            if (a.frame <= b.frame) b else a

        println(
            "PlayerViewModel: ordem do corte " +
                "start=${start.frame} end=${end.frame}"
        )

        println(
            "PlayerViewModel: testCutFrames " +
                "A=${start.frame} B=${end.frame}"
        )

        val result =
            player.testCutFrames(
                frameA = start.frame,
                frameB = end.frame
            )

        if (result != null && result.size >= 6) {

            val cutInfo =
                CutFrameInfo(
                    frameA = result[0],
                    ptsA = result[1],
                    frameB = result[2],
                    ptsB = result[3],
                    timeBaseNum = result[4],
                    timeBaseDen = result[5]
                )

            lastCutFrameInfo =
                cutInfo

            val timeA =
                cutInfo.timeA

            val timeB =
                cutInfo.timeB

            println(
                "PlayerViewModel: corte " +
                    "A=${cutInfo.frameA} " +
                    "timeA=$timeA " +
                    "B=${cutInfo.frameB} " +
                    "timeB=$timeB " +
                    "duration=${cutInfo.duration}"
            )

            val cutFile =
                File(
                    getApplication<Application>()
                        .cacheDir,
                    "frameA.mov"
                )

            if (!cutFile.isFile || cutFile.length() <= 0L) {

                println(
                    "PlayerViewModel: arquivo do corte " +
                        "não encontrado ou vazio = " +
                        cutFile.absolutePath
                )

                return
            }

            println(
                "PlayerViewModel: vídeo cortado encontrado = " +
                    cutFile.absolutePath +
                    " size=" +
                    cutFile.length()
            )

            viewModelScope.launch {

                copyCutVideoToTree(
                    cutFile,
                    treeUri
                )
            }

        } else {

            println(
                "PlayerViewModel: testCutFrames sem resultado"
            )
        }
    }
    fun toggleABLoop() {
        if (markerA != null && markerB != null) {
            abLoopEnabled = true
            println("PlayerViewModel: A/B loop ON")
        }
    }

    fun disableABLoop() {
        clearABMarkers()
        println("PlayerViewModel: A/B loop OFF e marcadores limpos")
    }

    private fun clearABMarkers() {
        markerA = null
        markerB = null
        abLoopEnabled = false
        abLoopSeekingToA = false
        _uiState.value =
            _uiState.value.copy(
                markerASet = false,
                markerBSet = false
            )
    }

    fun frameForward(frames: Int) {

        player.frameForward(frames)

        _uiState.update {
            it.copy(
                currentFrame =
                    it.currentFrame + frames
            )
        }

        keyframeCounterEnabled = true
        updatePreviousKeyframeCounter(
            _uiState.value.currentFrame
        )
    }

    fun frameBackward(frames: Int) {

        player.frameBackward(frames)

        _uiState.update {
            it.copy(
                currentFrame =
                    maxOf(
                        0L,
                        it.currentFrame - frames
                    )
            )
        }

        keyframeCounterEnabled = true
        updatePreviousKeyframeCounter(
            _uiState.value.currentFrame
        )
    }

    private fun updatePreviousKeyframeCounter(
        frame: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val keyframe =
                player.findPreviousKeyframeFrame(frame)

            _uiState.update { state ->
                if (
                    keyframeCounterEnabled &&
                    state.currentFrame == frame
                ) {
                    state.copy(
                        previousKeyframeFrame = keyframe
                    )
                } else {
                    state
                }
            }
        }
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

        when (screenshotMethod) {

            ScreenshotMethod.FRAMEBUFFER -> {
                player.screenshot()
            }

            ScreenshotMethod.MPV -> {
                player.screenshotMpv()
            }
        }

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

    fun setScreenshotMethod(method: String) {

        screenshotMethod =
            when (method) {

                "MPV" ->
                    ScreenshotMethod.MPV

                else ->
                    ScreenshotMethod.FRAMEBUFFER
            }

        println(
            "PlayerViewModel: screenshot method = $screenshotMethod"
        )
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
        baseName: String,
        screenshotExtension: String
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

                if (!name.endsWith(screenshotExtension))
                    continue


                val numberText =
                    name
                        .removePrefix(prefix)
                        .removeSuffix(screenshotExtension)
                        .removePrefix("_")

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
            "%s_%03d%s",
            baseName,
            number,
            screenshotExtension
        )
    }

    private fun findNextCutVideoName(
        resolver: ContentResolver,
        parentDocumentUri: Uri,
        baseName: String,
        extension: String
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

                if (nameColumn < 0) {
                    continue
                }

                val name =
                    cursor.getString(nameColumn)

                val match =
                    Regex(
                        "^${Regex.escape(baseName)}_(\\d+)${Regex.escape(extension)}$",
                        RegexOption.IGNORE_CASE
                    ).matchEntire(name)

                val number =
                    match
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.toIntOrNull()

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
            "%s_%03d%s",
            baseName,
            number,
            extension
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

                /*
                 * ------------------------------------------------
                 * NOME DO SCREENSHOT
                 * ------------------------------------------------
                 */

                val originalName =
                    _uiState.value.filename
                        ?: "screenshot"

                val lastDot =
                    originalName.lastIndexOf(".")

                val hasExtension =
                    lastDot > 0 &&
                        lastDot < originalName.length - 1

                val baseName =
                    if (hasExtension) {
                        originalName.substring(
                            0,
                            lastDot
                        )
                    } else {
                        originalName
                    }

                val screenshotExtension =
                    if (hasExtension) {
                        ".jpg"
                    } else {
                        ""
                    }

                val destinationName =
                    findNextScreenshotName(
                        resolver,
                        parentDocumentUri,
                        baseName,
                        screenshotExtension
                    )

                /*
                 * ------------------------------------------------
                 * CRIAR ARQUIVO NA PASTA
                 * ------------------------------------------------
                 */


                val mimeType =
                    if (hasExtension) {
                        "image/jpeg"
                    } else {
                        "application/octet-stream"
                    }

                val hiddenWithoutExtension =
                    !hasExtension &&
                        destinationName.startsWith(".")

                val createName =
                    if (hiddenWithoutExtension) {
                        destinationName.removePrefix(".")
                    } else {
                        destinationName
                    }


                val destinationUri =
                    DocumentsContract.createDocument(
                        resolver,
                        parentDocumentUri,
                        mimeType,
                        createName
                    )

                if (destinationUri == null) {

                    println(
                      "PlayerViewModel: não foi possível criar " +
                          "arquivo SAF"
                  )

                  return@withContext
                }

                val finalDestinationUri =
                    if (hiddenWithoutExtension) {

                        DocumentsContract.renameDocument(
                            resolver,
                            destinationUri,
                            destinationName
                        )

                    } else {
                        destinationUri
                    } ?: run {

                        println(
                            "PlayerViewModel: não foi possível " +
                                "renomear arquivo SAF"
                        )

                        return@withContext
                    }


                /*
                 * ------------------------------------------------
                 * COPIAR JPEG
                 * ------------------------------------------------
                 */

                resolver
                    .openOutputStream(
                        finalDestinationUri
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


            } catch (e: Exception) {

                println(
                    "PlayerViewModel: erro ao copiar screenshot: " +
                        e.message
                )
            }
        }
    }

    private suspend fun copyCutVideoToTree(
        cutFile: File,
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

                /*
                 * ------------------------------------------------
                 * NOME DO VÍDEO ORIGINAL
                 * ------------------------------------------------
                 */
                val originalName =
                    _uiState.value.filename
                        ?: "video"

                val lastDot =
                    originalName.lastIndexOf(".")

                val baseName: String
                val extension: String

                if (
                    lastDot > 0 &&
                    lastDot < originalName.length - 1
                ) {
                    baseName =
                        originalName.substring(
                            0,
                            lastDot
                        )

                    extension =
                        originalName.substring(
                            lastDot
                        )
                } else {
                    baseName = originalName
                    extension = ""
                }

                /*
                 * ------------------------------------------------
                 * PRÓXIMO NOME
                 * ------------------------------------------------
                 *
                 * exemplo:
                 *
                 * video_001.mov
                 * video_002.mov
                 * video_003.mov
                 * ------------------------------------------------
                 */
                val destinationName =
                    findNextCutVideoName(
                        resolver,
                        parentDocumentUri,
                        baseName,
                        extension
                    )

                /*
                 * ------------------------------------------------
                 * CRIAR ARQUIVO NA PASTA SAF
                 * ------------------------------------------------
                 */
                println(
                    "PlayerViewModel: destinationName = [$destinationName]"
                )
                val destinationUri =
                    DocumentsContract.createDocument(
                        resolver,
                        parentDocumentUri,
                        "application/octet-stream",
                        destinationName
                    )

                if (destinationUri == null) {

                    println(
                        "PlayerViewModel: não foi possível criar " +
                            "vídeo cortado na pasta SAF"
                    )

                    return@withContext
                }

                /*
                 * ------------------------------------------------
                 * COPIAR MOV
                 * ------------------------------------------------
                 */
                resolver
                    .openOutputStream(
                        destinationUri
                    )
                    ?.use { output ->

                        cutFile
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
                                "abrir outputStream do vídeo"
                        )

                        return@withContext
                    }

                println(
                    "PlayerViewModel: vídeo cortado copiado " +
                        "com sucesso: $destinationName"
                )

            } catch (e: Exception) {

                println(
                    "PlayerViewModel: erro ao copiar vídeo cortado: " +
                        e.message
                )
            }
        }
    }

    private val controls = PlayerControls(
        player = player,
        mpvNative = mpvNative,
        readState = { _uiState.value },
        writeState = { _uiState.value = it }
    )

    fun volumeUp(amount: Double = 5.0) = controls.volumeUp(amount)
    fun volumeDown(amount: Double = 5.0) = controls.volumeDown(amount)
    fun mute() = controls.mute()
    fun brightnessUp() = controls.brightnessUp()
    fun brightnessDown() = controls.brightnessDown()
    fun contrastUp() = controls.contrastUp()
    fun contrastDown() = controls.contrastDown()
    fun gammaUp() = controls.gammaUp()
    fun gammaDown() = controls.gammaDown()
    fun saturationUp() = controls.saturationUp()
    fun saturationDown() = controls.saturationDown()
    fun resetVideoAdjustments() = controls.resetVideoAdjustments()

    override fun onCleared() {

        player.release()

        super.onCleared()
    }
}
