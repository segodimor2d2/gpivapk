package com.rec.gpiv

import com.rec.gpiv.player.FileNameOrder
import java.text.SimpleDateFormat
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNameOrderTest {
    private data class Entry(val name: String, val modified: Long)
    private fun time(value: String) = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).parse(value)!!.time
    private fun sorted(vararg items: Entry) = FileNameOrder.sorted(items.toList(), Entry::name, Entry::modified).map { it.name }

    @Test fun timestampOverridesModificationDateAndIgnoresFolderAndType() {
        val older = Entry(".ZIm20261013120000_1010", time("20261015120000"))
        val newer = Entry(".AMv20261014120000_1010", 0)
        assertEquals(listOf(newer.name, older.name), sorted(older, newer))
    }

    @Test fun fallbackDatesMixWithNamedTimestampsInDescendingOrder() {
        val older = Entry("old.jpg", time("20261012120000"))
        val named = Entry(".FolderIm20261013120000_1010", 0)
        val newer = Entry("new.mp4", time("20261014120000"))
        assertEquals(listOf(newer.name, named.name, older.name), sorted(older, named, newer))
    }

    @Test fun invalidDatesAndDigitRunsUseModificationTime() {
        val invalid = Entry(".FolderIm20260230120000_1010", 300)
        val digits = Entry("12320261013120000999.jpg", 200)
        val plain = Entry("plain.jpg", 100)
        assertEquals(listOf(invalid.name, digits.name, plain.name), sorted(plain, digits, invalid))
    }

    @Test fun sameTimestampKeepsNumericSequenceAscendingAcrossMediaTypes() {
        val late = Entry(".FolderIm20261013120000_10002", 400)
        val early = Entry(".FolderMv20261013120000_9998", 0)
        assertEquals(listOf(early.name, late.name), sorted(late, early))
    }

    @Test fun fallbackTiesAreDeterministicByName() {
        assertEquals(listOf("a.jpg", "b.jpg"), sorted(Entry("b.jpg", 0), Entry("a.jpg", 0)))
    }
}
