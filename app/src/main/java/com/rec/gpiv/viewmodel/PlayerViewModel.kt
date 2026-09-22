package com.rec.gpiv.viewmodel
import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.data.FolderRepository
import com.rec.gpiv.player.FileItem
import com.rec.gpiv.player.FileList
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.MpvPlayer
import com.rec.gpiv.player.PlayerEvent
import com.rec.gpiv.player.VideoPlayer
import com.rec.gpiv.player.FILE_JUMP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToLong

class PlayerViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val folderRepository = FolderRepository(application)

    private val tagsEnabled: Boolean
        get() = _uiState.value.tagsEnabled

    private val fileTags =
        mutableMapOf<String, String>()

    @Volatile
    private var keyframeCounterEnabled = false

    val mpvNative =
        MpvNative()

    private var videoFps = 30.0
    private var frameSyncPending = false

    private val player: VideoPlayer =
        MpvPlayer(
            application.contentResolver,
            mpvNative
        )

    private val markerController = PlayerMarkerController(player)

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

    fun onVideoSelected(uri: Uri) {
        load(uri)

        folderRepository.findMatchingFolder(uri)?.let { folderUri ->
            loadFolder(folderUri)
        }
    }

    fun onFolderSelected(uri: Uri) {
        folderRepository.saveFolder(uri)
        loadFolder(uri)
        firstFile()
    }

    fun handleIncomingUri(uri: Uri) {
        folderRepository.findMatchingFolder(uri)?.let { folderUri ->
            loadFolder(folderUri)
        }

        load(uri)
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

                      if (markerController.onPositionChanged(event.position)) {
                          println(
                              "PlayerViewModel: B atingido, voltando para A"
                          )
                          frameSyncPending = true
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

    private val mediaExport = MediaExportManager(
        contentResolver = application.contentResolver,
        cacheDir = application.cacheDir,
        player = player,
        scope = viewModelScope,
        currentTreeUri = { fileList.getCurrentTreeUri() },
        currentFileName = { _uiState.value.filename }
    )

    private val cutController = PlayerCutController(
        player = player,
        cacheDir = application.cacheDir,
        scope = viewModelScope,
        currentTreeUri = { fileList.getCurrentTreeUri() },
        mediaExport = mediaExport
    )

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
            markerController.clear()

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
        markerController.clear()

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
            markerController.markerA() != null &&
            markerController.markerB() != null &&
            markerController.markerA()!!.position < markerController.markerB()!!.position
        ) {
            println(
                "PlayerViewModel: A/B loop ON " +
                "A=${markerController.markerA()!!.position} " +
                "B=${markerController.markerB()!!.position}"
            )
        }

        markerController.startPlayback()
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
        markerController.setA(FrameMarker(
            frame = _uiState.value.currentFrame,
            position = _uiState.value.position
        ))
        _uiState.value = _uiState.value.copy(markerASet = true)
        keyframeCounterEnabled = true
        updatePreviousKeyframeCounter(
            _uiState.value.currentFrame
        )

        println(
            "PlayerViewModel: A = frame=${markerController.markerA()?.frame}, position=${markerController.markerA()?.position}"
        )
    }

    fun setMarkerB() {
        markerController.setB(FrameMarker(
            frame = _uiState.value.currentFrame,
            position = _uiState.value.position
        ))
        _uiState.value = _uiState.value.copy(markerBSet = true)
        keyframeCounterEnabled = true
        updatePreviousKeyframeCounter(
            _uiState.value.currentFrame
        )

        println(
            "PlayerViewModel: B = frame=${markerController.markerB()?.frame}, position=${markerController.markerB()?.position}"
        )
    }

    fun testCurrentCut() {
        val markers = markerController.markersForCut()
        clearABMarkers()
        cutController.testCurrentCut(markers)
    }
    fun toggleABLoop() {
        if (markerController.markerA() != null && markerController.markerB() != null) {
            markerController.enableLoop()
            println("PlayerViewModel: A/B loop ON")
        }
    }

    fun disableABLoop() {
        clearABMarkers()
        println("PlayerViewModel: A/B loop OFF e marcadores limpos")
    }

    private fun clearABMarkers() {
        markerController.clear()
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

    fun screenshot() = mediaExport.screenshot()

    fun setScreenshotMethod(method: String) =
        mediaExport.setScreenshotMethod(method)

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
