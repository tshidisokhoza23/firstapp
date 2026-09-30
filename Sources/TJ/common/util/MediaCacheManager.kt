package com.tpsk.tj.common.util

import android.content.Context
import java.io.File

object MediaCacheManager {

    fun getTempMediaDir(context: Context): File {
        val dir = File(context.cacheDir, "media_temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun createTempFile(context: Context, prefix: String = "media_", suffix: String = ".tmp"): File {
        val dir = getTempMediaDir(context)
        return File.createTempFile(prefix, suffix, dir)
    }

    fun cleanExpiredCache(context: Context, maxAgeMs: Long = 24 * 60 * 60 * 1000L) {
        val dir = File(context.cacheDir, "media_temp")
        if (!dir.exists()) return

        val now = System.currentTimeMillis()
        dir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > maxAgeMs) {
                file.delete()
            }
        }
    }
}
