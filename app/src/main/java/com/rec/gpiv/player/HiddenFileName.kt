package com.rec.gpiv.player

object HiddenFileName {
    fun extension(name: String): String =
        if (name.lastIndexOf('.') > 0) name.substringAfterLast('.') else ""

    fun toggle(name: String, extension: String): String {
        val result = if (name.startsWith(".")) {
            name.drop(1) + if (extension.isEmpty()) "" else ".$extension"
        } else {
            "." + if (extension.isEmpty()) name else name.substringBeforeLast('.')
        }
        require(result.isNotBlank() && result != ".") { "Nome de arquivo inválido." }
        return result
    }
}
