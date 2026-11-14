package com.rec.gpiv.player

object FileEligibility {
    fun isMarkdown(name: String): Boolean = name.endsWith(".md", ignoreCase = true)
}
