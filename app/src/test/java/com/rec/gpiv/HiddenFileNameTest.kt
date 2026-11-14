package com.rec.gpiv

import com.rec.gpiv.player.HiddenFileName
import org.junit.Assert.assertEquals
import org.junit.Test

class HiddenFileNameTest {
    @Test fun hidingRemovesExtensionAndItsDot() {
        assertEquals(".video", HiddenFileName.toggle("video.mp4", "mp4"))
        assertEquals(".minhas.ferias", HiddenFileName.toggle("minhas.ferias.MOV", "MOV"))
    }

    @Test fun originalExtensionIsRestoredExactly() {
        val original = "minhas.ferias.MOV"
        val extension = HiddenFileName.extension(original)
        val hidden = HiddenFileName.toggle(original, extension)
        assertEquals(original, HiddenFileName.toggle(hidden, extension))
    }

    @Test fun extensionlessFileDoesNotGainTrailingDot() {
        assertEquals("", HiddenFileName.extension("video"))
        assertEquals(".video", HiddenFileName.toggle("video", ""))
        assertEquals("video", HiddenFileName.toggle(".video", ""))
    }

    @Test fun existingHiddenFileUsesDetectedExtension() {
        assertEquals("video.mp4", HiddenFileName.toggle(".video", "mp4"))
    }
}
