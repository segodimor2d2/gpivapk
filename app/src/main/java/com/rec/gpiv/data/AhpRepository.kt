package com.rec.gpiv.data

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.rec.gpiv.model.ThumbnailEntry
import com.rec.gpiv.ahp.AhpRanking
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AhpRepository(context: Context) {
    private val resolver = context.contentResolver
    private val preferences = context.getSharedPreferences("ahp_comparisons", Context.MODE_PRIVATE)
    private val journal = File(context.filesDir, "ahp-rename-journal.json")

    fun mediaType(uri: Uri): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        runCatching { resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) } }
        if (bounds.outWidth > 0 && bounds.outHeight > 0) return "Im"
        return runCatching {
            val retriever = MediaMetadataRetriever()
            try {
                resolver.openFileDescriptor(uri, "r")?.use {
                    retriever.setDataSource(it.fileDescriptor)
                    if (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes") "Mv" else null
                }
            } finally { retriever.release() }
        }.getOrNull()
    }

    private fun key(folder: Uri, entries: List<ThumbnailEntry>): String = folder.toString() + entries.joinToString { "${it.uri}:${it.name}:${it.modifiedTime}" }

    fun read(folder: Uri, entries: List<ThumbnailEntry>): List<Int> = runCatching {
        val array = JSONArray(preferences.getString(key(folder, entries), "[]"))
        List(array.length()) { array.getInt(it).also { value -> require(value in -8..8) } }
            .also { require(it.size.toLong() <= entries.size.toLong() * (entries.size - 1) / 2) }
    }.getOrDefault(emptyList())

    fun save(folder: Uri, entries: List<ThumbnailEntry>, values: List<Int>) {
        check(preferences.edit().putString(key(folder, entries), JSONArray(values).toString()).commit()) {
            "Não foi possível salvar a avaliação."
        }
    }

    private fun displayName(uri: Uri): String = if (uri.scheme == "file") File(requireNotNull(uri.path)).name else {
        requireNotNull(resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)).use {
            check(it.moveToFirst()); it.getString(0)
        }
    }

    private fun rename(uri: Uri, name: String): Uri {
        val result = if (uri.scheme == "file") {
            val source = File(requireNotNull(uri.path))
            val target = File(source.parentFile, name)
            check(!target.exists()) { "Já existe um arquivo chamado $name." }
            check(source.renameTo(target)) { "Não foi possível renomear ${source.name}." }
            Uri.fromFile(target)
        } else requireNotNull(DocumentsContract.renameDocument(resolver, uri, name))
        check(displayName(result) == name) { "O provedor não preservou o nome sem extensão: $name." }
        return result
    }

    /** Retained journal allows recovery if a process is interrupted during a batch. */
    fun recover() {
        if (!journal.exists()) return
        val records = JSONArray(journal.readText())
        for (i in records.length() - 1 downTo 0) {
            val record = records.getJSONObject(i)
            var current = Uri.parse(record.getString("current"))
            val pending = record.optString("pending")
            if (pending.isNotEmpty()) {
                if (current.scheme == "file") {
                    val candidate = File(File(requireNotNull(current.path)).parentFile, pending)
                    if (candidate.exists()) current = Uri.fromFile(candidate)
                } else {
                    val folder = Uri.parse(record.getString("folder"))
                    val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
                    resolver.query(children, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { cursor ->
                        while (cursor.moveToNext()) {
                            if (cursor.getString(1) == pending) {
                                current = DocumentsContract.buildDocumentUriUsingTree(folder, cursor.getString(0))
                                break
                            }
                        }
                    }
                }
            }
            if (displayName(current) != record.getString("original")) {
                record.put("current", current.toString()).put("pending", record.getString("original"))
                writeJournal(records)
                val restored = rename(current, record.getString("original"))
                record.put("current", restored.toString()).remove("pending")
                writeJournal(records)
            }
        }
        check(journal.delete())
    }

    private fun writeJournal(records: JSONArray) {
        val temporary = File(journal.parentFile, "ahp-rename-journal.tmp")
        temporary.outputStream().use { stream ->
            stream.write(records.toString().toByteArray())
            stream.fd.sync()
        }
        check(temporary.renameTo(journal))
    }

    fun finish(folder: Uri, entries: List<ThumbnailEntry>, values: List<Int>): Pair<List<Uri>, String> {
        check(!journal.exists()) { "Há uma renomeação interrompida que precisa ser recuperada." }
        val result = AhpRanking.rank(entries.size, values)
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val folderName = displayName(if (folder.scheme == "file") folder else
            DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder)))
        val targets = result.order.mapIndexed { index, original ->
            AhpRanking.name(folderName, requireNotNull(entries[original].mediaType), timestamp, index)
        }
        val occupied = if (folder.scheme == "file") File(requireNotNull(folder.path)).list()?.toSet().orEmpty() else {
            val root = DocumentsContract.getTreeDocumentId(folder)
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(folder, root)
            buildSet {
                requireNotNull(resolver.query(children, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)).use {
                    while (it.moveToNext()) add(it.getString(0))
                }
            }
        }
        check(targets.none { it in occupied }) { "Um nome de destino já existe. Tente novamente em outro segundo." }
        val records = JSONArray(entries.map { JSONObject().put("original", it.name).put("current", it.uri.toString()).put("folder", folder.toString()) })
        val token = UUID.randomUUID().toString()
        writeJournal(records)
        fun move(index: Int, name: String) {
            val record = records.getJSONObject(index)
            record.put("pending", name)
            writeJournal(records)
            val oldUri = Uri.parse(record.getString("current"))
            // Persist changed URI before verifying the provider's actual filename.
            val newUri = if (oldUri.scheme == "file") {
                val source = File(requireNotNull(oldUri.path)); val target = File(source.parentFile, name)
                check(!target.exists() && source.renameTo(target)) { "Falha ao renomear ${source.name}." }
                Uri.fromFile(target)
            } else requireNotNull(DocumentsContract.renameDocument(resolver, oldUri, name))
            record.put("current", newUri.toString()).remove("pending")
            writeJournal(records)
            check(displayName(newUri) == name) { "O provedor alterou o nome solicitado: $name." }
        }
        try {
            entries.indices.forEach { move(it, ".ahp-$token-$it") }
            result.order.forEachIndexed { rank, index -> move(index, targets[rank]) }
        } catch (e: Exception) {
            try { recover() } catch (rollback: Exception) {
                throw IllegalStateException("${e.message} Recuperação pendente: ${rollback.message}", e)
            }
            throw e
        }
        val renamed = result.order.map { Uri.parse(records.getJSONObject(it).getString("current")) }
        check(journal.delete())
        preferences.edit().remove(key(folder, entries)).commit()
        val consistency = result.consistencyRatio?.let { "Consistência CR: %.1f%%".format(it * 100) }
            ?: "Índice de consistência: %.4f".format(result.consistencyIndex)
        return renamed to "$consistency${if ((result.consistencyRatio ?: 0.0) > 0.10) " (avaliações com baixa consistência)" else ""}"
    }
}
