package com.tpsk.tj.common.util

import android.content.ContentResolver
import android.net.Uri
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import java.io.IOException

class UriRequestBody(
    private val resolver: ContentResolver,
    private val uri: Uri,
    private val mediaType: MediaType?
) : RequestBody() {
    override fun contentType(): MediaType? = mediaType

    override fun contentLength(): Long = try {
        if (uri.scheme == "file") {
            java.io.File(uri.path ?: "").length()
        } else {
            resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }
    } catch (_: Exception) {
        -1L
    }

    override fun writeTo(sink: BufferedSink) {
        val stream = if (uri.scheme == "file") {
            java.io.FileInputStream(java.io.File(uri.path ?: ""))
        } else {
            resolver.openInputStream(uri)
        }
        stream?.use { input ->
            sink.writeAll(input.source())
        } ?: throw IOException("Unable to open selected media ($uri)")
    }
}
