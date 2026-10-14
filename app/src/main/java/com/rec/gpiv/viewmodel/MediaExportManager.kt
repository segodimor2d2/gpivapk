package com.rec.gpiv.viewmodel

import com.rec.gpiv.model.OperationStatus
import kotlinx.coroutines.CancellationException
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
    private val currentFileName: () -> String?,
    private val currentFileUri: () -> Uri?,
    private val onScreenshotStatus: (Boolean, OperationStatus) -> Unit = { _, _ -> }
) {

    private var screenshotMethod = ScreenshotMethod.FRAMEBUFFER

    private var screenshotRunning = false

    fun screenshot() {
        if (screenshotRunning) return
        val method = screenshotMethod
        val mpv = method == ScreenshotMethod.MPV
        val treeUri = currentTreeUri()
        val fileUri = currentFileUri()
        val localDirectory = if (fileUri?.scheme == "file") fileUri.path?.let { File(it).parentFile } else null
        val originalName = currentFileName() ?: "screenshot"
        onScreenshotStatus(mpv, OperationStatus.RUNNING)
        if (treeUri == null && localDirectory == null) {
            onScreenshotStatus(mpv, OperationStatus.ERROR)
            return
        }
        screenshotRunning = true
        scope.launch {
            try {
                val existingFiles = withContext(Dispatchers.IO) {
                    cacheDir.listFiles()?.filter { it.name.startsWith("mpv-shot") && it.name.endsWith(".jpg") }
                        ?.map { it.name }?.toSet().orEmpty()
                }
                when (method) {
                    ScreenshotMethod.FRAMEBUFFER -> player.screenshot()
                    ScreenshotMethod.MPV -> player.screenshotMpv()
                }
                val success = withContext(Dispatchers.IO) {
                    val screenshotFile = waitForNewScreenshot(cacheDir, existingFiles)
                    if (screenshotFile == null) false
                    else if (localDirectory != null) copyScreenshotToDirectory(screenshotFile, localDirectory, originalName)
                    else copyScreenshotToTree(screenshotFile, requireNotNull(treeUri), originalName)
                }
                onScreenshotStatus(mpv, if (success) OperationStatus.SUCCESS else OperationStatus.ERROR)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("PlayerViewModel: erro no screenshot: ${e.message}")
                onScreenshotStatus(mpv, OperationStatus.ERROR)
            } finally {
                screenshotRunning = false
            }
        }
    }

    private suspend fun copyScreenshotToDirectory(
        screenshotFile: File,
        directory: File,
        originalName: String
    ) = withContext(Dispatchers.IO) {
        try {
            val lastDot = originalName.lastIndexOf('.')
            val hasExtension = lastDot > 0 && lastDot < originalName.length - 1
            val baseName = if (hasExtension) originalName.substring(0, lastDot) else originalName
            val extension = if (hasExtension) ".jpg" else ""
            var number = 1
            var destination: File
            do {
                destination = File(directory, String.format("%s_%03d%s", baseName, number++, extension))
            } while (destination.exists())
            screenshotFile.copyTo(destination, overwrite = false)
            println("PlayerViewModel: screenshot salvo = ${destination.absolutePath}")
            true
        } catch (e: Exception) {
            println("PlayerViewModel: erro ao copiar screenshot: ${e.message}")
            false
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
        treeUri: Uri,
        originalName: String
    ): Boolean = withContext(Dispatchers.IO) {

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

                  return@withContext false
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

                        return@withContext false
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

                        return@withContext false
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


                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                println(
                    "PlayerViewModel: erro ao copiar screenshot: " +
                        e.message
                )
                false
            }
        }

    suspend fun copyCutVideoToTree(
        cutFile: File,
        treeUri: Uri,
        originalName: String = currentFileName() ?: "video"
    ): Boolean = withContext(Dispatchers.IO) {

            try {

                val resolver = contentResolver

                /*
                 * ------------------------------------------------
                 * CONVERTER treeUri -> documentUri DA PASTA
                 * ------------------------------------------------
                 */
                if (treeUri.scheme == "file") {
                    val directory = File(requireNotNull(treeUri.path))
                    check(directory.isDirectory) { "Pasta do corte inacessível" }
                    val dot = originalName.lastIndexOf('.')
                    val baseName = if (dot > 0) originalName.substring(0, dot) else originalName
                    val extension = ".mov"
                    var number = 1
                    var destination: File
                    do {
                        destination = File(directory, "%s_%03d%s".format(baseName, number++, extension))
                    } while (!destination.createNewFile())
                    try {
                        cutFile.inputStream().use { input ->
                            destination.outputStream().use { output -> input.copyTo(output) }
                        }
                    } catch (e: Exception) {
                        destination.delete()
                        throw e
                    }
                    println("PlayerViewModel: vídeo cortado copiado com sucesso: ${destination.absolutePath}")
                    return@withContext true
                }

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

                    extension = ".mov"
                } else {
                    baseName = originalName
                    extension = ".mov"
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

                    return@withContext false
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

                        return@withContext false
                    }

                println(
                    "PlayerViewModel: vídeo cortado copiado " +
                        "com sucesso: $destinationName"
                )

                true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {

                println(
                    "PlayerViewModel: erro ao copiar vídeo cortado: " +
                        e.message
                )
                false
            }
        }


}
