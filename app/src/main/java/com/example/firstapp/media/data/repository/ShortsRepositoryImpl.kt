package com.example.firstapp.media.data.repository

import androidx.compose.ui.graphics.Color
import com.example.firstapp.common.data.local.dao.ShortDao
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.MediaApi
import com.example.firstapp.media.data.api.CreateShortRequest
import com.example.firstapp.media.data.api.CreateShortReplyRequest
import com.example.firstapp.media.data.api.ShortLikeResponse
import com.example.firstapp.media.data.api.ShortSubscribeResponse
import com.example.firstapp.media.data.repository.toEntity
import com.example.firstapp.media.data.repository.toModel
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.model.ShortReply
import com.example.firstapp.media.domain.repository.ShortsRepository
import kotlinx.coroutines.flow.*

import javax.inject.Inject

class ShortsRepositoryImpl @Inject constructor(
    private val mediaApi: MediaApi,
    private val shortDao: ShortDao
) : ShortsRepository {

    override fun getShorts(): Flow<Resource<List<ShortMedia>>> = flow {
        val cached = shortDao.getShorts().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = mediaApi.getShorts()
            if (response.isSuccessful) {
                val apiShorts = response.body()?.shorts ?: emptyList()
                val shorts = apiShorts.map { apiShort -> 
                    ShortMedia(
                        id = apiShort.id, 
                        creatorName = apiShort.creator_name, 
                        description = apiShort.description, 
                        likes = apiShort.likes.toString(), 
                        comments = apiShort.comments.toString(), 
                        shares = apiShort.shares.toString(), 
                        videoUrl = apiShort.video_url ?: "", 
                        thumbnailUrl = apiShort.thumbnail_url, 
                        thumbnailColor = Color.Black, 
                        profilePictureColor = Color.Gray, 
                        isLiked = apiShort.is_liked, 
                        isSubscribed = apiShort.is_subscribed
                    ) 
                }
                shortDao.clearShorts()
                shortDao.insertShorts(shorts.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        shortDao.getShorts().collect { entities ->
            emit(Resource.Success(entities.map { it.toModel() }))
        }
    }

    override fun searchShorts(query: String): Flow<Resource<List<ShortMedia>>> = flow {
        emit(Resource.Loading())
        try {
            val response = mediaApi.searchShorts(query)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.shorts?.map { apiShort -> 
                    ShortMedia(
                        id = apiShort.id, 
                        creatorName = apiShort.creator_name, 
                        description = apiShort.description, 
                        likes = apiShort.likes.toString(), 
                        comments = apiShort.comments.toString(), 
                        shares = apiShort.shares.toString(), 
                        videoUrl = apiShort.video_url ?: "", 
                        thumbnailUrl = apiShort.thumbnail_url, 
                        thumbnailColor = Color.Black, 
                        profilePictureColor = Color.Gray, 
                        isLiked = apiShort.is_liked, 
                        isSubscribed = apiShort.is_subscribed
                    ) 
                } ?: emptyList()))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<Long>> = flow {
        try {
            emit(Resource.Loading())
            val response = mediaApi.createShort(CreateShortRequest(description, videoUrl, thumbnailUrl))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.id))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }

    override fun repostShort(shortId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (mediaApi.repostShort(shortId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun bookmarkShort(shortId: Int): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (mediaApi.bookmarkShort(shortId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun replyToShort(shortId: Int, content: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (mediaApi.replyToShort(shortId, CreateShortReplyRequest(content)).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun getShortReplies(shortId: Int): Flow<Resource<List<ShortReply>>> = flow {
        emit(Resource.Loading())
        try {
            val response = mediaApi.getShortReplies(shortId)
            if (response.isSuccessful) {
                emit(Resource.Success(response.body()?.replies?.map { 
                    ShortReply(
                        it.id, 
                        it.author?.display_name ?: "Unknown", 
                        it.author?.username ?: "", 
                        it.author?.avatar_url, 
                        Color.Gray, 
                        it.author?.is_verified ?: false, 
                        it.content, 
                        it.created_at ?: "", 
                        it.likes.toString(), 
                        it.is_liked
                    ) 
                } ?: emptyList()))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun likeShort(shortId: Int): Flow<Resource<ShortLikeResponse>> = flow {
        try {
            val response = mediaApi.likeShort(shortId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }

    override fun subscribeToShortCreator(shortId: Int): Flow<Resource<ShortSubscribeResponse>> = flow {
        try {
            val response = mediaApi.subscribeToShortCreator(shortId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Error"))
        }
    }
}
