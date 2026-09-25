package com.rec.gpiv.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.rec.gpiv.player.FileItem

/** Responsável pelos metadados de tags e operações SAF relacionadas. */
class TagFileManager(
    private val contentResolver: ContentResolver,
    private val currentTreeUri: () -> Uri?,
    private val currentFile: () -> FileItem?,
    private val currentIndex: () -> Int,
    private val tags: MutableList<String>,
    private val fileTags: MutableMap<String, String>,
    private val onFileTagChanged: (String?) -> Unit
) {

    fun ensureGpivTagsFile(
        treeUri: Uri
    ) {

        val resolver =
            contentResolver

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
             * LOCALIZAR OU CRIAR gpvlogs
             * ----------------------------------------------------
             */

            var gpvlogsUri: Uri? = null

            contentResolver.query(
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

                    if (name == "gpvlogs") {

                        val documentId =
                            cursor.getString(idIndex)

                        gpvlogsUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (gpvlogsUri == null) {

                val rootUri =
                    DocumentsContract
                        .buildDocumentUriUsingTree(
                            treeUri,
                            treeDocumentId
                        )

                gpvlogsUri =
                    DocumentsContract.createDocument(
                        resolver,
                        rootUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        "gpvlogs"
                    )

                println(
                    "PlayerViewModel: " +
                        "gpvlogs criado = $gpvlogsUri"
                )
            }

            if (gpvlogsUri == null) {

                println(
                    "PlayerViewModel: " +
                        "ERRO -> não foi possível criar gpvlogs"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * LOCALIZAR gpivtags.csv DENTRO DE gpvlogs
             * ----------------------------------------------------
             */

            val gpvlogsChildrenUri =
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        treeUri,
                        DocumentsContract.getDocumentId(
                            gpvlogsUri
                        )
                    )

            var csvExists = false

            resolver.query(
                gpvlogsChildrenUri,
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
                        "gpvlogs/gpivtags.csv já existe"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * CRIAR gpivtags.csv DENTRO DE gpvlogs
             * ----------------------------------------------------
             */

            val createdUri =
                DocumentsContract.createDocument(
                    resolver,
                    gpvlogsUri,
                    "text/csv",
                    "gpivtags.csv"
                )

            println(
                "PlayerViewModel: " +
                    "gpvlogs/gpivtags.csv criado = $createdUri"
            )

        } catch (e: Exception) {

            Log.e(
                "PlayerViewModel",
                "Erro ao criar gpvlogs/gpivtags.csv",
                e
            )
        }
    }

    fun loadLisTags(
        treeUri: Uri
    ) {

        val resolver =
            contentResolver

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
             * LOCALIZAR gpvlogs
             * ----------------------------------------------------
             */

            var gpvlogsUri: Uri? = null

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

                    if (name == "gpvlogs") {

                        val documentId =
                            cursor.getString(idIndex)

                        gpvlogsUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (gpvlogsUri == null) {

                println(
                    "PlayerViewModel: " +
                        "gpvlogs não encontrado"
                )

                tags.clear()

                return
            }

            /*
             * ----------------------------------------------------
             * LOCALIZAR gpivtags.csv DENTRO DE gpvlogs
             * ----------------------------------------------------
             */

            val gpvlogsChildrenUri =
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        treeUri,
                        DocumentsContract.getDocumentId(
                            gpvlogsUri
                        )
                    )

            var csvUri: Uri? = null

            resolver.query(
                gpvlogsChildrenUri,
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
                        "gpvlogs/gpivtags.csv não encontrado"
                )

                tags.clear()

                return
            }

            /*
             * ----------------------------------------------------
             * LER TAGS
             * ----------------------------------------------------
             */

            tags.clear()
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

                            tags.add(tag)

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
                tags.distinct()

            tags.clear()

            tags.addAll(
                uniqueTags
            )

            /*
             * ----------------------------------------------------
             * DEBUG
             * ----------------------------------------------------
             */

            println(
                "TagFileManager: tags = $tags"
            )

            println(
                "PlayerViewModel: " +
                    "quantidade de tags = ${tags.size}"
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
            currentTreeUri()

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
            contentResolver

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

            for (tag in tags) {

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
        inputTag: String
    ) {

        // Dois espaços são o comando para remover a tag do arquivo atual.
        // Internamente usamos uma string vazia para acionar o fluxo de remoção.
        val tag = if (inputTag == "  ") "" else inputTag

        if (tag.isNotEmpty() && !isValidTag(tag)) {
            println(
                "PlayerViewModel: TAG INVÁLIDA -> $tag"
            )
            return
        }

        val currentFile =
            currentFile()
                ?: return

        val treeUri =
            currentTreeUri()
                ?: return

        val index =
            currentIndex()

        val fileUri =
            currentFile.uri

        println(
            "PlayerViewModel: arquivo atual = $fileUri"
        )

        val resolver =
            contentResolver

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
             * LOCALIZAR gpvlogs
             * ----------------------------------------------------
             */

            var gpvlogsUri: Uri? = null

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

                    if (name == "gpvlogs") {

                        val documentId =
                            cursor.getString(idIndex)

                        gpvlogsUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (gpvlogsUri == null) {

                println(
                    "PlayerViewModel: " +
                        "gpvlogs não encontrado"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * LOCALIZAR gpivtags.csv DENTRO DE gpvlogs
             * ----------------------------------------------------
             */

            val gpvlogsChildrenUri =
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        treeUri,
                        DocumentsContract.getDocumentId(
                            gpvlogsUri
                        )
                    )

            var csvUri: Uri? = null

            resolver.query(
                gpvlogsChildrenUri,
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
                        "gpvlogs/gpivtags.csv não encontrado"
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

                fileTags.remove(fileUri.toString())
                onFileTagChanged(null)

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

            // Mantém a UI sincronizada sem depender de recarregar o CSV.
            fileTags[fileUri.toString()] = tag
            if (!tags.contains(tag)) {
                tags.add(tag)
            }
            onFileTagChanged(tag)

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

    fun readFileTag(
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
        gpvlogsChildrenUri: Uri
    ): String {

        val existingNames =
            mutableSetOf<String>()

        resolver.query(
            gpvlogsChildrenUri,
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
            currentTreeUri()

        if (treeUri == null) {

            println(
                "PlayerViewModel: " +
                    "nenhuma pasta selecionada"
            )

            return
        }

        val resolver =
            contentResolver

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
             * LOCALIZAR gpvlogs
             * ----------------------------------------------------
             */

            var gpvlogsUri: Uri? = null

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

                    if (name == "gpvlogs") {

                        val documentId =
                            cursor.getString(idIndex)

                        gpvlogsUri =
                            DocumentsContract
                                .buildDocumentUriUsingTree(
                                    treeUri,
                                    documentId
                                )

                        break
                    }
                }
            }

            if (gpvlogsUri == null) {

                println(
                    "PlayerViewModel: " +
                        "gpvlogs não encontrado"
                )

                return
            }

            /*
             * ----------------------------------------------------
             * LOCALIZAR gpivtags.csv DENTRO DE gpvlogs
             * ----------------------------------------------------
             */

            val gpvlogsChildrenUri =
                DocumentsContract
                    .buildChildDocumentsUriUsingTree(
                        treeUri,
                        DocumentsContract.getDocumentId(
                            gpvlogsUri
                        )
                    )

            var csvUri: Uri? = null

            resolver.query(
                gpvlogsChildrenUri,
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
                        "gpvlogs/gpivtags.csv não encontrado"
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
                    gpvlogsChildrenUri
                )

            println(
                "PlayerViewModel: " +
                    "INICIANDO CRIAÇÃO DO NOME DO LOG"
            )

            val logName =
                getNextGpivTagsLogName(
                    resolver,
                    gpvlogsChildrenUri
                )

            println(
                "PlayerViewModel: " +
                    "LOG -> $logName"
            )

            val logUri =
                DocumentsContract.createDocument(
                    resolver,
                    gpvlogsUri,
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
                        gpvlogsUri,
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

    fun getFileName(uri: Uri): String {

        return try {
            contentResolver.query(
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



}
