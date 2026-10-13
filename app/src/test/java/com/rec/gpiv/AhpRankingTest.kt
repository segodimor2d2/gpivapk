package com.rec.gpiv

import com.rec.gpiv.ahp.AhpRanking
import org.junit.Assert.*
import org.junit.Test

class AhpRankingTest {
    @Test fun sliderUsesReciprocalSaatyScale() {
        assertEquals(1.0, AhpRanking.ratio(0), 0.0)
        assertEquals(9.0, AhpRanking.ratio(8), 0.0)
        assertEquals(1.0 / 9, AhpRanking.ratio(-8), 0.0)
        for (value in 0..8) assertEquals(1.0, AhpRanking.ratio(value) * AhpRanking.ratio(-value), 1e-12)
    }
    @Test fun visitsEveryPairOnce() {
        assertEquals(listOf(0 to 1, 0 to 2, 0 to 3, 1 to 2, 1 to 3, 2 to 3),
            (0..5).map { AhpRanking.pair(4, it) })
        assertNull(AhpRanking.pair(4, 6))
    }
    @Test fun consistentRatiosProduceExpectedPriorities() {
        // A/B=2, A/C=6, B/C=3 => priorities 6:3:1.
        val result = AhpRanking.rank(3, listOf(1, 5, 2))
        assertEquals(listOf(0, 1, 2), result.order)
        listOf(0.6, 0.3, 0.1).forEachIndexed { index, expected -> assertEquals(expected, result.weights[index], 1e-10) }
        assertEquals(0.0, result.consistencyRatio!!, 1e-10)
    }
    @Test fun detectsContradictoryPreferences() {
        assertTrue(AhpRanking.rank(3, listOf(8, -8, 8)).consistencyRatio!! > 0.10)
    }
    @Test fun tiesAndSingleFileHaveStableOrder() {
        assertEquals(listOf(0, 1, 2), AhpRanking.rank(3, listOf(0, 0, 0)).order)
        assertEquals(listOf(0), AhpRanking.rank(1, emptyList()).order)
    }
    @Test fun namesHaveNoExtensionAndIncrementByFour() {
        assertEquals(".FériasIm20261013153045_1010", AhpRanking.name("Férias", "Im", "20261013153045", 0))
        assertEquals(".FériasMv20261013153045_1018", AhpRanking.name("Férias", "Mv", "20261013153045", 2))
    }
}
