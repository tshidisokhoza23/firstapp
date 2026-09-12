package com.example.firstapp.mingle.data.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.mingle.data.api.*
import com.example.firstapp.mingle.domain.repository.MingleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

class MingleRepositoryImpl @Inject constructor(
    private val api: MingleApi
) : MingleRepository {

    override fun getMe(): Flow<Resource<MingleProfileDto>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getMe()
            val profile = response.body()?.profile
            if (response.isSuccessful && profile != null) {
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error(response.errorBody()?.string() ?: "Unable to load mingle profile"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to load mingle profile"))
        }
    }

    override fun updateLocation(lat: Double, lng: Double, radiusKm: Int): Flow<Resource<MingleProfileDto>> = flow {
        try {
            val response = api.updateLocation(MingleLocationRequest(lat, lng, radiusKm))
            val profile = response.body()?.profile
            if (response.isSuccessful && profile != null) {
                emit(Resource.Success(profile))
            } else {
                emit(Resource.Error("Unable to update location"))
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
                emit(Resource.Error(response.errorBody()?.string() ?: "Unable to load people nearby"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to load people nearby"))
        }
    }

    override fun swipe(targetUserId: String, action: String): Flow<Resource<MingleSwipeResultDto>> = flow {
        try {
            val response = api.swipe(MingleSwipeRequest(target_user_id = targetUserId, action = action))
            val body = response.body()
            if (response.code() == 402) {
                emit(Resource.Error("INSUFFICIENT_POINTS", lastData = body))
                return@flow
            }
            if (response.isSuccessful && body != null) {
                emit(Resource.Success(body))
            } else {
                emit(Resource.Error(body?.error ?: response.errorBody()?.string() ?: "Swipe failed"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Swipe failed"))
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
                emit(Resource.Error(response.errorBody()?.string() ?: "Unable to upload photo"))
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
                emit(Resource.Error("Unable to delete photo"))
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
                emit(Resource.Error("Unable to load activity"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to load activity"))
        }
    }

    override fun openChat(userId: String): Flow<Resource<String>> = flow {
        try {
            val response = api.openChat(userId)
            val conversationId = response.body()?.conversation_id
            if (response.isSuccessful && !conversationId.isNullOrBlank()) {
                emit(Resource.Success(conversationId))
            } else {
                emit(Resource.Error(response.body()?.error ?: "Match required before chatting"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error(e.message ?: "Unable to open chat"))
        }
    }
}
