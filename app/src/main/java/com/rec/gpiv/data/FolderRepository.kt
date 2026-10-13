package com.rec.gpiv.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.content.ContentUris
import android.os.Environment
import android.system.Os
import java.io.File

/**
 * Persiste as pastas escolhidas pelo usuário e resolve a pasta SAF que
 * corresponde a um arquivo recebido pelo sistema.
 */
class FolderRepository(context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun hasReadPermission(uri: Uri): Boolean =
        contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isReadPermission
        }

    fun savedFolders(): List<Uri> =
        preferences.getStringSet(FOLDER_URIS_KEY, emptySet())
            .orEmpty()
            .map(Uri::parse)

    fun validSavedFolders(): List<Uri> =
        savedFolders().filter(::hasReadPermission)

    fun saveFolder(uri: Uri) {
        val folders = savedFolders()
            .toMutableSet()
            .apply { add(uri) }

        preferences.edit()
            .putStringSet(
                FOLDER_URIS_KEY,
                folders.map(Uri::toString).toSet()
            )
            .apply()
    }

    fun findMatchingFolder(uri: Uri): Uri? {
        val folders = validSavedFolders()

        findDocumentsProviderMatch(uri, folders)?.let { return it }
        return findExternalStorageMatch(uri, folders)
    }

    /** Resolve somente um arquivo real e acessível; nunca procura apenas pelo nome. */
    fun findLocalFile(uri: Uri): File? {
        fun accessible(path: String?): File? = runCatching {
            path?.let(::File)?.canonicalFile?.takeIf {
                it.isFile && it.canRead() && it.parentFile?.canRead() == true
            }
        }.getOrNull()

        if (uri.scheme == "file") return accessible(uri.path)
        if (uri.scheme != "content") return null

        if (uri.authority == "com.android.externalstorage.documents") {
            val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            if (id != null) {
                val volume = id.substringBefore(':')
                val root = if (volume == "primary") Environment.getExternalStorageDirectory()
                    else File("/storage/$volume")
                accessible(File(root, id.substringAfter(':', "")).path)?.let { return it }
            }
        }

        fun queryPath(target: Uri): File? = runCatching {
            contentResolver.query(target, arrayOf(MediaStore.MediaColumns.DATA), null, null, null)?.use {
                if (it.moveToFirst()) accessible(it.getString(0)) else null
            }
        }.getOrNull()

        queryPath(uri)?.let { return it }
        if (uri.authority == "com.android.providers.media.documents") {
            val id = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull()
            val number = id?.substringAfter(':')?.toLongOrNull()
            val collection = when (id?.substringBefore(':')) {
                "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                else -> null
            }
            if (number != null && collection != null) {
                queryPath(ContentUris.withAppendedId(collection, number))?.let { return it }
            }
        }

        // FileProvider de gerenciadores de arquivos pode não fornecer a coluna DATA.
        // O descritor aponta para o arquivo local quando o provedor não usa um pipe.
        runCatching {
            contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                accessible(Os.readlink("/proc/self/fd/${descriptor.fd}"))
            }
        }.getOrNull()?.let { return it }

        // Alguns gerenciadores enviam o caminho absoluto dentro da URI content.
        val path = uri.path
        if (path != null) {
            val storageIndex = path.indexOf("/storage/")
            if (storageIndex >= 0) accessible(path.substring(storageIndex))?.let { return it }
        }
        return null
    }

    private fun findDocumentsProviderMatch(
        fileUri: Uri,
        folders: List<Uri>
    ): Uri? {
        val documentId = try {
            DocumentsContract.getDocumentId(fileUri)
        } catch (_: Exception) {
            return null
        }

        return folders.firstOrNull { treeUri ->
            if (treeUri.authority != fileUri.authority) return@firstOrNull false
            val treeDocumentId = try {
                DocumentsContract.getTreeDocumentId(treeUri)
            } catch (_: Exception) {
                return@firstOrNull false
            }

            documentId == treeDocumentId ||
                documentId.startsWith("$treeDocumentId/")
        }
    }

    private fun findExternalStorageMatch(
        fileUri: Uri,
        folders: List<Uri>
    ): Uri? {
        val path = fileUri.path ?: return null
        if (!path.startsWith(EXTERNAL_STORAGE_PREFIX)) return null

        val relativePath = path.removePrefix(EXTERNAL_STORAGE_PREFIX)

        return folders.firstOrNull { treeUri ->
            val folderPath = try {
                DocumentsContract.getTreeDocumentId(treeUri)
                    .removePrefix(PRIMARY_PREFIX)
                    .trimEnd('/')
            } catch (_: Exception) {
                return@firstOrNull false
            }

            relativePath == folderPath ||
                relativePath.startsWith("$folderPath/")
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "gpiv_preferences"
        const val FOLDER_URIS_KEY = "folder_uris"
        const val EXTERNAL_STORAGE_PREFIX = "/storage/emulated/0/"
        const val PRIMARY_PREFIX = "primary:"
    }
}
