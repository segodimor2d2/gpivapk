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

    private var volumeBeforeMute = 100.0
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

        loadLisTags(treeUri)

        val currentFile =
            fileList.current()
                ?: return

        val fileTag =
            readFileTag(
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
                ?: getFileName(uri)

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
            ensureGpivTagsFile(uri)
            loadLisTags(uri)
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
                            readFileTag(
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

    private fun ensureGpivTagsFile(
        treeUri: Uri
    ) {

        val resolver =
            getApplication<Application>()
                .contentResolver

        try {

            val treeDocumentId =
                DocumentsContract.getTreeDocumentId(
                    treeUri
                )

            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    treeDocumentId
                )

            /*
             * ----------------------------------------------------
             * LOCALIZAR OU CRIAR gpivlogs
             * ----------------------------------------------------
             */

            var gpivlogsUri: Uri? = null

            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->

                val idIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID
                    )

                val nameIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )

                while (cursor.moveToNext()) {

                    val name =
                        cursor.getString(nameIndex)

                    if (name == "gpivlogs") {

                        val documentId =
                            cursor.getString(idIndex)

                        gpivlogsUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (gpivlogsUri == null) {

                val rootUri =
                    DocumentsContract
                        .buildDocumentUriUsingTree(
                            treeUri,
                            treeDocumentId
                        )

                gpivlogsUri =
                    DocumentsContract.createDocument(
                        resolver,
                        rootUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        "gpivlogs"
                    )

                println(
                    "PlayerViewModel: " +
                        "gpivlogs criado = $gpivlogsUri"
                )
            }

            if (gpivlogsUri == null) {

                println(
                    "PlayerViewModel: " +
                        "ERRO -> não foi possível criar gpivlogs"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * LOCALIZAR gpivtags.csv DENTRO DE gpivlogs
             * ----------------------------------------------------
             */

            val gpivlogsChildrenUri =
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        treeUri,
                        DocumentsContract.getDocumentId(
                            gpivlogsUri
                        )
                    )

            var csvExists = false

            resolver.query(
                gpivlogsChildrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->

                val nameIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )

                while (cursor.moveToNext()) {

                    val name =
                        cursor.getString(nameIndex)

                    if (name == "gpivtags.csv") {

                        csvExists = true
                        break
                    }
                }
            }

            if (csvExists) {

                println(
                    "PlayerViewModel: " +
                        "gpivlogs/gpivtags.csv já existe"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * CRIAR gpivtags.csv DENTRO DE gpivlogs
             * ----------------------------------------------------
             */

            val createdUri =
                DocumentsContract.createDocument(
                    resolver,
                    gpivlogsUri,
                    "text/csv",
                    "gpivtags.csv"
                )

            println(
                "PlayerViewModel: " +
                    "gpivlogs/gpivtags.csv criado = $createdUri"
            )

        } catch (e: Exception) {

            Log.e(
                "PlayerViewModel",
                "Erro ao criar gpivlogs/gpivtags.csv",
                e
            )
        }
    }

    private fun loadLisTags(
        treeUri: Uri
    ) {

        val resolver =
            getApplication<Application>()
                .contentResolver

        try {

            val treeDocumentId =
                DocumentsContract.getTreeDocumentId(
                    treeUri
                )

            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    treeDocumentId
                )

            /*
             * ----------------------------------------------------
             * LOCALIZAR gpivlogs
             * ----------------------------------------------------
             */

            var gpivlogsUri: Uri? = null

            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->

                val idIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID
                    )

                val nameIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME
                    )

                while (cursor.moveToNext()) {

                    val name =
                        cursor.getString(nameIndex)

                    if (name == "gpivlogs") {
