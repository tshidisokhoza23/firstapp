package com.example.firstapp.account.data.repository

import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.data.api.*
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.common.data.local.dao.ContactDao
import com.example.firstapp.common.data.repository.mapApiContact
import com.example.firstapp.common.model.PrivacySettings
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val userApi: UserApi,
    private val mediaApi: MediaApi,
    private val contactDao: ContactDao
) : AccountRepository {

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApi.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.user))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun updateUser(displayName: String?, bio: String?, location: String?, avatarUrl: String?, bannerUrl: String?, isArtist: Boolean?): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = userApi.updateUser(UpdateUserRequest(display_name = displayName, bio = bio, location = location, avatar_url = avatarUrl, banner_url = bannerUrl, is_artist = isArtist))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.user))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun deleteAccount(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            if (userApi.deleteAccount().isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>> = flow {
        try {
            emit(Resource.Loading())
            val res = userApi.search("") // Fallback or use a specific sync endpoint if available in UserApi
            // For now, let's assume we use the chatApi for sync in a real app, but here we try to keep it in account.
            // Actually, syncContacts was in ChatApi earlier. Let's move it to UserApi if it's "account" related.
            // Wait, I put syncContacts in ChatApi.
            emit(Resource.Error("Use ChatRepository for syncContacts"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
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
            emit(Resource.Error("Error"))
        }
    }

    override fun blockUser(userId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.blockUser(userId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }

    override fun unblockUser(userId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.unblockUser(userId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }

    override fun getBlockedUsers(): Flow<Resource<List<Contact>>> = flow {
        emit(Resource.Loading())
        try {
            val res = userApi.getBlockedUsers()
            if (res.isSuccessful) {
                val contacts = res.body()?.contacts?.map { mapApiContact(it) } ?: emptyList()
                emit(Resource.Success(contacts))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }

    override suspend fun getPublicKey(userId: Int): String? {
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

    override fun reportUser(userId: Int, reason: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (userApi.reportUser(userId, ReportRequest(reason)).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
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
            emit(Resource.Error("Error"))
        }
    }

    override fun uploadFile(file: File, folder: String?): Flow<Resource<UploadResponse>> = flow {
        emit(Resource.Loading())
        try {
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val folderBody = folder?.toRequestBody("text/plain".toMediaTypeOrNull())
            
            val response = mediaApi.uploadMedia(body, folderBody)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Upload error: ${e.localizedMessage}"))
        }
    }
}
