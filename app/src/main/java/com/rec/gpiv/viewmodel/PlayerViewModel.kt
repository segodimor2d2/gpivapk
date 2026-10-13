package com.rec.gpiv.viewmodel
import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rec.gpiv.model.ThumbnailEntry
import com.rec.gpiv.data.ThumbnailRepository
import kotlinx.coroutines.ensureActive
import com.rec.gpiv.model.PlayerUiState
import com.rec.gpiv.model.OperationStatus
import com.rec.gpiv.data.FolderRepository
import com.rec.gpiv.player.FileItem
import com.rec.gpiv.player.FileList
import com.rec.gpiv.player.MpvNative
import com.rec.gpiv.player.MpvPlayer
import com.rec.gpiv.player.PlayerEvent
import com.rec.gpiv.player.VideoPlayer
import com.rec.gpiv.player.FILE_JUMP
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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

    private val thumbnailRepository = ThumbnailRepository(application.contentResolver, application.cacheDir)
    private var galleryJob: Job? = null

    private val ahpRepository = com.rec.gpiv.data.AhpRepository(application)
    private var ahpFolder: Uri? = null

    fun openThumbnailGallery() {
        if (_uiState.value.ahpBusy) return
        galleryJob?.cancel()
        val folder = fileList.getTagFolderUri() ?: return
        ahpFolder = folder
        _uiState.update { it.copy(galleryVisible = true, thumbnails = emptyList(), ahpValues = emptyList(),
            ahpReady = false, ahpMessage = "Identificando imagens e vídeos…") }
        setOperationStatus(FeedbackOperation.THUMBNAILS, OperationStatus.RUNNING)
        galleryJob = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { ahpRepository.recover() }
                if (folder.scheme == "file") fileList.loadFromDirectory(java.io.File(requireNotNull(folder.path)))
                else fileList.loadFromTree(folder)
                val files = fileList.all()
                val persist = withContext(Dispatchers.IO) { thumbnailRepository.prepare(folder) }
                val entries = mutableListOf<ThumbnailEntry>()
                for (file in files) {
                    ensureActive()
                    val type = withContext(Dispatchers.IO) { ahpRepository.mediaType(file.uri) } ?: continue
                    val preview = withContext(Dispatchers.IO) { thumbnailRepository.thumbnail(file, persist) }
                    entries += ThumbnailEntry(file.uri, file.name, preview?.absolutePath, true, type, file.modifiedTime)
                    _uiState.update { it.copy(thumbnails = entries.toList()) }
                }
                val values = withContext(Dispatchers.IO) { ahpRepository.read(folder, entries) }
                _uiState.update { it.copy(ahpReady = true, ahpValues = values,
                    ahpMessage = if (entries.isEmpty()) "Nenhuma imagem ou vídeo nesta pasta." else null) }
                setOperationStatus(FeedbackOperation.THUMBNAILS, OperationStatus.SUCCESS)
                if (entries.size > 1 && com.rec.gpiv.ahp.AhpRanking.pair(entries.size, values.size) == null) finishAhp()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(ahpMessage = e.message ?: "Falha ao preparar a avaliação.") }
                setOperationStatus(FeedbackOperation.THUMBNAILS, OperationStatus.ERROR)
            }
        }
    }

    fun closeThumbnailGallery() {
        if (_uiState.value.ahpBusy) return
        galleryJob?.cancel()
        setOperationStatus(FeedbackOperation.THUMBNAILS, OperationStatus.IDLE)
        _uiState.update { it.copy(galleryVisible = false, thumbnails = emptyList(), ahpReady = false) }
    }

    fun compareThumbnails(value: Int) {
        val state = _uiState.value
        if (!state.ahpReady || state.ahpBusy || value !in -8..8) return
        if (com.rec.gpiv.ahp.AhpRanking.pair(state.thumbnails.size, state.ahpValues.size) == null) return
        val folder = ahpFolder ?: return
        val values = state.ahpValues + value
        _uiState.update { it.copy(ahpBusy = true) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { ahpRepository.save(folder, state.thumbnails, values) }
                _uiState.update { it.copy(ahpValues = values, ahpBusy = false, ahpMessage = null) }
                if (com.rec.gpiv.ahp.AhpRanking.pair(state.thumbnails.size, values.size) == null) finishAhp()
            } catch (e: Exception) {
                _uiState.update { it.copy(ahpBusy = false, ahpMessage = e.message) }
            }
        }
    }

    fun finishAhp() {
        val state = _uiState.value
        val folder = ahpFolder ?: return
        if (!state.ahpReady || state.ahpBusy || state.thumbnails.isEmpty()) return
        if (com.rec.gpiv.ahp.AhpRanking.pair(state.thumbnails.size, state.ahpValues.size) != null) return
        _uiState.update { it.copy(ahpBusy = true, ahpMessage = "Renomeando arquivos…") }
        viewModelScope.launch {
            try {
                val (renamed, consistency) = withContext(Dispatchers.IO) {
                    ahpRepository.finish(folder, state.thumbnails, state.ahpValues)
                }
                val cleanupError = withContext(Dispatchers.IO) {
                    runCatching { thumbnailRepository.cleanup(folder, state.thumbnails) }.exceptionOrNull()
                }
                if (fileList.getTagFolderUri() == folder) {
                    if (folder.scheme == "file") fileList.loadFromDirectory(java.io.File(requireNotNull(folder.path)))
                    else fileList.loadFromTree(folder)
                    reloadTags()
                    firstFile()
                }
                _uiState.update { it.copy(ahpBusy = false, ahpReady = false, thumbnails = emptyList(),
                    ahpMessage = "${renamed.size} arquivos renomeados. $consistency" +
                        (cleanupError?.let { error -> "\nMiniaturas mantidas: ${error.message}" } ?: "")) }
                setOperationStatus(FeedbackOperation.THUMBNAILS, OperationStatus.SUCCESS)
            } catch (e: Exception) {
                _uiState.update { it.copy(ahpBusy = false, ahpMessage = e.message ?: "Erro ao renomear arquivos.") }
                setOperationStatus(FeedbackOperation.THUMBNAILS, OperationStatus.ERROR)
            }
        }
    }

    private val lisTags = mutableListOf<String>()

    private enum class FeedbackOperation { SCREENSHOT_MPV, SCREENSHOT, CUT, MAKE_FOLDERS, MOVE_TAGS, THUMBNAILS }
    private val feedbackTimers = mutableMapOf<FeedbackOperation, Job>()

    private fun setOperationStatus(operation: FeedbackOperation, status: OperationStatus) {
        feedbackTimers.remove(operation)?.cancel()
        updateOperationStatus(operation, status)
        if (status == OperationStatus.SUCCESS || status == OperationStatus.ERROR) {
            feedbackTimers[operation] = viewModelScope.launch {
                delay(if (operation == FeedbackOperation.THUMBNAILS) 2_000 else 2_00)
                updateOperationStatus(operation, OperationStatus.IDLE)
                feedbackTimers.remove(operation)
            }
        }
    }

    private fun updateOperationStatus(operation: FeedbackOperation, status: OperationStatus) {
        _uiState.update {
            when (operation) {
                FeedbackOperation.SCREENSHOT_MPV -> it.copy(screenshotMpvStatus = status)
                FeedbackOperation.SCREENSHOT -> it.copy(screenshotStatus = status)
                FeedbackOperation.CUT -> it.copy(cutStatus = status)
                FeedbackOperation.MAKE_FOLDERS -> it.copy(makeFoldersStatus = status)
                FeedbackOperation.MOVE_TAGS -> it.copy(moveTagsStatus = status)
                FeedbackOperation.THUMBNAILS -> it.copy(thumbnailStatus = status)
            }
        }
    }

    private val tagFiles = TagFileManager(
        contentResolver = application.contentResolver,
        currentTreeUri = { fileList.getTagFolderUri() },
        currentFile = { fileList.current() },
        currentIndex = { fileList.currentIndex() },
        tags = lisTags,
        fileTags = fileTags,
        onFileTagChanged = { tag ->
            _uiState.update { it.copy(fileTag = tag) }
        }
    )



    fun makeTagFolders() = runTagOperation(move = false)

    fun testSaveFileTag(tag: String) {
        if (_uiState.value.movingTaggedFiles) return
        tagFiles.testSaveFileTag(tag)
        syncTagsWithUiState()
    }

    fun testMoveTags() = runTagOperation(move = true)

    private fun runTagOperation(move: Boolean) {
        if (_uiState.value.movingTaggedFiles) return
        val operation = if (move) FeedbackOperation.MOVE_TAGS else FeedbackOperation.MAKE_FOLDERS
        val folder = fileList.getTagFolderUri()
        if (folder == null) {
            setOperationStatus(operation, OperationStatus.ERROR)
            return
        }
        val file = fileList.current()
        val index = fileList.currentIndex()
        // The worker owns its collections and keeps the original folder even
        // when the user navigates elsewhere during a long move.
        val movedTags = lisTags.toMutableList()
        val movedFileTags = fileTags.toMutableMap()
        val worker = TagFileManager(
            contentResolver = getApplication<Application>().contentResolver,
            currentTreeUri = { folder },
            currentFile = { file },
            currentIndex = { index },
            tags = movedTags,
            fileTags = movedFileTags,
            onFileTagChanged = {}
        )
        setOperationStatus(operation, OperationStatus.RUNNING)
        _uiState.update { it.copy(movingTaggedFiles = true) }
        viewModelScope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    val result = if (move) worker.testMoveTags() else worker.makeTagFolders()
                    worker.loadLisTags(folder)
                    result
                }
                setOperationStatus(operation, if (success) OperationStatus.SUCCESS else OperationStatus.ERROR)
                if (fileList.getTagFolderUri() == folder && tagsEnabled) {
                    lisTags.clear()
                    lisTags.addAll(movedTags)
                    fileTags.clear()
                    fileTags.putAll(movedFileTags)
                    _uiState.update {
                        it.copy(
                            tags = lisTags.toList(),
                            fileTag = fileList.current()?.uri?.toString()?.let(fileTags::get)
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                setOperationStatus(operation, OperationStatus.ERROR)
            } finally {
                _uiState.update { it.copy(movingTaggedFiles = false) }
            }
        }
    }

    private fun reloadTags() {
        fileTags.clear()
        lisTags.clear()
        if (tagsEnabled) {
            fileList.getTagFolderUri()?.let {
                tagFiles.ensureGpivTagsFile(it)
                tagFiles.loadLisTags(it)
            }
        }
        syncTagsWithUiState()
    }

    private fun syncTagsWithUiState() {
        _uiState.update { it.copy(tags = lisTags.toList()) }
    }

    init {

        observePlayerEvents()

        player.initialize(
            application.cacheDir.absolutePath
        )
    }

    fun setTagsEnabled(
        enabled: Boolean
    ) {
        if (_uiState.value.movingTaggedFiles) return

        if (!enabled) {

            fileTags.clear()

            _uiState.update {
                it.copy(
                    tagsEnabled = false,
                    fileTag = null,
                    tags = emptyList()
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
            fileList.getTagFolderUri()
                ?: return

        // tagsEnabled=true sempre garante a estrutura SAF antes da leitura.
        tagFiles.ensureGpivTagsFile(treeUri)
        tagFiles.loadLisTags(treeUri)
        syncTagsWithUiState()

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

    private var incomingFileJob: Job? = null

    fun onVideoSelected(uri: Uri) = handleIncomingUri(uri)

    fun onLocalFolderSelected(directory: java.io.File, selectedFile: java.io.File? = null) {
        incomingFileJob?.cancel()
        fileList.loadFromDirectory(directory)
        _uiState.update { it.copy(fileTag = null, tags = emptyList(), fileIndex = -1, fileCount = fileList.size()) }
        reloadTags()
        if (selectedFile != null) load(Uri.fromFile(selectedFile)) else firstFile()
    }

    fun onFolderSelected(uri: Uri) {
        incomingFileJob?.cancel()
        folderRepository.saveFolder(uri)
        loadFolder(uri)
        firstFile()
    }

    fun handleIncomingUri(uri: Uri) {
        incomingFileJob?.cancel()
        incomingFileJob = viewModelScope.launch {
            val localFile = withContext(Dispatchers.IO) { folderRepository.findLocalFile(uri) }
            // Limpa a pasta anterior para nunca abrir outro arquivo com o mesmo nome.
            fileList.clear()
            _uiState.update { it.copy(fileTag = null, tags = emptyList(), fileIndex = -1, fileCount = 0) }
            if (localFile != null) {
                fileList.loadFromDirectory(requireNotNull(localFile.parentFile))
                reloadTags()
                load(Uri.fromFile(localFile))
            } else {
                folderRepository.findMatchingFolder(uri)?.let(::loadFolder)
                load(uri)
            }
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
                    if (uri.scheme == "file") file.uri == uri else try {
                        file.uri.authority == uri.authority && DocumentsContract.getDocumentId(file.uri) ==
                            DocumentsContract.getDocumentId(uri)
                    } catch (e: Exception) {
                        false
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
                ?: if (uri.scheme == "file") java.io.File(requireNotNull(uri.path)).name else tagFiles.getFileName(uri)

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
                fileTag = if (tagsEnabled) fileTags[loadUri.toString()] else null,
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
            syncTagsWithUiState()
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
        currentFileName = { _uiState.value.filename },
        currentFileUri = { fileList.current()?.uri },
        onScreenshotStatus = { mpv, status ->
            setOperationStatus(
                if (mpv) FeedbackOperation.SCREENSHOT_MPV else FeedbackOperation.SCREENSHOT,
                status
            )
        }
    )

    private val cutController = PlayerCutController(
        player = player,
        cacheDir = application.cacheDir,
        scope = viewModelScope,
        currentTreeUri = { fileList.getTagFolderUri() },
        mediaExport = mediaExport,
        currentFileName = { _uiState.value.filename },
        onStatus = { status -> setOperationStatus(FeedbackOperation.CUT, status) }
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
        if (file.uri.scheme == "file") {
            _uiState.update { it.copy(fileTag = if (tagsEnabled) fileTags[file.uri.toString()] else null) }
            load(file.uri)
            return
        }

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
