package com.rec.gpiv.data

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

object LocalFileMover {
    /** Reserva o nome sem sobrescrever e só remove a origem após concluir a cópia. */
    fun move(source: File, directory: File): File {
        require(source.isFile) { "Arquivo de origem inacessível." }
        require(directory.isDirectory) { "Pasta de destino inacessível." }
        require(source.canonicalFile.parentFile != directory.canonicalFile) {
            "O arquivo já está nesta pasta."
        }
        val dot = source.name.lastIndexOf('.').takeIf { it > 0 } ?: source.name.length
        val stem = source.name.substring(0, dot)
        val extension = source.name.substring(dot)
        var sequence = 0
        var destination: File
        while (true) {
            val suffix = if (sequence == 0) "" else String.format(Locale.ROOT, "%03d", sequence)
            destination = File(directory, "$stem$suffix$extension")
            if (destination.createNewFile()) break
            sequence++
        }
        try {
            source.inputStream().use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }
            if (!source.delete()) throw IOException("Não foi possível remover o arquivo de origem.")
            return destination
        } catch (error: Exception) {
            if (!destination.delete()) {
                throw IOException("${error.message} Cópia incompleta em ${destination.absolutePath}.", error)
            }
            throw error
        }
    }
}
