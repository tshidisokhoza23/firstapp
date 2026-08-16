package com.example.firstapp.media.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import javax.inject.Inject

class DriveUploadWorker(
    context: Context,
    params: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FILE_URI = "file_uri"
        const val KEY_REMOTE_PATH = "remote_path"
        const val KEY_PROGRESS = "progress"
        private const val CHUNK_SIZE = 5 * 1024 * 1024 // 5MB
    }

    override suspend fun doWork(): Result {
        val fileUriStr = inputData.getString(KEY_FILE_URI) ?: return Result.failure()
        val remotePath = inputData.getString(KEY_REMOTE_PATH) ?: "uploads"
        
        val fileUri = Uri.parse(fileUriStr)
        val file = getFileFromUri(fileUri) ?: return Result.failure()
        
        val totalSize = file.length()
        var uploadedSize = 0L
        val uploadId = java.util.UUID.randomUUID().toString()

        try {
            FileInputStream(file).use { inputStream ->
                val buffer = ByteArray(CHUNK_SIZE)
                var bytesRead: Int
                var chunkIndex = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val chunk = if (bytesRead < CHUNK_SIZE) buffer.copyOf(bytesRead) else buffer
                    
                    val success = uploadChunk(
                        chunk = chunk,
                        uploadId = uploadId,
                        offset = uploadedSize,
                        totalSize = totalSize,
                        remotePath = remotePath
                    )

                    if (!success) {
                        return Result.retry()
                    }

                    uploadedSize += bytesRead
                    chunkIndex++
                    
                    val progress = (uploadedSize * 100 / totalSize).toInt()
                    setProgress(workDataOf(KEY_PROGRESS to progress))
                    Log.d("DriveUploadWorker", "Upload progress: $progress%")
                }
            }
            
            return Result.success(workDataOf("url" to "https://api.example.com/files/$uploadId"))
        } catch (e: Exception) {
            Log.e("DriveUploadWorker", "Upload failed", e)
            return Result.retry()
        }
    }

    private suspend fun uploadChunk(
        chunk: ByteArray,
        uploadId: String,
        offset: Long,
        totalSize: Long,
        remotePath: String
    ): Boolean {
        val requestBody = chunk.toRequestBody("application/octet-stream".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url("https://firstapp-backend.fly.dev/api/v1/drive/upload/chunk")
            .header("X-Upload-ID", uploadId)
            .header("X-Content-Offset", offset.toString())
            .header("X-Content-Length", totalSize.toString())
            .header("X-Remote-Path", remotePath)
            .post(requestBody)
            .build()

        return try {
            okHttpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: IOException) {
            false
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val context = applicationContext
            val file = File(context.cacheDir, "upload_tmp_${System.currentTimeMillis()}")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            null
        }
    }
}
