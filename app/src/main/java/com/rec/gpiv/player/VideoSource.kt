package com.rec.gpiv.player

import android.content.ContentResolver
import android.net.Uri
import android.os.ParcelFileDescriptor

class VideoSource(
    private val contentResolver: ContentResolver
) {

    private var parcelFileDescriptor: ParcelFileDescriptor? = null

    fun open(uri: Uri): Boolean {

        close()

        parcelFileDescriptor =
            contentResolver.openFileDescriptor(
                uri,
                "r"
            )

        return parcelFileDescriptor != null
    }

    fun getFileDescriptor(): Int? {
        return parcelFileDescriptor?.fd
    }

    fun close() {

        val fd = parcelFileDescriptor?.fd

        if (fd != null) {
            println(
                "VideoSource: closing fd = $fd"
            )
        }

        parcelFileDescriptor?.close()
        parcelFileDescriptor = null
    }
}
