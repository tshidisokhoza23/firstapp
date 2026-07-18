package com.example.firstapp.media.data.repository

import androidx.compose.ui.graphics.Color
import com.example.firstapp.common.data.local.dao.ShortDao
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.*
import com.example.firstapp.common.data.api.*
import com.example.firstapp.common.data.repository.*
import com.example.firstapp.media.data.repository.*
import com.example.firstapp.media.model.ShortMedia
import com.example.firstapp.media.model.ShortReply
import com.example.firstapp.media.domain.repository.ShortsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map as flowMap
import javax.inject.Inject
import kotlin.collections.*

class ShortsRepositoryImpl @Inject constructor(
    private val mediaApi: MediaApi,
    private val socialApi: SocialApi,
    private val shortDao: ShortDao
) : ShortsRepository {

    override fun getShorts(): Flow<Resource<List<ShortMedia>>> = flow {
        val cached = shortDao.getShorts().first()
        val mappedCached = cached.map { it.toShortMedia() }
        emit(Resource.Loading(mappedCached))
        try {
            val response = mediaApi.getShortsFeed(page = 1)
            if (response.isSuccessful) {
                val feedItems = response.body()?.items ?: emptyList()
                val shorts = mapShortsFeedItems(feedItems)
                shortDao.clearShorts()
                shortDao.insertShorts(shorts.map { it.toShortEntity() })
            } else {
                android.util.Log.e("ShortsRepo", "API error: ${response.code()}")
            }
        } catch (e: Exception) {
            android.util.Log.e("ShortsRepo", "Sync failed: ${e.message}")
            val cachedFromDb = shortDao.getShorts().first()
            if (cachedFromDb.isEmpty()) {
                val mockShorts = listOf(
                    ShortMedia(
                        id = "mock-1",
                        creatorId = "official",
                        creatorName = "FirstApp_Official",
                        description = "Welcome to Shorts! Explore amazing content.",
                        likes = "1.2K",
                        comments = "45",
                        shares = "12",
                        videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4",
                        thumbnailUrl = "https://images.pexels.com/photos/2582818/pexels-photo-2582818.jpeg",
                        thumbnailColor = Color.DarkGray,
                        profilePictureColor = Color.Blue,
                        isLiked = false,
                        isSubscribed = false
                    )
                )
                emit(Resource.Success(mockShorts))
                return@flow
            }
        }

        emitAll(shortDao.getShorts().flowMap { entities ->
            val models = entities.map { it.toShortMedia() }
            Resource.Success<List<ShortMedia>>(models)
        })
    }

    override fun getUserShorts(userId: String): Flow<Resource<List<ShortMedia>>> = flow {
        val cached = shortDao.getShorts().first().filter { it.creatorId == userId }.map { it.toShortMedia() }
        emit(Resource.Loading(cached))
        try {
            val response = mediaApi.getShortsByUserId(userId)
            if (response.isSuccessful) {
                val apiShorts = response.body()?.shorts ?: emptyList()
                val mapped = apiShorts.map { apiShort -> 
                    ShortMedia(
                        id = apiShort.id, 
                        creatorId = apiShort.creator_id,
                        creatorName = apiShort.creator_name,
                        creatorUsername = apiShort.creator_username ?: "",
                        creatorAvatarUrl = formatUrl(apiShort.creator_avatar_url),
                        description = apiShort.description, 
                        likes = apiShort.likes.toString(), 
                        comments = apiShort.comments.toString(), 
                        shares = apiShort.shares.toString(), 
                        videoUrl = formatUrl(apiShort.video_url), 
                        thumbnailUrl = formatUrl(apiShort.thumbnail_url), 
                        thumbnailColor = Color.Black, 
                        profilePictureColor = Color.Gray, 
                        isLiked = apiShort.is_liked, 
                        isSubscribed = apiShort.is_subscribed,
                        isBookmarked = apiShort.is_bookmarked
                    )
                }
                shortDao.insertShorts(mapped.map { it.toShortEntity() })
            }
        } catch (_: Exception) {}

        emitAll(shortDao.getShorts().flowMap { entities ->
            val models = entities.filter { it.creatorId == userId }.map { it.toShortMedia() }
            Resource.Success<List<ShortMedia>>(models)
        })
    }

    override fun searchShorts(query: String): Flow<Resource<List<ShortMedia>>> = flow {
        emit(Resource.Loading())
        try {
            val response = mediaApi.searchShorts(query)
            if (response.isSuccessful) {
                val apiShorts = response.body()?.shorts ?: emptyList()
                val mapped = apiShorts.map { apiShort -> 
                    ShortMedia(
                        id = apiShort.id, 
                        creatorId = apiShort.creator_id,
                        creatorName = apiShort.creator_name,
                        creatorUsername = apiShort.creator_username ?: "",
                        creatorAvatarUrl = formatUrl(apiShort.creator_avatar_url),
                        description = apiShort.description, 
                        likes = apiShort.likes.toString(), 
                        comments = apiShort.comments.toString(), 
                        shares = apiShort.shares.toString(), 
                        videoUrl = formatUrl(apiShort.video_url), 
                        thumbnailUrl = formatUrl(apiShort.thumbnail_url),
                        thumbnailColor = Color.Black, 
                        profilePictureColor = Color.Gray, 
                        isLiked = apiShort.is_liked, 
                        isSubscribed = apiShort.is_subscribed,
                        isBookmarked = apiShort.is_bookmarked
                    ) 
                }
                emit(Resource.Success<List<ShortMedia>>(mapped))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<String>> = flow {
        try {
            emit(Resource.Loading())
            val response = mediaApi.createShort(CreateShortRequest(description, videoUrl, thumbnailUrl))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.id))
            } else {
                emit(Resource.Error("Create short failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Error creating short"))
        }
    }

    override fun repostShort(shortId: String): Flow<Resource<Boolean>> = flow {
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

    override fun bookmarkShort(shortId: String): Flow<Resource<Boolean>> = flow {
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

    override fun trackShortShare(shortId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (mediaApi.trackShortShare(shortId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun replyToShort(shortId: String, content: String, parentReplyId: String?): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (mediaApi.replyToShort(shortId, CreateShortReplyRequest(content, parentReplyId)).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun getShortReplies(shortId: String): Flow<Resource<List<ShortReply>>> = flow {
        emit(Resource.Loading())
        try {
            val response = mediaApi.getShortReplies(shortId)
            if (response.isSuccessful) {
                val repliesList = response.body()?.replies ?: emptyList()
                val mapped = repliesList.map { mapApiShortReplyToShortReply(it) }
                emit(Resource.Success<List<ShortReply>>(mapped))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun likeShort(shortId: String): Flow<Resource<ShortLikeResponse>> = flow {
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

    override fun likeShortReply(replyId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (mediaApi.likeShortReply(replyId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun subscribeToShortCreator(shortId: String): Flow<Resource<ShortSubscribeResponse>> = flow {
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
