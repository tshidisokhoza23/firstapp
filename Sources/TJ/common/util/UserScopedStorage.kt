package com.tpsk.tj.common.util

import android.content.Context
import java.io.File

class UserScopedStorage(private val context: Context, private val userId: String) {

    fun getAttachmentsDir(): File {
        val dir = File(context.getExternalFilesDir(null), "attachments/$userId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getTempDir(): File {
        val dir = File(context.cacheDir, "temp/$userId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun clearUserData() {
        getAttachmentsDir().deleteRecursively()
        getTempDir().deleteRecursively()
    }
}
