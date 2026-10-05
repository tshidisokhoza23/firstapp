package com.tpsk.tj.media.data.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tpsk.tj.media.data.api.CreatePostRequest
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.media.data.local.dao.PostDao
import com.tpsk.tj.media.util.MediaProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLConnection
import java.util.UUID

@HiltWorker
class PostUploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val socialApi: SocialApi,
    private val mediaApi: com.tpsk.tj.media.data.api.MediaApi,
    private val postDao: PostDao
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_POST_ID = "POST_ID"
        const val KEY_TEXT = "TEXT"
        const val KEY_IMAGE_URIS = "IMAGE_URIS"
        const val KEY_POST_TYPE = "POST_TYPE"
        const val KEY_PARENT_POST_ID = "PARENT_POST_ID"
        const val KEY_ROOT_POST_ID = "ROOT_POST_ID"
        const val KEY_REPOST_OF_ID = "REPOST_OF_ID"
        const val KEY_LOCATION = "LOCATION"
        const val KEY_POLL_OPTIONS = "POLL_OPTIONS"

        fun enqueue(
            context: Context,
            clientPostId: String,
            text: String,
            imageUris: List<String> = emptyList(),
            postType: String = "standard",
            parentPostId: String? = null,
            rootPostId: String? = null,
            repostOfId: String? = null,
            location: String? = null,
            pollOptions: List<String>? = null
        ) {
            val inputData = Data.Builder()
                .putString(KEY_POST_ID, clientPostId)
                .putString(KEY_TEXT, text)
                .putStringArray(KEY_IMAGE_URIS, imageUris.toTypedArray())
                .putString(KEY_POST_TYPE, postType)
                .putString(KEY_PARENT_POST_ID, parentPostId)
                .putString(KEY_ROOT_POST_ID, rootPostId)
                .putString(KEY_REPOST_OF_ID, repostOfId)
                .putString(KEY_LOCATION, location)
                .apply {
                    if (!pollOptions.isNullOrEmpty()) {
                        putStringArray(KEY_POLL_OPTIONS, pollOptions.toTypedArray())
                    }
                }
                .build()

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<PostUploadWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, WorkRequest.MIN_BACKOFF_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "post_upload_$clientPostId",
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        val clientPostId = inputData.getString(KEY_POST_ID) ?: UUID.randomUUID().toString()
        val text = inputData.getString(KEY_TEXT) ?: ""
        val uriStrings = inputData.getStringArray(KEY_IMAGE_URIS) ?: emptyArray()
        val postType = inputData.getString(KEY_POST_TYPE) ?: "standard"
        val parentPostId = inputData.getString(KEY_PARENT_POST_ID)
        val rootPostId = inputData.getString(KEY_ROOT_POST_ID)
        val repostOfId = inputData.getString(KEY_REPOST_OF_ID)
        val location = inputData.getString(KEY_LOCATION)
        val pollOptions = inputData.getStringArray(KEY_POLL_OPTIONS)?.toList()

        val processedFiles = mutableListOf<File>()

        return try {
            Log.d("PostUploadWorker", "Starting background worker for post: $clientPostId")

            // 1. Process all attached images: downsample, strip EXIF metadata & upload to server
            val processedImageUrls = mutableListOf<String>()
            var firstThumb: String? = null
            for (uriStr in uriStrings) {
                if (uriStr.startsWith("http:") || uriStr.startsWith("https:") || uriStr.startsWith("uploads/")) {
                    processedImageUrls.add(uriStr)
                } else {
                    val uri = Uri.parse(uriStr)
                    val cleanFile = MediaProcessor.processAndStripExif(applicationContext, uri)
                    processedFiles.add(cleanFile)

                    // Upload EXIF-stripped clean file to server upload endpoint
                    val mimeType = URLConnection.guessContentTypeFromName(cleanFile.name) ?: "image/jpeg"
                    val requestFile = cleanFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    val body = MultipartBody.Part.createFormData("file", cleanFile.name, requestFile)
                    val folderBody = "posts".toRequestBody("text/plain".toMediaTypeOrNull())

                    val uploadResponse = mediaApi.uploadMedia(body, folderBody)
                    if (uploadResponse.isSuccessful && uploadResponse.body() != null) {
                        val uploaded = uploadResponse.body()!!
                        val serverMediaUrl = uploaded.url
                        Log.d("PostUploadWorker", "Uploaded clean image to server: $serverMediaUrl")
                        processedImageUrls.add(serverMediaUrl)
                        if (firstThumb.isNullOrBlank()) {
                            firstThumb = uploaded.thumbnailUrl
                        }
                    } else {
                        Log.e("PostUploadWorker", "Media upload failed with code ${uploadResponse.code()}")
                        throw IllegalStateException("Image upload failed with status code ${uploadResponse.code()}")
                    }
                }
            }

            // 2. Build CreatePostRequest with clientPostId as Idempotency key
            val request = CreatePostRequest(
                content = text,
                media_urls = processedImageUrls,
                post_type = postType,
                location = location,
                tags = emptyList(),
                poll_options = pollOptions,
                parent_post_id = parentPostId,
                root_post_id = rootPostId,
                repost_of_id = repostOfId,
                thumbnail_url = firstThumb
            )

            // 3. Post payload to backend
            val response = socialApi.createPost(request)
            if (response.isSuccessful) {
                Log.d("PostUploadWorker", "Post $clientPostId uploaded successfully")
                Result.success(workDataOf("POST_ID" to clientPostId))
            } else {
                Log.e("PostUploadWorker", "Post $clientPostId upload failed with code ${response.code()}")
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        } catch (e: Exception) {
            Log.e("PostUploadWorker", "Exception uploading post $clientPostId", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        } finally {
            // Clean up temporary compressed/EXIF-stripped files from internal cache
            processedFiles.forEach { file ->
                if (file.exists()) file.delete()
            }
        }
    }
}
