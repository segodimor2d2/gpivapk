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

        println(
            "VideoSource: abrindo URI = $uri"
        )

        parcelFileDescriptor =
            contentResolver.openFileDescriptor(
                uri,
                "r"
            )

        println(
            "VideoSource: PFD = $parcelFileDescriptor"
        )

        println(
            "VideoSource: FD = ${parcelFileDescriptor?.fd}"
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
