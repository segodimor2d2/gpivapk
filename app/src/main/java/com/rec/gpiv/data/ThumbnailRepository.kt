package com.rec.gpiv.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import com.rec.gpiv.player.FileItem
import java.io.File
import java.security.MessageDigest

/** Small disk previews; the UI only decodes the visible cells. */
class ThumbnailRepository(private val resolver: ContentResolver, cacheDir: File) {
    private val cache = File(cacheDir, "thumbnails").apply { mkdirs() }

    fun prepare(folder: Uri?): (File) -> Unit {
        return try {
            if (folder == null) return {}
            if (folder.scheme == "file") {
                val directory = File(requireNotNull(folder.path), "gpvlogs/thumbnails")
                check(directory.isDirectory || directory.mkdirs())
                val persist: (File) -> Unit = { file: File ->
                    val target = File(directory, file.name)
                    if (!target.exists()) file.copyTo(target)
                    Unit
                }
                persist
            } else {
                val root = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
                val logs = child(root, "gpvlogs", DocumentsContract.Document.MIME_TYPE_DIR)
                val previews = child(logs, "thumbnails", DocumentsContract.Document.MIME_TYPE_DIR)
                val existing = children(previews).toMutableMap()
                val persist: (File) -> Unit = { file: File ->
                    val uri = existing[file.name] ?: requireNotNull(
                        DocumentsContract.createDocument(resolver, previews, "image/jpeg", file.name)
                    )
                    if (file.name !in existing) {
                        requireNotNull(resolver.openOutputStream(uri, "wt")).use { output ->
                            file.inputStream().use { it.copyTo(output) }
                        }
                        existing[file.name] = uri
                    }
                }
                persist
            }
        } catch (e: Exception) {
            Log.w("Thumbnails", "Miniaturas disponíveis no cache; pasta de logs inacessível", e)
            val fallback: (File) -> Unit = {}
            fallback
        }
    }

    fun cleanup(folder: Uri, entries: List<com.rec.gpiv.model.ThumbnailEntry>) {
        if (folder.scheme == "file") {
            val directory = File(requireNotNull(folder.path), "gpvlogs/thumbnails")
            check(!directory.exists() || directory.deleteRecursively()) { "Não foi possível apagar a pasta de thumbnails." }
        } else {
            val root = DocumentsContract.buildDocumentUriUsingTree(folder, DocumentsContract.getTreeDocumentId(folder))
            children(root)["gpvlogs"]?.let { logs ->
                children(logs)["thumbnails"]?.let { check(DocumentsContract.deleteDocument(resolver, it)) }
            }
        }
        entries.forEach { it.thumbnailPath?.let { path -> File(path).delete() } }
    }

    private fun children(parent: Uri): Map<String, Uri> {
        val result = mutableMapOf<String, Uri>()
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(parent, DocumentsContract.getDocumentId(parent))
        requireNotNull(resolver.query(uri, arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME
        ), null, null, null)).use { cursor ->
            while (cursor.moveToNext()) {
                result[cursor.getString(1)] = DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(0))
            }
        }
        return result
    }

    private fun child(parent: Uri, name: String, mime: String): Uri =
        children(parent)[name] ?: requireNotNull(DocumentsContract.createDocument(resolver, parent, mime, name))

    fun thumbnail(item: FileItem, persist: (File) -> Unit): File? {
        val key = MessageDigest.getInstance("SHA-256")
            .digest("${item.uri}|${item.modifiedTime}".toByteArray()).joinToString("") { "%02x".format(it) }
        val target = File(cache, "$key.jpg")
        try {
            if (!target.isFile || target.length() == 0L) {
                val bitmap = image(item.uri) ?: video(item.uri) ?: return null
                try {
                    val small = if (bitmap.width > 256 || bitmap.height > 256) {
                        val scale = 256.0 / maxOf(bitmap.width, bitmap.height)
                        Bitmap.createScaledBitmap(bitmap, maxOf(1, (bitmap.width * scale).toInt()), maxOf(1, (bitmap.height * scale).toInt()), true)
                    } else bitmap
                    try {
                        val temporary = File(cache, "$key.tmp")
                        temporary.outputStream().use { check(small.compress(Bitmap.CompressFormat.JPEG, 80, it)) }
                        check(temporary.renameTo(target))
                    } finally {
                        if (small !== bitmap) small.recycle()
                    }
                } finally { bitmap.recycle() }
            }
            try { persist(target) } catch (e: Exception) { Log.w("Thumbnails", "Erro ao salvar miniatura nos logs", e) }
            return target
        } catch (e: Exception) {
            Log.w("Thumbnails", "Sem miniatura: ${item.name}", e)
            return null
        }
    }

    private fun image(uri: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        options.inSampleSize = 1
        while (maxOf(options.outWidth, options.outHeight) / options.inSampleSize > 512) options.inSampleSize *= 2
        options.inJustDecodeBounds = false
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun video(uri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        try {
            requireNotNull(resolver.openFileDescriptor(uri, "r")).use { retriever.setDataSource(it.fileDescriptor) }
            return if (Build.VERSION.SDK_INT >= 27) retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, 256, 256)
            else retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        } finally { retriever.release() }
    }
}
