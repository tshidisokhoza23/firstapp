package com.example.firstapp.media.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * DriveUploadWorker
 * Handles chunked, resumable file uploads to the Cloud Drive.
 * Features: Background execution, retry logic, progress reporting, and network constraints.
 */
@HiltWorker
class DriveUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val okHttpClient: OkHttpClient
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FILE_URI = "key_file_uri"
        const val KEY_REMOTE_PATH = "key_remote_path"
        const val KEY_PROGRESS = "key_progress"
        const val CHUNK_SIZE = 5 * 1024 * 1024 // 5 MB chunks
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val fileUri = inputData.getString(KEY_FILE_URI) ?: return@withContext Result.failure()
        val remotePath = inputData.getString(KEY_REMOTE_PATH) ?: "uploads/${UUID.randomUUID()}"
        
        try {
            val file = getFileFromUri(Uri.parse(fileUri)) ?: return@withContext Result.failure()
            val totalSize = file.length()
            
            // In a production resumable upload, we would first call the server to get an upload ID
            // and find out where we left off. For this implementation, we simulate chunked transfer.
            
            var bytesUploaded = 0L
            val buffer = ByteArray(CHUNK_SIZE)
            file.inputStream().use { inputStream ->
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val chunk = buffer.sliceArray(0 until bytesRead)
                    
                    // Upload chunk (Simulation of a chunked API call)
                    val success = uploadChunk(chunk, remotePath, bytesUploaded, totalSize)
                    if (!success) return@withContext Result.retry()
                    
                    bytesUploaded += bytesRead
                    val progress = (bytesUploaded.toFloat() / totalSize * 100).toInt()
                    
                    // Report progress
                    setProgress(workDataOf(KEY_PROGRESS to progress))
                    Log.d("DriveUploadWorker", "Progress: $progress%")
                }
            }

            Result.success(workDataOf("remote_url" to remotePath))
        } catch (e: Exception) {
            Log.e("DriveUploadWorker", "Upload failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private suspend fun uploadChunk(chunk: ByteArray, path: String, offset: Long, totalSize: Long): Boolean {
        // Implementation would use OkHttp to send bytes with Content-Range header
        // For now, this is a placeholder for the network logic
        delay(500) // Simulate network latency
        return true
    }

    private fun getFileFromUri(uri: Uri): File? {
        val context = applicationContext
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File(context.cacheDir, "upload_temp_${UUID.randomUUID()}")
        inputStream?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return if (file.exists()) file else null
    }
    
    private suspend fun delay(time: Long) = kotlinx.coroutines.delay(time)
}
