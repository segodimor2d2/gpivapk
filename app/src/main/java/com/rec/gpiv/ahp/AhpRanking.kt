package com.rec.gpiv.ahp

import kotlin.math.abs

/** Positive slider values prefer A; negative values prefer B. Zero means equality. */
object AhpRanking {
    fun ratio(value: Int): Double {
        require(value in -8..8)
        return if (value >= 0) (value + 1).toDouble() else 1.0 / (1 - value)
    }

    fun pair(size: Int, comparison: Int): Pair<Int, Int>? {
        var remaining = comparison
        for (a in 0 until size) {
            val count = size - a - 1
            if (remaining < count) return a to a + 1 + remaining
            remaining -= count
        }
        return null
    }

    data class Result(val order: List<Int>, val weights: List<Double>, val consistencyIndex: Double,
                      val consistencyRatio: Double?)

    fun rank(size: Int, values: List<Int>): Result {
        require(size > 0 && values.size.toLong() == size.toLong() * (size - 1) / 2)
        val matrix = Array(size) { DoubleArray(size) { 1.0 } }
        values.forEachIndexed { index, value ->
            val (a, b) = requireNotNull(pair(size, index))
            matrix[a][b] = ratio(value)
            matrix[b][a] = 1.0 / matrix[a][b]
        }
        var weights = DoubleArray(size) { 1.0 / size }
        for (iteration in 0 until 10000) {
            val next = DoubleArray(size) { row -> matrix[row].indices.sumOf { matrix[row][it] * weights[it] } }
            val sum = next.sum()
            next.indices.forEach { next[it] /= sum }
            val error = next.indices.maxOf { abs(next[it] - weights[it]) }
            weights = next
            if (error < 1e-12) break
        }
        val lambda = matrix.indices.sumOf { row -> matrix[row].indices.sumOf { matrix[row][it] * weights[it] } / weights[row] } / size
        val ci = if (size <= 2) 0.0 else ((lambda - size) / (size - 1)).coerceAtLeast(0.0)
        val ri = listOf(0.0, 0.0, 0.0, 0.58, 0.90, 1.12, 1.24, 1.32, 1.41, 1.45, 1.49, 1.51, 1.48, 1.56, 1.57, 1.59).getOrNull(size)
        return Result(weights.indices.sortedWith(compareByDescending<Int> { weights[it] }.thenBy { it }),
            weights.toList(), ci, if (size <= 2) 0.0 else ri?.let { ci / it })
    }

    fun name(folder: String, type: String, timestamp: String, index: Int): String {
        require(type == "Im" || type == "Mv")
        return ".$folder$type${timestamp}_${1010L + index.toLong() * 4}"
    }
}
