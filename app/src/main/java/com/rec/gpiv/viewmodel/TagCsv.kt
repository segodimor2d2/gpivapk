package com.rec.gpiv.viewmodel

/** CSV rows with quoted folder names, including commas and quotation marks. */
internal object TagCsv {
    fun row(uri: String, tag: String, index: Int): String =
        listOf(uri, tag, index.toString()).joinToString(",") { value ->
            if (value.contains(',') || value.contains('"')) "\"${value.replace("\"", "\"\"")}\"" else value
        }

    fun parse(line: String): List<String> {
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && index + 1 < line.length && line[index + 1] == '"' -> {
                    field.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    fields.add(field.toString())
                    field.setLength(0)
                }
                else -> field.append(char)
            }
            index++
        }
        fields.add(field.toString())
        return fields
    }
}
