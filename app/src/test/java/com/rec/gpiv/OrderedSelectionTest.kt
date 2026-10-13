package com.rec.gpiv

import com.rec.gpiv.model.OrderedSelection
import org.junit.Assert.assertEquals
import org.junit.Test

class OrderedSelectionTest {
    @Test fun clickingAppendsInChosenOrder() {
        val first = OrderedSelection.toggle(emptyList<String>(), "B")
        assertEquals(listOf("B", "A"), OrderedSelection.toggle(first, "A"))
    }
    @Test fun removingClosesGapAndReselectingAppends() {
        val removed = OrderedSelection.toggle(listOf("B", "A", "C"), "A")
        assertEquals(listOf("B", "C"), removed)
        assertEquals(listOf("B", "C", "A"), OrderedSelection.toggle(removed, "A"))
    }
    @Test fun selectedEntriesExcludeUnselectedAndUseClickOrder() {
        assertEquals(listOf("C", "A"), OrderedSelection.entries(listOf("A", "B", "C"), listOf("C", "A")) { it })
    }
    @Test fun missingAndEmptySelectionDoNotSelectOtherFiles() {
        assertEquals(emptyList<String>(), OrderedSelection.entries(listOf("A", "B"), listOf("missing")) { it })
        assertEquals(emptyList<String>(), OrderedSelection.entries(listOf("A", "B"), emptyList<String>()) { it })
    }
}
