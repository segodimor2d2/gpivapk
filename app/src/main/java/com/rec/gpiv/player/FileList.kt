package com.rec.gpiv.player

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract


class FileList(
    private val contentResolver: ContentResolver
) {

    private val files =
        mutableListOf<FileItem>()

    private var currentIndex = -1

    private var currentTreeUri: Uri? = null

    /*
     * ========================================================
     * CARREGAR PASTA
     * ========================================================
     *
     * Recebe diretamente a treeUri obtida através de
     * ACTION_OPEN_DOCUMENT_TREE.
     *
     * Exemplo:
     *
     * content://com.android.externalstorage.documents/tree/
     * primary%3Aig_repit
     */

    fun loadFromTree(
        treeUri: Uri
    ): Boolean {

        files.clear()
        currentIndex = -1
        currentTreeUri = treeUri

        println(
            "FileList: carregando árvore = $treeUri"
        )


        /*
         * ----------------------------------------------------
         * DOCUMENT ID DA PASTA
         * ----------------------------------------------------
         */

        val treeDocumentId =
            try {

                DocumentsContract.getTreeDocumentId(
                    treeUri
                )

            } catch (e: Exception) {

                println(
                    "FileList: não foi possível obter " +
                        "treeDocumentId: ${e.message}"
                )

                return false
            }


        println(
            "FileList: treeDocumentId=$treeDocumentId"
        )


        /*
         * ----------------------------------------------------
         * URI DOS FILHOS
         * ----------------------------------------------------
         *
         * IMPORTANTE:
         *
         * Não usamos:
         *
         * buildChildDocumentsUri()
         *
         * Usamos:
         *
         * buildChildDocumentsUriUsingTree()
         *
         * porque temos uma URI de árvore autorizada.
         */

        val childrenUri =
            DocumentsContract
                .buildChildDocumentsUriUsingTree(
                    treeUri,
                    treeDocumentId
                )


        println(
            "FileList: childrenUri=$childrenUri"
        )


        /*
         * ----------------------------------------------------
         * CONSULTAR FILHOS
         * ----------------------------------------------------
         */

        try {

            contentResolver.query(

                childrenUri,

                arrayOf(

                    DocumentsContract.Document
                        .COLUMN_DOCUMENT_ID,

                    DocumentsContract.Document
                        .COLUMN_DISPLAY_NAME,

                    DocumentsContract.Document
                        .COLUMN_LAST_MODIFIED,

                    DocumentsContract.Document
                        .COLUMN_MIME_TYPE
                ),

                null,
                null,
                null

            )?.use { cursor ->


                val idIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document
                            .COLUMN_DOCUMENT_ID
                    )


                val nameIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document
                            .COLUMN_DISPLAY_NAME
                    )


                val modifiedIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document
                            .COLUMN_LAST_MODIFIED
                    )


                val mimeIndex =
                    cursor.getColumnIndex(
                        DocumentsContract.Document
                            .COLUMN_MIME_TYPE
                    )


                println(
                    "FileList: idIndex=$idIndex"
                )

                println(
                    "FileList: nameIndex=$nameIndex"
                )

                println(
                    "FileList: modifiedIndex=$modifiedIndex"
                )

                println(
                    "FileList: mimeIndex=$mimeIndex"
                )


                /*
                 * --------------------------------------------
                 * PERCORRER ARQUIVOS
                 * --------------------------------------------
                 */

                while (cursor.moveToNext()) {


                    if (idIndex < 0) {
                        continue
                    }


                    val childId =
                        cursor.getString(
                            idIndex
                        )


                    val name =
                        if (nameIndex >= 0) {

                            cursor.getString(
                                nameIndex
                            )

                        } else {

                            childId
                        }


                    val modifiedTime =
                        if (
                            modifiedIndex >= 0 &&
                            !cursor.isNull(
                                modifiedIndex
                            )
                        ) {

                            cursor.getLong(
                                modifiedIndex
                            )

                        } else {

                            0L
                        }


                    val mimeType =
                        if (
                            mimeIndex >= 0 &&
                            !cursor.isNull(
                                mimeIndex
                            )
                        ) {

                            cursor.getString(
                                mimeIndex
                            )

                        } else {

                            null
                        }


                    /*
                     * ----------------------------------------
                     * IGNORAR DIRETÓRIOS
                     * ----------------------------------------
                     *
                     * Neste primeiro teste queremos apenas
                     * os arquivos da pasta atual.
                     *
                     * Subpastas serão tratadas posteriormente.
                     */

                    if (
                        mimeType ==
                        DocumentsContract.Document
                            .MIME_TYPE_DIR
                    ) {

                        println(
                            "FileList: ignorando pasta = $name"
                        )

                        continue
                    }


                    /*
                     * ----------------------------------------
                     * URI DO ARQUIVO
                     * ----------------------------------------
                     *
                     * IMPORTANTE:
                     *
                     * Usamos buildDocumentUriUsingTree()
                     * porque o acesso vem da treeUri.
                     */

                    val childUri =
                        DocumentsContract
                            .buildDocumentUriUsingTree(
                                treeUri,
                                childId
                            )


                    val file =
                        FileItem(

                            uri =
                                childUri,

                            name =
                                name,

                            modifiedTime =
                                modifiedTime
                        )


                    files.add(
                        file
                    )


                    println(
                        "FileList: encontrado = " +
                            "$name " +
                            "modified=$modifiedTime " +
                            "mime=$mimeType " +
                            "uri=$childUri"
                    )
                }
            }


        } catch (e: Exception) {

            println(
                "FileList: erro ao listar arquivos: " +
                    e.message
            )

            return false
        }


        /*
         * ----------------------------------------------------
         * ORDENAR
         * ----------------------------------------------------
         *
         * Mais recente primeiro.
         */

        files.sortByDescending {

            it.modifiedTime
        }


        /*
         * ----------------------------------------------------
         * RESULTADO
         * ----------------------------------------------------
         */

        println(
            "FileList: encontrados " +
                "${files.size} arquivos"
        )


        for (index in files.indices) {

            val file =
                files[index]

            println(
                "FileList: [$index] " +
                    "${file.name} " +
                    "modified=${file.modifiedTime} " +
                    "uri=${file.uri}"
            )
        }


        /*
         * Não selecionamos nenhum arquivo ainda.
         *
         * O próximo passo será informar ao FileList qual
         * arquivo foi aberto pelo usuário para determinar
         * currentIndex.
         */

        currentIndex = -1


        return files.isNotEmpty()
    }

    fun getCurrentTreeUri(): Uri? {
        return currentTreeUri
    }

    fun setCurrent(
        uri: Uri
    ): Boolean {

        val targetDocumentId =
            try {

                DocumentsContract.getDocumentId(
                    uri
                )

            } catch (e: Exception) {

                println(
                    "FileList: não foi possível obter " +
                        "documentId do arquivo selecionado: " +
                        e.message
                )

                return false
            }

        println(
            "FileList: procurando arquivo atual"
        )

        println(
            "FileList: targetDocumentId=$targetDocumentId"
        )

        for (index in files.indices) {

            val file =
                files[index]

            val documentId =
                try {

                    DocumentsContract.getDocumentId(
                        file.uri
                    )

                } catch (e: Exception) {

                    println(
                        "FileList: erro obtendo documentId " +
                            "de ${file.name}: ${e.message}"
                    )

                    continue
                }

            if (documentId == targetDocumentId) {

                currentIndex = index

                println(
                    "FileList: arquivo atual encontrado"
                )

                println(
                    "FileList: currentIndex=$currentIndex"
                )

                println(
                    "FileList: current=${file.name}"
                )

                return true
            }
        }

        println(
            "FileList: arquivo selecionado não foi " +
                "encontrado na lista"
        )

        currentIndex = -1

        return false
    }

    /*
     * ========================================================
     * CURRENT
     * ========================================================
     */

    fun current(): FileItem? {

        if (currentIndex < 0) {
            return null
        }

        if (currentIndex >= files.size) {
            return null
        }

        return files[currentIndex]
    }


    /*
     * ========================================================
     * NEXT
     * ========================================================
     */

    fun next(): FileItem? {

        if (files.isEmpty()) {
            return null
        }

        if (currentIndex < 0) {
            return null
        }

        if (currentIndex >= files.lastIndex) {
            return null
        }

        currentIndex++

        return files[currentIndex]
    }


    /*
     * ========================================================
     * PREVIOUS
     * ========================================================
     */

    fun previous(): FileItem? {

        if (files.isEmpty()) {
            return null
        }

        if (currentIndex <= 0) {
            return null
        }

        currentIndex--

        return files[currentIndex]
    }


    /*
     * ========================================================
     * SIZE
     * ========================================================
     */

    fun size(): Int {

        return files.size
    }


    /*
     * ========================================================
     * CURRENT INDEX
     * ========================================================
     */

    fun currentIndex(): Int {

        return currentIndex
    }


    /*
     * ========================================================
     * ALL
     * ========================================================
     */

    fun all(): List<FileItem> {

        return files.toList()
    }
}
