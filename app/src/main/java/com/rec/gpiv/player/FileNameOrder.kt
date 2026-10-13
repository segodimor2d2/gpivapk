package com.rec.gpiv.player

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

/** Uses local-time yyyyMMddHHmmss names and modification time on the same timeline. */
object FileNameOrder {
    private val timestampPattern = Regex("(?<![0-9])([0-9]{14})(?![0-9])(?:_([0-9]+))?")

    private data class Key(val timestamp: Long, val sequence: Long, val name: String)

    fun <T> sorted(items: List<T>, name: (T) -> String, modifiedTime: (T) -> Long): List<T> {
        val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply { isLenient = false }
        return items.map { item ->
            val filename = name(item)
            val timestamp = timestampPattern.findAll(filename).mapNotNull { match ->
                val raw = match.groupValues[1]
                val position = ParsePosition(0)
                val date = format.parse(raw, position)
                if (date != null && position.index == raw.length) {
                    date.time to (match.groupValues[2].toLongOrNull() ?: Long.MAX_VALUE)
                } else null
            }.lastOrNull()
            item to Key(timestamp?.first ?: modifiedTime(item), timestamp?.second ?: Long.MAX_VALUE, filename)
        }.sortedWith(compareByDescending<Pair<T, Key>> { it.second.timestamp }
            .thenBy { it.second.sequence }.thenBy { it.second.name })
            .map { it.first }
    }
}
