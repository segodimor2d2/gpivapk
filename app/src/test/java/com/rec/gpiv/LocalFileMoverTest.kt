package com.rec.gpiv

import com.rec.gpiv.data.LocalFileMover
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalFileMoverTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun movesContentsAndRemovesSource() {
        val source = temporary.newFile("foto.jpg").apply { writeText("imagem") }
        val destination = LocalFileMover.move(source, temporary.newFolder("destino"))
        assertEquals("foto.jpg", destination.name)
        assertEquals("imagem", destination.readText())
        assertFalse(source.exists())
    }

    @Test fun skipsExistingNamesAndDirectoriesWithoutOverwriting() {
        val folder = temporary.newFolder("destino")
        File(folder, "foto.jpg").writeText("original")
        File(folder, "foto001.jpg").mkdir()
        File(folder, "foto002.jpg").writeText("outra")
        val source = temporary.newFile("foto.jpg").apply { writeText("nova") }
        assertEquals("foto003.jpg", LocalFileMover.move(source, folder).name)
        assertEquals("original", File(folder, "foto.jpg").readText())
        assertEquals("outra", File(folder, "foto002.jpg").readText())
    }

    @Test fun numbersFilesWithoutExtension() {
        val folder = temporary.newFolder("destino")
        File(folder, "arquivo").writeText("original")
        val source = temporary.newFile("arquivo").apply { writeText("novo") }
        assertEquals("arquivo001", LocalFileMover.move(source, folder).name)
    }

    @Test fun sameFolderKeepsSource() {
        val source = temporary.newFile("foto.jpg").apply { writeText("original") }
        assertThrows(IllegalArgumentException::class.java) {
            LocalFileMover.move(source, source.parentFile!!)
        }
        assertEquals("original", source.readText())
    }
}
