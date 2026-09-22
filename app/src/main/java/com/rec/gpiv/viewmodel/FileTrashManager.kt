package com.rec.gpiv.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log

/** Isola as operações SAF relacionadas à pasta trash. */
class FileTrashManager(
    private val resolver: ContentResolver,
    private val currentTreeUri: () -> Uri?
) {

    fun findTrashUri(): Uri? {
        val treeUri = currentTreeUri() ?: return null
        val treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri)

        return try {
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                treeDocumentId
            )

            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID
                )
                val nameIndex = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                )
                val mimeIndex = cursor.getColumnIndex(
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                )

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    val mime = cursor.getString(mimeIndex)

                    if (
                        name == "trash" &&
                        mime == DocumentsContract.Document.MIME_TYPE_DIR
                    ) {
                        val documentId = cursor.getString(idIndex)
                        println("FileTrashManager: pasta trash encontrada")
                        return@use DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            documentId
                        )
                    }
                }

                null
            } ?: run {
                val rootUri = DocumentsContract.buildDocumentUriUsingTree(
                    treeUri,
                    treeDocumentId
                )

                val createdUri = DocumentsContract.createDocument(
                    resolver,
                    rootUri,
                    DocumentsContract.Document.MIME_TYPE_DIR,
                    "trash"
                )

                if (createdUri != null) {
                    println("FileTrashManager: pasta trash criada = $createdUri")
                } else {
                    println("FileTrashManager: não foi possível criar pasta trash")
                }

                createdUri
            }
        } catch (e: Exception) {
            Log.e("FileTrashManager", "Erro ao localizar pasta trash", e)
            null
        }
    }

    fun moveToTrash(uri: Uri): Boolean {
        val trashUri = findTrashUri() ?: return false
        val treeUri = currentTreeUri() ?: return false

        return try {
            // O pai deve ser o documento da árvore autorizada, não a URI do arquivo.
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            )
            val movedUri = DocumentsContract.moveDocument(
                resolver,
                uri,
                parentUri,
                trashUri
            )

            if (movedUri != null) {
                println("FileTrashManager: arquivo movido para trash = $movedUri")
                true
            } else {
                println("FileTrashManager: moveDocument retornou null")
                false
            }
        } catch (e: Exception) {
            Log.e("FileTrashManager", "Erro ao mover arquivo para trash", e)
            false
        }
    }

    fun deletePermanently(uri: Uri): Boolean {
        return try {
            DocumentsContract.deleteDocument(resolver, uri)
        } catch (e: Exception) {
            Log.e("FileTrashManager", "Erro ao apagar arquivo: $uri", e)
            false
        }
    }
}
