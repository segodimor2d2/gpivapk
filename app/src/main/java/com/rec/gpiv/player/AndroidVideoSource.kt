package com.rec.gpiv.player

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor

class AndroidVideoSource(
    private val contentResolver: ContentResolver
) {

    fun open(uri: Uri): ParcelFileDescriptor? {
        return contentResolver.openFileDescriptor(
            uri,
            "r"
        )
    }
}
