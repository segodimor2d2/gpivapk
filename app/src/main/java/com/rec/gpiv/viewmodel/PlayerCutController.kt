package com.rec.gpiv.viewmodel

import com.rec.gpiv.player.VideoPlayer
import com.rec.gpiv.model.OperationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Executa o corte nativo e exporta o vídeo temporário para a pasta aberta. */
class PlayerCutController(
    private val player: VideoPlayer,
    private val cacheDir: File,
    private val scope: CoroutineScope,
    private val currentTreeUri: () -> android.net.Uri?,
    private val mediaExport: MediaExportManager,
    private val currentFileName: () -> String?,
    private val onStatus: (Boolean, OperationStatus) -> Unit = { _, _ -> }
) {

    private var lastCutFrameInfo: CutFrameInfo? = null

    private var running = false

    fun testCurrentCut(markers: Pair<FrameMarker, FrameMarker>?, copyPackets: Boolean = false) {
        if (running) return
        val folder = currentTreeUri()
        val originalName = currentFileName() ?: "video"
        onStatus(copyPackets, OperationStatus.RUNNING)
        if (markers == null || folder == null || markers.first.position > markers.second.position) {
            onStatus(copyPackets, OperationStatus.ERROR)
            return
        }
        running = true
        scope.launch {
            try {
                val success = withContext(Dispatchers.IO) {
                    val cutFile = File(cacheDir, "frameA.mov")
                    // A failed native cut must never export a previous result.
                    if (cutFile.exists() && !cutFile.delete()) return@withContext false
                    val result = player.testCutFrames(
                        markers.first.frame, markers.second.frame,
                        markers.first.position, markers.second.position, copyPackets
                    )
                    if (result == null || result.size < 6 || !cutFile.isFile || cutFile.length() == 0L) {
                        return@withContext false
                    }
                    lastCutFrameInfo = CutFrameInfo(result[0], result[1], result[2], result[3], result[4], result[5])
                    mediaExport.copyCutVideoToTree(cutFile, folder, originalName)
                }
                onStatus(copyPackets, if (success) OperationStatus.SUCCESS else OperationStatus.ERROR)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("PlayerViewModel: erro no corte: ${e.message}")
                onStatus(copyPackets, OperationStatus.ERROR)
            } finally {
                running = false
            }
        }
    }
}
