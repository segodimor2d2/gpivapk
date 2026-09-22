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

                println(
                    "PlayerViewModel: " +
                        "gpivlogs não encontrado"
                )

                lisTags.clear()

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

            var csvUri: Uri? = null

            resolver.query(
                gpivlogsChildrenUri,
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

                    if (name == "gpivtags.csv") {

                        val documentId =
                            cursor.getString(idIndex)

                        csvUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (csvUri == null) {

                println(
                    "PlayerViewModel: " +
                        "gpivlogs/gpivtags.csv não encontrado"
                )

                lisTags.clear()

                return
            }

            /*
             * ----------------------------------------------------
             * LER TAGS
             * ----------------------------------------------------
             */

            lisTags.clear()
            fileTags.clear()

            resolver.openInputStream(
                csvUri!!
            )?.bufferedReader(
                Charsets.UTF_8
            )?.useLines { lines ->

                lines.forEach { line ->

                    val parts =
                        line.split(",")

                    if (parts.size >= 2) {

                        val tag =
                            parts[1].trim()

                        if (isValidTag(tag)) {

                            lisTags.add(tag)

                            val fileUri =
                                parts[0].trim()

                            fileTags[fileUri] = tag

                        } else {

                            println(
                                "PlayerViewModel: " +
                                    "TAG INVÁLIDA NO CSV -> $tag"
                            )
                        }
                    }
                }
            }

            /*
             * ----------------------------------------------------
             * REMOVER DUPLICADAS
             * ----------------------------------------------------
             */

            val uniqueTags =
                lisTags.distinct()

            lisTags.clear()

            lisTags.addAll(
                uniqueTags
            )

            /*
             * ----------------------------------------------------
             * DEBUG
             * ----------------------------------------------------
             */

            println(
                "PlayerViewModel: lisTags = $lisTags"
            )

            println(
                "PlayerViewModel: " +
                    "quantidade de tags = ${lisTags.size}"
            )

        } catch (e: Exception) {

            Log.e(
                "PlayerViewModel",
                "Erro ao carregar lisTags",
                e
            )
        }
    }

    fun makeTagFolders() {

        val treeUri =
            fileList.getCurrentTreeUri()

        if (treeUri == null) {

            println(
                "PlayerViewModel: " +
                    "nenhuma pasta selecionada"
            )

            return
        }

        makeTagFolders(treeUri)
    }

    private fun makeTagFolders(
        treeUri: Uri
    ) {

        val resolver =
            getApplication<Application>()
                .contentResolver

        try {

            /*
             * ----------------------------------------------------
             * DOCUMENTO RAIZ DA PASTA
             * ----------------------------------------------------
             */

            val treeDocumentId =
                DocumentsContract.getTreeDocumentId(
                    treeUri
                )

            val rootUri =
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    treeDocumentId
                )

            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    treeDocumentId
                )

            /*
             * ----------------------------------------------------
             * PERCORRER LIS TAGS
             * ----------------------------------------------------
             */

            for (tag in lisTags) {

                var folderExists = false

                /*
                 * ------------------------------------------------
                 * VERIFICAR SE A PASTA JÁ EXISTE
                 * ------------------------------------------------
                 */

                resolver.query(
                    childrenUri,
                    arrayOf(
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->

                    val nameIndex =
                        cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME
                        )

                    val mimeIndex =
                        cursor.getColumnIndex(
                            DocumentsContract.Document.COLUMN_MIME_TYPE
                        )

                    while (cursor.moveToNext()) {

                        val name =
                            cursor.getString(nameIndex)

                        val mime =
                            cursor.getString(mimeIndex)

                        if (
                            name == tag &&
                            mime ==
                                DocumentsContract.Document.MIME_TYPE_DIR
                        ) {

                            folderExists = true

                            break
                        }
                    }
                }

                /*
                 * ------------------------------------------------
                 * PASTA JÁ EXISTE
                 * ------------------------------------------------
                 */

                if (folderExists) {

                    println(
                        "PlayerViewModel: " +
                            "pasta já existe = $tag"
                    )

                    continue
                }

                /*
                 * ------------------------------------------------
                 * CRIAR PASTA
                 * ------------------------------------------------
                 */

                val createdUri =
                    DocumentsContract.createDocument(
                        resolver,
                        rootUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        tag
                    )

                if (createdUri != null) {

                    println(
                        "PlayerViewModel: " +
                            "pasta criada = $tag"
                    )

                } else {

                    println(
                        "PlayerViewModel: " +
                            "ERRO ao criar pasta = $tag"
                    )
                }
            }

        } catch (e: Exception) {

            Log.e(
                "PlayerViewModel",
                "Erro ao criar pastas das tags",
                e
            )
        }
    }

    private fun isValidTag(tag: String): Boolean {
        return tag.matches(Regex("[a-z0-9]+"))
    }

    private fun saveFileTag(
        tag: String
    ) {

        if (!isValidTag(tag)) {
            println(
                "PlayerViewModel: TAG INVÁLIDA -> $tag"
            )
            return
        }

        val currentFile =
            fileList.current()
                ?: return

        val treeUri =
            fileList.getCurrentTreeUri()
                ?: return

        val index =
            fileList.currentIndex()

        val fileUri =
            currentFile.uri

        println(
            "PlayerViewModel: arquivo atual = $fileUri"
        )

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

                println(
                    "PlayerViewModel: " +
                        "gpivlogs não encontrado"
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

            var csvUri: Uri? = null

            resolver.query(
                gpivlogsChildrenUri,
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

                    if (name == "gpivtags.csv") {

                        val documentId =
                            cursor.getString(idIndex)

                        csvUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (csvUri == null) {

                println(
                    "PlayerViewModel: " +
                        "gpivlogs/gpivtags.csv não encontrado"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * REMOVER TAG
             * ----------------------------------------------------
             */

            if (tag.isBlank()) {

                val lines =
                    resolver.openInputStream(
                        csvUri!!
                    )?.bufferedReader(
                        Charsets.UTF_8
                    )?.use { reader ->
                        reader.readLines()
                    } ?: emptyList()

                val updatedLines =
                    lines.filter { line ->

                        val parts =
                            line.split(",")

                        parts.size < 3 ||
                            parts[0] != fileUri.toString()
                    }

                resolver.openOutputStream(
                    csvUri!!,
                    "wt"
                )?.use { output ->

                    output.write(
                        (
                            updatedLines.joinToString("\n") +
                                if (updatedLines.isNotEmpty()) "\n" else ""
                        ).toByteArray(
                            Charsets.UTF_8
                        )
                    )
                }

                println(
                    "PlayerViewModel: tag removida = $fileUri"
                )

                _uiState.value =
                    _uiState.value.copy(
                        fileTag = null
                    )

                return
            }

            /*
             * ----------------------------------------------------
             * SALVAR TAG
             * ----------------------------------------------------
             */

            val newLine =
                "${fileUri},${tag},${index}"

            val lines =
                resolver.openInputStream(
                    csvUri!!
                )?.bufferedReader(
                    Charsets.UTF_8
                )?.use { reader ->
                    reader.readLines()
                } ?: emptyList()

            val updatedLines =
                mutableListOf<String>()

            var found =
                false

            for (oldLine in lines) {

                val parts =
                    oldLine.split(",")

                if (
                    parts.size >= 3 &&
                    parts[0] == fileUri.toString()
                ) {
                    updatedLines.add(newLine)
                    found = true
                } else {
                    updatedLines.add(oldLine)
                }
            }

            if (!found) {
                updatedLines.add(newLine)
            }

            resolver.openOutputStream(
                csvUri!!,
                "wt"
            )?.use { output ->

                output.write(
                    (
                        updatedLines.joinToString("\n") +
                            "\n"
                    ).toByteArray(
                        Charsets.UTF_8
                    )
                )
            }

            println(
                "PlayerViewModel: tag salva = $newLine"
            )

            _uiState.value =
                _uiState.value.copy(
                    fileTag = tag
                )

        } catch (e: Exception) {

            Log.e(
                "PlayerViewModel",
                "Erro ao salvar tag",
                e
            )
        }
    }

    fun testSaveFileTag(
        tag: String
    ) {
        saveFileTag(tag)
    }

    private fun readFileTag(
        treeUri: Uri,
        fileUri: Uri
    ): String? {

        return fileTags[fileUri.toString()]
    }

    private fun getUniqueDestinationName(
        resolver: ContentResolver,
        destinationChildrenUri: Uri,
        fileName: String
    ): String {

        val existingNames =
            mutableSetOf<String>()

        resolver.query(
            destinationChildrenUri,
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

                existingNames.add(
                    cursor.getString(nameIndex)
                )
            }
        }

        if (!existingNames.contains(fileName)) {
            return fileName
        }

        val dotIndex =
            fileName.lastIndexOf('.')

        val baseName: String
        val extension: String

        if (
            dotIndex > 0 &&
            dotIndex < fileName.length - 1
        ) {

            baseName =
                fileName.substring(
                    0,
                    dotIndex
                )

            extension =
                fileName.substring(
                    dotIndex
                )

        } else {

            baseName = fileName
            extension = ""
        }

        var number = 1

        while (true) {

            val candidate =
                String.format(
                    "%s_%03d%s",
                    baseName,
                    number,
                    extension
                )

            if (!existingNames.contains(candidate)) {
                return candidate
            }

            number++
        }
    }

    private fun getNextGpivTagsLogName(
        resolver: ContentResolver,
        gpivlogsChildrenUri: Uri
    ): String {

        val existingNames =
            mutableSetOf<String>()

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

                if (nameIndex >= 0) {

                    existingNames.add(
                        cursor.getString(nameIndex)
                    )
                }
            }
        }

        for (index in 1..999) {

            val name =
                "gpivtagslog_" +
                    String.format("%03d", index) +
                    ".txt"

            if (!existingNames.contains(name)) {
                return name
            }
        }

        throw IllegalStateException(
            "Não foi possível encontrar nome disponível para gpivtagslog"
        )
    }

    private fun getNextGpivTagsBackupName(
        resolver: ContentResolver,
        childrenUri: Uri
    ): String {

        val existingNames =
            mutableSetOf<String>()

        resolver.query(
            childrenUri,
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

                existingNames.add(
                    cursor.getString(nameIndex)
                )
            }
        }

        var number = 1

        while (true) {

            val candidate =
                String.format(
                    "gpivtagsbkp_%03d.csv",
                    number
                )

            if (!existingNames.contains(candidate)) {
                return candidate
            }

            number++
        }
    }

    fun testMoveTags() {

        val treeUri =
            fileList.getCurrentTreeUri()

        if (treeUri == null) {

            println(
                "PlayerViewModel: " +
                    "nenhuma pasta selecionada"
            )

            return
        }

        val resolver =
            getApplication<Application>()
                .contentResolver

        try {

            val treeDocumentId =
                DocumentsContract.getTreeDocumentId(
                    treeUri
                )

            val rootUri =
                DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    treeDocumentId
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

                println(
                    "PlayerViewModel: " +
                        "gpivlogs não encontrado"
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

            var csvUri: Uri? = null

            resolver.query(
                gpivlogsChildrenUri,
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

                    if (name == "gpivtags.csv") {

                        val documentId =
                            cursor.getString(idIndex)

                        csvUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (csvUri == null) {

                println(
                    "PlayerViewModel: " +
                        "gpivlogs/gpivtags.csv não encontrado"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * LOCALIZAR PASTAS DAS TAGS
             * ----------------------------------------------------
             */

            val tagFolders =
                mutableMapOf<String, Uri>()

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

                val mimeIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document.COLUMN_MIME_TYPE
                    )

                while (cursor.moveToNext()) {

                    val name =
                        cursor.getString(nameIndex)

                    val mime =
                        cursor.getString(mimeIndex)

                    if (
                        mime ==
                            DocumentsContract.Document.MIME_TYPE_DIR
                    ) {

                        val folderUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    cursor.getString(idIndex)
                                )

                        tagFolders[name] =
                            folderUri
                    }
                }
            }

            /*
             * ----------------------------------------------------
             * PROCESSAR gpivtags.csv
             * ----------------------------------------------------
             */

            val pendingLines =
                mutableListOf<String>()

            var processedCount = 0
            var failedCount = 0
            val logLines = mutableListOf<String>()

            resolver.openInputStream(
                csvUri!!
            )?.bufferedReader(
                Charsets.UTF_8
            )?.useLines { lines ->

                lines.forEach { line ->

                    val parts =
                        line.split(",")

                    if (parts.size < 3) {

                        println(
                            "PlayerViewModel: " +
                                "linha inválida = $line"
                        )

                        logLines.add(
                            "ERRO | " +
                                "Linha: $line | " +
                                "Motivo: linha CSV inválida"
                        )

                        pendingLines.add(line)
                        failedCount++

                        return@forEach
                    }

                    val fileUri =
                        Uri.parse(parts[0])

                    val tag =
                        parts[1].trim()

                    val destinationUri =
                        tagFolders[tag]

                    if (destinationUri == null) {

                        println(
                            "PlayerViewModel: " +
                                "ERRO -> pasta não encontrada " +
                                "para tag=$tag"
                        )

                        logLines.add(
                            "ERRO | " +
                                "Tag: $tag | " +
                                "Motivo: pasta não encontrada"
                        )

                        pendingLines.add(line)
                        failedCount++

                        return@forEach
                    }

                    /*
                     * ------------------------------------------------
                     * OBTER NOME DO ARQUIVO
                     * ------------------------------------------------
                     */

                    val fileName =
                        getFileName(fileUri)

                    if (
                        fileName.isBlank() ||
                        fileName == "Nenhum arquivo"
                    ) {

                        println(
                            "PlayerViewModel: " +
                                "ERRO -> nome vazio para $fileUri"
                        )

                        logLines.add(
                            "ERRO | " +
                                "URI: $fileUri | " +
                                "Motivo: nome do arquivo vazio ou inacessível"
                        )

                        pendingLines.add(line)
                        failedCount++

                        return@forEach
                    }

                    /*
                     * ------------------------------------------------
                     * VERIFICAR COLISÃO
                     * ------------------------------------------------
                     */

                    val destinationChildrenUri =
                        DocumentsContract
                            .buildChildDocumentsUriUsingTree(
                                treeUri,
                                DocumentsContract
                                    .getDocumentId(destinationUri)
                            )

                    var destinationExists =
                        false

                    resolver.query(
                        destinationChildrenUri,
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

                            val existingName =
                                cursor.getString(nameIndex)

                            if (
                                existingName == fileName
                            ) {

                                destinationExists =
                                    true

                                break
                            }
                        }
                    }

                    var destinationFileName =
                        fileName

                    if (destinationExists) {

                        destinationFileName =
                            getUniqueDestinationName(
                                resolver,
                                destinationChildrenUri,
                                fileName
                            )

                        println(
                            "PlayerViewModel: " +
                                "COLISÃO -> $fileName " +
                                "novo nome = $destinationFileName"
                        )
                    }

                    /*
                     * ------------------------------------------------
                     * MOVER / COPIAR ARQUIVO
                     * ------------------------------------------------
                     */

                    println(
                        "PlayerViewModel: " +
                            "MV -> $fileName para $tag"
                    )

                    if (!destinationExists) {

                        /*
                         * ------------------------------------------------
                         * SEM COLISÃO
                         * ------------------------------------------------
                         */

                        val movedUri =
                            DocumentsContract.moveDocument(
                                resolver,
                                fileUri,
                                rootUri,
                                destinationUri
                            )

                        if (movedUri != null) {

                            println(
                                "PlayerViewModel: " +
                                    "MV SUCESSO -> $fileName -> $tag"
                            )

                            logLines.add(
                                "SUCESSO | " +
                                    "Arquivo: $fileName | " +
                                    "Tag: $tag"
                            )

                            processedCount++

                        } else {

                            println(
                                "PlayerViewModel: " +
                                    "MV ERRO -> $fileName -> $tag"
                            )

                            logLines.add(
                                "ERRO | " +
                                    "Arquivo: $fileName | " +
                                    "Tag: $tag | " +
                                    "Motivo: falha ao mover arquivo"
                            )

                            pendingLines.add(line)
                            failedCount++

                        return@forEach
                        }

                    } else {

                        /*
                         * ------------------------------------------------
                         * COM COLISÃO
                         *
                         * COPIAR -> CONFIRMAR -> APAGAR ORIGINAL
                         * ------------------------------------------------
                         */

                        println(
                            "PlayerViewModel: " +
                                "COLISÃO -> copiando como " +
                                destinationFileName
                        )

                        val mimeType =
                            resolver.getType(fileUri)
                                ?: "application/octet-stream"

                        val newFileUri =
                            DocumentsContract.createDocument(
                                resolver,
                                destinationUri,
                                mimeType,
                                destinationFileName
                            )

                        if (newFileUri == null) {

                            println(
                                "PlayerViewModel: " +
                                    "ERRO -> não foi possível criar " +
                                    destinationFileName
                            )

                            logLines.add(
                                "ERRO | " +
                                    "Arquivo: $fileName | " +
                                    "Destino: $destinationFileName | " +
                                    "Motivo: não foi possível criar arquivo de destino"
                            )

                            pendingLines.add(line)
                            failedCount++

                        return@forEach

                            return@forEach
                        }

                        var bytesCopied = 0L

                        try {

                            resolver.openInputStream(fileUri).use { input ->

                                resolver.openOutputStream(newFileUri).use { output ->

                                    if (input == null || output == null) {

                                        throw IllegalStateException(
                                            "Não foi possível abrir origem ou destino"
                                        )
                                    }

                                    val buffer =
                                        ByteArray(1024 * 1024)

                                    while (true) {

                                        val count =
                                            input.read(buffer)

                                        if (count == -1) {
                                            break
                                        }

                                        output.write(
                                            buffer,
                                            0,
                                            count
                                        )

                                        bytesCopied += count
                                    }

                                    output.flush()
                                }
                            }

                            println(
                                "PlayerViewModel: " +
                                    "CÓPIA SUCESSO -> " +
                                    destinationFileName +
                                    " bytes=" +
                                    bytesCopied
                            )

                            /*
                             * ------------------------------------------------
                             * APAGAR ORIGINAL SOMENTE APÓS A CÓPIA
                             * ------------------------------------------------
                             */

                            val deleted =
                                DocumentsContract.deleteDocument(
                                    resolver,
                                    fileUri
                                )

                            if (deleted) {

                                println(
                                    "PlayerViewModel: " +
                                        "MV SUCESSO -> " +
                                        fileName +
                                        " -> " +
                                        destinationFileName
                                )

                                logLines.add(
                                    "SUCESSO | " +
                                        "Arquivo: $fileName | " +
                                        "Destino: $destinationFileName | " +
                                        "Tag: $tag"
                                )

                                processedCount++

                            } else {

                                println(
                                    "PlayerViewModel: " +
                                        "ERRO -> cópia feita, " +
                                        "mas não foi possível apagar " +
                                        fileName
                                )

                                logLines.add(
                                    "ERRO | " +
                                        "Arquivo: $fileName | " +
                                        "Motivo: cópia realizada, mas não foi possível apagar arquivo original"
                                )

                                pendingLines.add(line)
                                failedCount++

                        return@forEach
                            }

                        } catch (e: Exception) {

                            /*
                             * A cópia falhou.
                             *
                             * NÃO apagamos o original.
                             */

                            println(
                                "PlayerViewModel: " +
                                    "ERRO NA CÓPIA -> " +
                                    destinationFileName
                            )

                            Log.e(
                                "PlayerViewModel",
                                "Erro ao copiar arquivo por colisão",
                                e
                            )

                            logLines.add(
                                "ERRO | " +
                                    "Arquivo: $fileName | " +
                                    "Destino: $destinationFileName | " +
                                    "Motivo: erro ao copiar arquivo por colisão | " +
                                    "Erro: ${e.message}"
                            )

                            pendingLines.add(line)
                            failedCount++

                        return@forEach
                        }
                    }
                }
            }

            /*
             * ------------------------------------------------
             * RESULTADO DO MV
             * ------------------------------------------------
             */


            println(
                "PlayerViewModel: " +
                    "MOVIMENTOS PROCESSADOS = $processedCount"
            )

            println(
                "PlayerViewModel: " +
                    "MOVIMENTOS PENDENTES = $failedCount"
            )

            /*
             * ------------------------------------------------
             * BACKUP DO gpivtags.csv
             * ------------------------------------------------
             */

            val backupName =
                getNextGpivTagsBackupName(
                    resolver,
                    gpivlogsChildrenUri
                )

            println(
                "PlayerViewModel: " +
                    "INICIANDO CRIAÇÃO DO NOME DO LOG"
            )

            val logName =
                getNextGpivTagsLogName(
                    resolver,
                    gpivlogsChildrenUri
                )

            println(
                "PlayerViewModel: " +
                    "LOG -> $logName"
            )

            val logUri =
                DocumentsContract.createDocument(
                    resolver,
                    gpivlogsUri,
                    "text/plain",
                    logName
                )

            if (logUri != null) {

                println(
                    "PlayerViewModel: " +
                        "LOG CRIADO -> $logName"
                )

                try {

                    resolver.openOutputStream(logUri).use { output ->

                        if (output == null) {
                            throw IllegalStateException(
                                "Não foi possível abrir arquivo de log"
                            )
                        }

                        output.write(
                            "GPIV TAGS - LOG\n".toByteArray(Charsets.UTF_8)
                        )

                        output.write(
                            "==============================\n"
                                .toByteArray(Charsets.UTF_8)
                        )

                        logLines.forEach { logLine ->

                            output.write(
                                "$logLine\n".toByteArray(Charsets.UTF_8)
                            )
                        }

                        output.write(
                            "\n".toByteArray(Charsets.UTF_8)
                        )

                        output.write(
                            "==============================\n"
                                .toByteArray(Charsets.UTF_8)
                        )

                        output.write(
                            "PROCESSADOS: $processedCount\n"
                                .toByteArray(Charsets.UTF_8)
                        )

                        output.write(
                            "PENDENTES: $failedCount\n"
                                .toByteArray(Charsets.UTF_8)
                        )

                        output.write(
                            "==============================\n"
                                .toByteArray(Charsets.UTF_8)
                        )

                        output.flush()
                    }

                    println(
                        "PlayerViewModel: " +
                            "LOG GRAVADO -> $logName"
                    )

                } catch (e: Exception) {

                    Log.e(
                        "PlayerViewModel",
                        "Erro ao gravar arquivo de log",
                        e
                    )
                }

            } else {

                println(
                    "PlayerViewModel: " +
                        "ERRO -> não foi possível criar " +
                        "log $logName"
                )
            }

            println(
                "PlayerViewModel: " +
                    "BACKUP -> gpivtags.csv -> $backupName"
            )

            val backupUri =
                DocumentsContract.renameDocument(
                    resolver,
                    csvUri!!,
                    backupName
                )

            if (backupUri != null) {

                println(
                    "PlayerViewModel: " +
                        "BACKUP SUCESSO -> $backupName"
                )

                /*
                 * ------------------------------------------------
                 * CRIAR NOVO gpivtags.csv
                 * ------------------------------------------------
                 */

                val newCsvUri =
                    DocumentsContract.createDocument(
                        resolver,
                        gpivlogsUri,
                        "text/csv",
                        "gpivtags.csv"
                    )

                if (newCsvUri != null) {

                    println(
                        "PlayerViewModel: " +
                            "NOVO CSV CRIADO -> gpivtags.csv"
                    )
                    try {

                        resolver.openOutputStream(newCsvUri).use { output ->

                            if (output == null) {
                                throw IllegalStateException(
                                    "Não foi possível abrir novo gpivtags.csv"
                                )
                            }

                            pendingLines.forEach { line ->

                                output.write(
                                    line.toByteArray(Charsets.UTF_8)
                                )

                                output.write(
                                    "\n".toByteArray(Charsets.UTF_8)
                                )
                            }

                            output.flush()
                        }

                        println(
                            "PlayerViewModel: " +
                                "PENDENTES GRAVADOS = " +
                                pendingLines.size
                        )

                    } catch (e: Exception) {

                        Log.e(
                            "PlayerViewModel",
                            "Erro ao gravar pendentes no novo gpivtags.csv",
                            e
                        )
                    }

                } else {

                    println(
                        "PlayerViewModel: " +
                            "ERRO -> não foi possível criar " +
                            "novo gpivtags.csv"
                    )
                }

            } else {

                println(
                    "PlayerViewModel: " +
                        "ERRO -> não foi possível renomear " +
                        "gpivtags.csv"
                )
            }

        } catch (e: Exception) {

            Log.e(
                "PlayerViewModel",
                "Erro ao mover arquivos por tag",
                e
            )
        }
    }

    private fun getFileName(uri: Uri): String {
        val resolver = getApplication<Application>().contentResolver

        return try {
            resolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex =
                    cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)

                if (nameIndex >= 0 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                        ?.takeIf { it.isNotBlank() }
                        ?: "Nenhum arquivo"
                } else {
                    "Nenhum arquivo"
                }
            } ?: "Nenhum arquivo"
        } catch (e: Exception) {
            Log.e(
                "PlayerViewModel",
                "Arquivo não encontrado ou inacessível: $uri"
            )
            "Nenhum arquivo"
        }
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
                readFileTag(
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
        val state = _uiState.value

        mpvNative.resetVideoAdjustments()
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
