package com.rec.gpiv.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.rec.gpiv.player.VideoPlayer
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class ScreenshotMethod {
    FRAMEBUFFER,
    MPV
}

/** Exporta screenshots e vídeos cortados para o armazenamento SAF. */
class MediaExportManager(
    private val contentResolver: ContentResolver,
    private val cacheDir: File,
    private val player: VideoPlayer,
    private val scope: CoroutineScope,
    private val currentTreeUri: () -> Uri?,
    private val currentFileName: () -> String?
) {

    private var screenshotMethod = ScreenshotMethod.FRAMEBUFFER

    fun screenshot() {

        println(
            "PlayerViewModel: screenshot()"
        )

        val cacheDir =
            cacheDir

        val treeUri =
            currentTreeUri()

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

        scope.launch {

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

                val resolver = contentResolver

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
                    currentFileName()
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

    suspend fun copyCutVideoToTree(
        cutFile: File,
        treeUri: Uri
    ) {

        withContext(Dispatchers.IO) {

            try {

                val resolver = contentResolver

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
                    currentFileName()
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


}
