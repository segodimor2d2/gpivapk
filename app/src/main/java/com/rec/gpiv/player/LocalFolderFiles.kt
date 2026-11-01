package com.rec.gpiv.player

import java.io.File

/** Arquivos da pasta e, opcionalmente, de suas subpastas imediatas. */
object LocalFolderFiles {
    fun list(directory: File, includeSubfolders: Boolean): List<File> {
        val children = directory.listFiles().orEmpty()
        val candidates = children.asSequence().flatMap { child ->
            if (includeSubfolders && child.isDirectory) {
                child.listFiles().orEmpty().asSequence()
            } else {
                sequenceOf(child)
            }
        }
        return candidates.filter { it.isFile && it.name != "gpivtags.csv" }.toList()
    }
}
