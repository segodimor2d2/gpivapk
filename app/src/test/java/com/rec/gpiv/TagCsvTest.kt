package com.rec.gpiv

import com.rec.gpiv.viewmodel.TagCsv
import org.junit.Assert.assertEquals
import org.junit.Test

class TagCsvTest {
    @Test fun readsExistingRows() {
        assertEquals(listOf("file:///images/photo.jpg", "ferias", "2"),
            TagCsv.parse("file:///images/photo.jpg,ferias,2"))
    }

    @Test fun preservesFolderNamesWhenSavingAndReading() {
        listOf("Férias 2026", "Família, amigos", "Fotos \"favoritas\"", " pasta ").forEach { name ->
            assertEquals(listOf("file:///images/photo.jpg", name, "2"),
                TagCsv.parse(TagCsv.row("file:///images/photo.jpg", name, 2)))
        }
    }
}
