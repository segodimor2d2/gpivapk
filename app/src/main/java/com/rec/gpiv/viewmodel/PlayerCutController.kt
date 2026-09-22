package com.rec.gpiv.viewmodel

import com.rec.gpiv.player.VideoPlayer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Executa o corte nativo e exporta o vídeo temporário para a pasta SAF. */
class PlayerCutController(
    private val player: VideoPlayer,
    private val cacheDir: File,
    private val scope: CoroutineScope,
    private val currentTreeUri: () -> android.net.Uri?,
    private val mediaExport: MediaExportManager
) {

    private var lastCutFrameInfo: CutFrameInfo? = null

    fun testCurrentCut(markers: Pair<FrameMarker, FrameMarker>?) {
        val treeUri = currentTreeUri()
        if (markers == null) {
            println("PlayerViewModel: A/B não definidos")
            return
        }
        if (treeUri == null) {
            println("PlayerViewModel: nenhuma pasta SAF selecionada")
            return
        }

        val start = markers.first
        val end = markers.second
        println("PlayerViewModel: ordem do corte start=${start.frame} end=${end.frame}")
        println("PlayerViewModel: testCutFrames A=${start.frame} B=${end.frame}")

        val result = player.testCutFrames(start.frame, end.frame)
        if (result == null || result.size < 6) {
            println("PlayerViewModel: testCutFrames sem resultado")
            return
        }

        val cutInfo = CutFrameInfo(
            frameA = result[0],
            ptsA = result[1],
            frameB = result[2],
            ptsB = result[3],
            timeBaseNum = result[4],
            timeBaseDen = result[5]
        )
        lastCutFrameInfo = cutInfo
        println(
            "PlayerViewModel: corte A=${cutInfo.frameA} timeA=${cutInfo.timeA} " +
                "B=${cutInfo.frameB} timeB=${cutInfo.timeB} duration=${cutInfo.duration}"
        )

        val cutFile = File(cacheDir, "frameA.mov")
        if (!cutFile.isFile || cutFile.length() <= 0L) {
            println(
                "PlayerViewModel: arquivo do corte não encontrado ou vazio = " +
                    cutFile.absolutePath
            )
            return
        }

        println(
            "PlayerViewModel: vídeo cortado encontrado = ${cutFile.absolutePath} " +
                "size=${cutFile.length()}"
        )
        scope.launch {
            mediaExport.copyCutVideoToTree(cutFile, treeUri)
        }
    }
}
