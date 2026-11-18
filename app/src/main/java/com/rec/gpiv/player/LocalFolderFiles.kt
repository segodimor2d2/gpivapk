package com.rec.gpiv.player

import java.io.File

/** Arquivos da pasta e, opcionalmente, de suas subpastas imediatas. */
object LocalFolderFiles {
    fun list(directory: File, includeSubfolders: Boolean, maxFilesPerSubfolder: Int? = null): List<File> {
        val entries = directory.listFiles().orEmpty().toList()
        val children = if (includeSubfolders && maxFilesPerSubfolder != null) {
            val (subfolders, rootFiles) = entries.partition(File::isDirectory)
            subfolders.sortedBy { it.name.lowercase() } +
                FileNameOrder.sorted(rootFiles, File::getName, File::lastModified)
        } else {
            entries
        }
        val candidates = children.asSequence().flatMap { child ->
            if (includeSubfolders && child.isDirectory) {
                val files = child.listFiles().orEmpty().filter { it.isFile && it.name != "gpivtags.csv" && !FileEligibility.isMarkdown(it.name) }
                (if (maxFilesPerSubfolder != null) {
                    FileNameOrder.sorted(files, File::getName, File::lastModified).take(maxFilesPerSubfolder)
                } else files).asSequence()
            } else {
                sequenceOf(child)
            }
        }
        return candidates.filter { it.isFile && it.name != "gpivtags.csv" && !FileEligibility.isMarkdown(it.name) }.toList()
    }
}
