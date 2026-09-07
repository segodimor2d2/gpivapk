package com.rec.gpiv.player

import android.net.Uri

data class FileItem(
    val uri: Uri,
    val name: String,
    val modifiedTime: Long
)
