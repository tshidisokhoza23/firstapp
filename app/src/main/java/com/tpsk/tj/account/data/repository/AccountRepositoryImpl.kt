@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.tpsk.tj.account.data.repository

import com.tpsk.tj.account.domain.repository.AccountRepository
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.data.api.*
import com.tpsk.tj.media.data.api.MediaApi
import com.tpsk.tj.chat.data.local.dao.ContactDao
import com.tpsk.tj.chat.data.repository.mapApiContact
import com.tpsk.tj.common.model.PrivacySettings
import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.common.util.UriRequestBody
import com.tpsk.tj.common.util.VideoTooLargeException
import com.tpsk.tj.common.util.VideoTransformerManager
import com.tpsk.tj.common.util.VideoUploadProfile
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLConnection

import javax.inject.Inject
import kotlin.collections.*

class AccountRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val mediaApi: MediaApi,
    private val contactDao: ContactDao,
    private val chatRepository: javax.inject.Provider<com.tpsk.tj.domain.repository.ChatRepository>
) : AccountRepository {

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApi.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.user))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun getUserById(userId: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApi.getUserById(userId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.user))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun updateUser(displayName: String?, bio: String?, location: String?, avatarUrl: String?, bannerUrl: String?, isArtist: Boolean?): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApi.updateUser(UpdateUserRequest(display_name = displayName, bio = bio, location = location, avatar_url = avatarUrl, banner_url = bannerUrl, is_artist = isArtist))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.user))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun deleteAccount(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (userApi.deleteAccount().isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> {
        return chatRepository.get().syncContacts(phoneNumbers)
    }

    override fun getPrivacySettings(): Flow<Resource<PrivacySettings>> = flow {
        emit(Resource.Loading())
        try {
            val res = userApi.getPrivacySettings()
            if (res.isSuccessful && res.body() != null) {
                emit(Resource.Success(res.body()!!))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun updatePrivacySettings(settings: PrivacySettings): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.updatePrivacySettings(settings).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun blockUser(userId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.blockUser(userId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun unblockUser(userId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.unblockUser(userId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun followUser(userId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.followUser(userId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun unfollowUser(userId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.unfollowUser(userId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun getFollowers(userId: String): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val res = userApi.getFollowers(userId)
            if (res.isSuccessful) {
                val contacts = res.body()?.contacts?.map { mapApiContact(it) } ?: emptyList()
                emit(Resource.Success(contacts))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun getFollowing(userId: String): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val res = userApi.getFollowing(userId)
            if (res.isSuccessful) {
                val contacts = res.body()?.contacts?.map { mapApiContact(it) } ?: emptyList()
                emit(Resource.Success(contacts))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun getBlockedUsers(): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val res = userApi.getBlockedUsers()
            if (res.isSuccessful) {
                val contacts = res.body()?.users?.map { mapApiContact(it) } ?: emptyList()
                emit(Resource.Success(contacts))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override suspend fun getPublicKey(userId: String): String? {
        return try {
            userApi.getPublicKey(userId).body()?.public_key
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun setPublicKey(publicKey: String): Boolean {
        return try {
            userApi.setPublicKey(PublicKeyResponse(publicKey)).isSuccessful
        } catch (_: Exception) {
            false
        }
    }

    override fun reportUser(userId: String, reason: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.reportUser(userId, ReportRequest(reason)).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun upsertPushToken(token: String, deviceId: String): Flow<Resource<Boolean>> = flow {
        try {
            if (userApi.upsertPushToken(PushTokenRequest(token, deviceId)).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    private fun voiceNoteMime(name: String): String? = when {
        name.endsWith(".m4a", true) || name.endsWith(".aac", true) -> "audio/mp4"
        name.endsWith(".mp3", true) -> "audio/mpeg"
        name.endsWith(".ogg", true) -> "audio/ogg"
        name.endsWith(".3gp", true) -> "audio/3gpp"
        name.endsWith(".wav", true) -> "audio/wav"
        else -> null
    }

    override fun uploadFile(file: File, folder: String?): Flow<Resource<UploadResponse>> = flow {
        emit(Resource.Loading())
        try {
            val mediaType = (URLConnection.guessContentTypeFromName(file.name)
                ?: voiceNoteMime(file.name)
                ?: "application/octet-stream").toMediaTypeOrNull()
            val requestFile = file.asRequestBody(mediaType)
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val folderBody = folder?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = withTimeout(45_000) { mediaApi.uploadMedia(body, folderBody) }
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error(uploadFailure(response.code(), response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException && e !is TimeoutCancellationException) throw e
            emit(Resource.Error("Upload error: ${e.localizedMessage ?: "Timeout"}"))
        }
    }

    override fun uploadUri(context: Context, uri: Uri, folder: String?): Flow<Resource<UploadResponse>> = flow {
        emit(Resource.Loading())
        var tempFile: File? = null
        try {
            val resolver = context.contentResolver
            val mediaType = resolver.getType(uri)?.toMediaTypeOrNull()
                ?: when {
                    uri.toString().endsWith(".mp4", ignoreCase = true) -> "video/mp4".toMediaTypeOrNull()
                    uri.toString().endsWith(".png", ignoreCase = true) -> "image/png".toMediaTypeOrNull()
                    else -> "image/jpeg".toMediaTypeOrNull()
                }

            var name: String? = null
            if (uri.scheme == "content") {
                try {
                    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) name = cursor.getString(0)
                    }
                } catch (_: Exception) {}
            }
            if (name.isNullOrBlank()) {
                name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                    ?: "upload${extensionForMediaType(mediaType?.toString())}"
            }

            val fileName = uri.lastPathSegment?.substringAfterLast('/').orEmpty()
            val alreadyCompressed = fileName.startsWith("compressed_") || fileName.startsWith("mingle_")
            var uploadTarget = uri
            val mime = mediaType?.toString().orEmpty()
            if (!alreadyCompressed && mime.startsWith("video/")) {
                val exported = VideoTransformerManager.exportForUpload(
                    context, uri, VideoUploadProfile.GENERAL
                )
                tempFile = exported
                uploadTarget = Uri.fromFile(exported)
                name = exported.name
            } else if (!alreadyCompressed && mime.startsWith("image/") && folder != "avatars") {
                MediaUtils.compressImage(context, uri)?.let { compressed ->
                    tempFile = compressed
                    uploadTarget = Uri.fromFile(compressed)
                    name = compressed.name
                }
            }

            val uploadType = when {
                tempFile != null && mime.startsWith("video/") -> "video/mp4".toMediaTypeOrNull()
                tempFile != null && mime.startsWith("image/") -> "image/jpeg".toMediaTypeOrNull()
                else -> mediaType
            }
            val body = MultipartBody.Part.createFormData(
                "file",
                name,
                UriRequestBody(resolver, uploadTarget, uploadType)
            )
            val folderBody = folder?.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = withTimeout(180_000) { mediaApi.uploadMedia(body, folderBody) }
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error(uploadFailure(response.code(), response.errorBody()?.string())))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException && e !is TimeoutCancellationException) throw e
            android.util.Log.e("AccountRepo", "uploadUri failed", e)
            val message = if (e is VideoTooLargeException) {
                e.message ?: "Video is too long or too large"
            } else {
                "Upload error: ${e.localizedMessage ?: "Timeout"}"
            }
            emit(Resource.Error(message))
        } finally {
            tempFile?.delete()
        }
    }

    private fun uploadFailure(code: Int, body: String?): String {
        val detail = body?.trim()?.take(180).orEmpty()
        return if (detail.isBlank()) "Upload failed: $code" else "Upload failed: $code $detail"
    }

    private fun extensionForMediaType(mediaType: String?): String = when {
        mediaType?.startsWith("image/") == true -> ".jpg"
        mediaType?.startsWith("video/") == true -> ".mp4"
        else -> ""
    }
}
