package com.tpsk.tj.mingle.data.repository

import android.util.Log
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.mingle.data.api.*
import com.tpsk.tj.mingle.domain.repository.MingleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class MingleRepositoryImpl @Inject constructor(
    private val api: MingleApi,
    private val chatDaoProvider: javax.inject.Provider<com.tpsk.tj.chat.data.local.dao.ChatDao>
) : MingleRepository {

    override fun getMe(): Flow<Resource<MingleProfileDto>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getMe()
            val profile = response.body()?.profile
            if (response.isSuccessful && profile != null) {
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to load mingle profile")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to load mingle profile"))
        }
    }

    override fun updateProfile(name: String?, bio: String?, age: Int?, gender: String?, lookingFor: String?, targetAgeRange: String?): Flow<Resource<MingleProfileDto>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.updateProfile(
                MingleUpdateProfileRequest(
                    name = name,
                    bio = bio,
                    age = age,
                    gender = gender,
                    looking_for = lookingFor,
                    target_age_range = targetAgeRange
                )
            )
            val profile = response.body()?.profile
            if (response.isSuccessful && profile != null) {
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to update profile")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to update profile"))
        }
    }

    override fun updateLocation(lat: Double, lng: Double, radiusKm: Int): Flow<Resource<MingleProfileDto>> = flow {
        try {
            val response = api.updateLocation(MingleLocationRequest(lat, lng, radiusKm))
            val profile = response.body()?.profile
            if (response.isSuccessful && profile != null) {
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to update location")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to update location"))
        }
    }

    override fun getFeed(lat: Double, lng: Double, radiusKm: Int, offset: Int): Flow<Resource<MingleFeedResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getFeed(lat, lng, radiusKm, offset = offset)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                emit(Resource.Success(body))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to load people nearby")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to load people nearby"))
        }
    }

    override fun swipe(targetUserId: String, action: String, candidateName: String?): Flow<Resource<MingleSwipeResultDto>> = flow {
        try {
            val response = api.swipe(MingleSwipeRequest(target_user_id = targetUserId, action = action))
            val body = response.body()
            if (response.code() == 402) {
                emit(Resource.Error("INSUFFICIENT_POINTS", lastData = body))
                return@flow
            }
            if (response.isSuccessful && body != null) {
                val conversationId = body.conversation_id
                if (body.matched && !conversationId.isNullOrBlank()) {
                    saveMingleChatLocally(conversationId, candidateName, targetUserId, "You matched on Mingle")
                }
                emit(Resource.Success(body))
            } else if (response.code() >= 500) {
                emit(Resource.Error("RETRYABLE"))
            } else {
                val err = parseError(body?.error ?: response.errorBody()?.string(), "Swipe failed")
                emit(Resource.Error(err))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(if (e is IOException) "RETRYABLE" else (e.message ?: "Swipe failed")))
        }
    }

    override fun syncSwipes(swipes: List<MingleSwipeSyncItemDto>): Flow<Resource<MingleSwipeSyncResponse>> = flow {
        try {
            val response = api.syncSwipes(MingleSwipeSyncRequest(swipes = swipes))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                for (item in body.results.orEmpty()) {
                    if (item.matched && !item.conversation_id.isNullOrBlank()) {
                        saveMingleChatLocally(item.conversation_id, null, item.target_user_id, "You matched on Mingle")
                    }
                }
                emit(Resource.Success(body))
            } else {
                emit(Resource.Error("Failed to sync swipes"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Sync failed"))
        }
    }

    override fun rewind(): Flow<Resource<MingleRewindResultDto>> = flow {
        try {
            val response = api.rewind()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                emit(Resource.Success(body))
            } else {
                emit(Resource.Error("Unable to rewind swipe"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to rewind swipe"))
        }
    }

    private fun parseError(raw: String?, fallback: String): String {
        if (raw.isNullOrBlank()) return fallback
        return try {
            val json = org.json.JSONObject(raw)
            val extracted = when {
                json.has("error") -> json.getString("error")
                json.has("message") -> json.getString("message")
                else -> ""
            }
            if (extracted.isNotBlank() && !extracted.contains("mingle request failed", ignoreCase = true)) {
                extracted
            } else {
                fallback
            }
        } catch (_: Exception) {
            if (raw.length < 80 && !raw.startsWith("{")) raw else fallback
        }
    }

    override fun uploadPhoto(file: File): Flow<Resource<MinglePhotoDto>> = flow {
        emit(Resource.Loading())
        try {
            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("photo", file.name, requestFile)
            val response = api.uploadPhoto(body)
            val photo = response.body()?.photo
            if (response.isSuccessful && photo != null) {
                emit(Resource.Success(photo))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to upload photo")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to upload photo"))
        }
    }

    override fun deletePhoto(photoId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = api.deletePhoto(photoId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to delete photo")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to delete photo"))
        }
    }

    override fun getActivity(): Flow<Resource<List<MingleActivityDto>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getActivity()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.items ?: emptyList()))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to load activity")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to load activity"))
        }
    }

    override fun openChat(userId: String, candidateName: String?): Flow<Resource<String>> = flow {
        try {
            val response = api.openChat(userId)
            val conversationId = response.body()?.conversation_id
            if (response.isSuccessful && !conversationId.isNullOrBlank()) {
                saveMingleChatLocally(conversationId, candidateName, userId, "You matched on Mingle")
                emit(Resource.Success(conversationId))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Match required before chatting"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to open Mingle chat"))
        }
    }

    override fun sendIntro(recipientId: String, message: String): Flow<Resource<MingleIntroDto>> = flow {
        try {
            val response = api.sendIntro(MingleIntroRequest(recipient_id = recipientId, message = message))
            val body = response.body()
            if (response.code() == 402) {
                emit(Resource.Error("INSUFFICIENT_POINTS", lastData = body))
                return@flow
            }
            if (response.isSuccessful && body != null) {
                emit(Resource.Success(body))
            } else {
                emit(Resource.Error(parseError(body?.error ?: response.errorBody()?.string(), "Unable to send request")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to send request"))
        }
    }

    override fun listIntros(): Flow<Resource<List<MingleIntroDto>>> = flow {
        try {
            val response = api.listIntros()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.items ?: emptyList()))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), "Unable to load requests")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to load requests"))
        }
    }

    override fun respondIntro(introId: String, action: String, senderName: String?): Flow<Resource<MingleIntroDto>> = flow {
        try {
            val response = api.respondIntro(introId, MingleIntroRespondRequest(action = action))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                val conversationId = body.conversation_id
                if (action == "accept" && !conversationId.isNullOrBlank()) {
                    saveMingleChatLocally(conversationId, senderName, body.sender_id.orEmpty(), body.message ?: "Message request")
                }
                emit(Resource.Success(body))
            } else {
                emit(Resource.Error(parseError(body?.error ?: response.errorBody()?.string(), "Unable to update request")))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to update request"))
        }
    }

    override fun extendFeed(method: String): Flow<Resource<MingleEconomyResult>> = economyCall("Unable to extend the feed") {
        api.extendFeed(mapOf("method" to method))
    }

    override fun boost(): Flow<Resource<MingleEconomyResult>> = economyCall("Unable to boost this profile") {
        api.boost()
    }

    override fun unlockAdmirer(userId: String): Flow<Resource<MingleEconomyResult>> = economyCall("Unable to unlock this like") {
        api.unlockAdmirer(userId)
    }

    private fun economyCall(
        fallback: String,
        call: suspend () -> retrofit2.Response<MingleEconomyResult>
    ): Flow<Resource<MingleEconomyResult>> = flow {
        try {
            val response = call()
            val body = response.body()
            if (response.code() == 402) {
                emit(Resource.Error("INSUFFICIENT_POINTS", lastData = body))
                return@flow
            }
            if (response.code() == 404) {
                emit(Resource.Error("This action is not on the server this app is using"))
                return@flow
            }
            if (response.isSuccessful && body != null) {
                emit(Resource.Success(body))
            } else {
                emit(Resource.Error(parseError(response.errorBody()?.string(), fallback)))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: fallback))
        }
    }

    private suspend fun saveMingleChatLocally(conversationId: String, candidateName: String?, targetUserId: String, lastMessage: String) {
        if (conversationId.isBlank()) return
        try {
            val chatDao = chatDaoProvider.get()
            val existing = chatDao.searchChatsById(conversationId)
            val nameToUse = candidateName.orEmpty().ifBlank { existing?.senderName.orEmpty().ifBlank { "Mingle User" } }
            val chatEntity = com.tpsk.tj.chat.data.local.entity.ChatEntity(
                id = conversationId,
                type = com.tpsk.tj.common.model.ConversationType.MINGLE,
                senderName = nameToUse,
                lastMessage = existing?.lastMessage ?: "You matched on Mingle ❤️",
                timestamp = System.currentTimeMillis(),
                unreadCount = existing?.unreadCount ?: 0,
                avatarUrl = existing?.avatarUrl,
                isOnline = false,
                isMuted = false,
                isPinned = false,
                isTyping = false,
                otherUserId = targetUserId
            )
            chatDao.insertChats(listOf(chatEntity))
        } catch (error: Exception) {
            Log.e("MingleRepository", "Unable to cache matched Mingle chat $conversationId", error)
        }
    }
}
