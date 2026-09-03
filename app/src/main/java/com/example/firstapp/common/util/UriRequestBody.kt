package com.example.firstapp.common.util

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
        resolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
    } catch (_: IOException) {
        -1L
    }

    override fun writeTo(sink: BufferedSink) {
        resolver.openInputStream(uri)?.use { input ->
            sink.writeAll(input.source())
        } ?: throw IOException("Unable to open selected media")
    }
}
