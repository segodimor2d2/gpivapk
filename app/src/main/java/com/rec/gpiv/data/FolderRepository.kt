package com.rec.gpiv.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

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
