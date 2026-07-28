package com.example.firstapp.media.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map as pagingMap
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.common.data.local.dao.PostDao
import com.example.firstapp.common.data.local.dao.StoryDao
import com.example.firstapp.common.data.local.entity.StoryEntity
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.paging.PostRemoteMediator
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.rewards.data.RewardsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map as flowMap
import javax.inject.Inject
import kotlin.collections.*

class SocialRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val postDao: PostDao,
    private val storyDao: StoryDao,
    private val appDatabase: AppDatabase,
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver
) : SocialRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getPostsPaging(following: Boolean, authorId: String, feedType: String): Flow<PagingData<Post>> {
        val pagingSourceFactory = { postDao.getPostsPaging(feedType) }
        
        return if (authorId == "") {
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                remoteMediator = PostRemoteMediator(socialApi, appDatabase, following, "", feedType),
                pagingSourceFactory = pagingSourceFactory
            ).flow.flowMap { pagingData ->
                pagingData.pagingMap { entity -> entity.toPost() }
            }
        } else {
            Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = { 
                    com.example.firstapp.media.data.paging.PostPagingSource(socialApi, following = false, authorId = authorId, feedType = feedType)
                }
            ).flow
        }
    }

    override suspend fun updatePostLikeLocally(postId: String, likes: Long, isLiked: Boolean) {
        postDao.updateLikeStatus(postId, likes, isLiked)
    }

    override suspend fun updatePostRepostLocally(postId: String, shares: Long, isReposted: Boolean) {
        postDao.updateRepostStatus(postId, shares, isReposted)
    }

    override suspend fun updatePostBookmarkLocally(postId: String, isBookmarked: Boolean) {
        postDao.updateBookmarkStatus(postId, isBookmarked)
    }

    override fun getUserPosts(userId: String, feedType: String, page: Int): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.getPosts(page = page, following = false, authorId = userId, feedType = feedType)
            if (response.isSuccessful) {
                val apiPosts = response.body() ?: emptyList<com.example.firstapp.media.data.api.ApiPost>()
                val posts = apiPosts.map { apiPost: com.example.firstapp.media.data.api.ApiPost -> mapApiPost(apiPost) }
                emit(Resource.Success(posts))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error: ${e.localizedMessage}"))
        }
    }

    override fun getPosts(page: Int, feedType: String): Flow<Resource<List<Post>>> = flow {
        val cachedEntities = postDao.getPosts(feedType).first()
        val cached = cachedEntities.map { it.toPost() }
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        } else {
            emit(Resource.Loading())
        }
        
        try {
            val response = socialApi.getPosts(page = page, following = false, authorId = "", feedType = feedType)
            if (response.isSuccessful) {
                val apiPosts = response.body() ?: emptyList<com.example.firstapp.media.data.api.ApiPost>()
                Log.d("SocialRepo", "Fetched ${apiPosts.size} posts for page $page")
                val posts = apiPosts.map { apiPost: com.example.firstapp.media.data.api.ApiPost -> mapApiPost(apiPost) }
                // Avoid wiping media feed cache when legacy /posts returns empty.
                if (posts.isNotEmpty()) {
                    if (page == 1) postDao.clearPosts(feedType)
                    postDao.insertPosts(posts.map { it.toPostEntity(feedType) })
                }
            } else {
                Log.e("SocialRepo", "getPosts failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("SocialRepositoryImpl", "getPosts failed with exception", e)
        }
        
        emitAll(postDao.getPosts(feedType).flowMap { entities ->
            val models = entities.map { it.toPost() }
            Resource.Success<List<Post>>(models)
        })
    }

    override fun getFollowingPosts(page: Int, feedType: String): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.getPosts(page = page, following = true, authorId = "", feedType = feedType)
            if (response.isSuccessful) {
                val apiPosts = response.body() ?: emptyList<com.example.firstapp.media.data.api.ApiPost>()
                val posts = apiPosts.map { apiPost: com.example.firstapp.media.data.api.ApiPost -> mapApiPost(apiPost) }
                emit(Resource.Success(posts))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun getChannelPosts(channelId: String, page: Int, feedType: String): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.getPosts(page = page, following = false, channelId = channelId, authorId = "", feedType = feedType)
            if (response.isSuccessful) {
                val apiPosts = response.body() ?: emptyList<com.example.firstapp.media.data.api.ApiPost>()
                val posts = apiPosts.map { apiPost: com.example.firstapp.media.data.api.ApiPost -> mapApiPost(apiPost) }
                emit(Resource.Success(posts))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun createPost(content: String, mediaUrls: List<String>, postType: String?, tags: List<String>, channelId: String?, pollOptions: List<String>?, location: String?, mediaType: String?): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val pollRequest = if (pollOptions != null && pollOptions.size > 0) {
                com.example.firstapp.media.data.api.CreatePollRequest(options = pollOptions)
            } else null
            
            val request = com.example.firstapp.media.data.api.CreatePostRequest(
                content = content,
                media_urls = mediaUrls,
                post_type = postType ?: "text_only",
                media_type = mediaType,
                location = location,
                tags = tags,
                channel_id = channelId,
                poll = pollRequest
            )
            if (socialApi.createPost(request).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Offline"))
        }
    }
    
    override fun likePost(postId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = socialApi.likePost(postId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Offline"))
        }
    }

    override fun repostPost(postId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.repostPost(postId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun bookmarkPost(postId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.bookmarkPost(postId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    
    override fun getReplies(postId: String): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.getReplies(postId)
            if (response.isSuccessful && response.body() != null) {
                val replies = mapMediaReplyItems(response.body()!!.items)
                emit(Resource.Success(replies))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error: ${e.localizedMessage}"))
        }
    }

    override fun createPostReply(postId: String, content: String, parentReplyId: String?): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.createPostReply(postId, com.example.firstapp.media.data.api.CreatePostReplyRequest(content, parentReplyId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun likePostReply(replyId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.likePostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun repostPostReply(replyId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.repostPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun bookmarkPostReply(replyId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.bookmarkPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun deletePostReply(replyId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.deletePost(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun votePoll(postId: String, optionId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.votePoll(postId, com.example.firstapp.media.data.api.VotePollRequest(optionId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun followUser(userId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.followUser(userId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun unfollowUser(userId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.unfollowUser(userId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun getTrendingTopics(): Flow<Resource<List<com.example.firstapp.media.data.api.TrendingTopic>>> = flow { 
        emit(Resource.Loading())
        try { 
            val response = socialApi.getTrendingTopics()
            if (response.isSuccessful) emit(Resource.Success(response.body()?.trending ?: emptyList()))
            else emit(Resource.Error("Fail")) 
        } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } 
    }
    override fun getSuggestedUsers(): Flow<Resource<List<Contact>>> = flow { 
        emit(Resource.Loading())
        try { 
            val response = socialApi.getSuggestedUsers()
            if (response.isSuccessful && response.body() != null) { 
                val users = response.body()!!.contacts
                val contacts = users.map { user: com.example.firstapp.common.data.api.ApiUser -> 
                    val userModel = com.example.firstapp.common.model.User(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        displayName = user.display_name,
                        phoneNumber = user.phone_number ?: ""
                    )
                    val resolvedName = displayNameResolver.resolve(userModel, user.phone_number ?: "")
                    
                    Contact(
                        id = user.id, 
                        username = user.username, 
                        displayName = resolvedName,
                        avatarUrl = user.avatar_url, 
                        phoneNumber = user.phone_number ?: "", 
                        isRegistered = true, 
                        isAuthenticated = true, 
                        isVerified = user.is_verified, 
                        bio = user.bio,
                        publicKey = user.public_key
                    ) 
                }
                emit(Resource.Success(contacts)) 
            } else { 
                emit(Resource.Error("Fail")) 
            } 
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Offline")) 
        } 
    }

    override fun getStories(): Flow<Resource<List<UserStory>>> = flow {
        val cachedEntities = storyDao.getStories().first()
        val cached = cachedEntities.map { it.toUserStory() }
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        } else {
            emit(Resource.Loading())
        }

        try {
            val response = socialApi.getStories()
            if (response.isSuccessful) {
                val apiStories = response.body()?.stories ?: emptyList<com.example.firstapp.media.data.api.ApiStoryResponse>()
                Log.d("SocialRepo", "Fetched ${apiStories.size} story sets")
                val stories = apiStories.map { apiStory: com.example.firstapp.media.data.api.ApiStoryResponse -> mapApiStory(apiStory) }
                storyDao.clearStories()
                storyDao.insertStories(stories.map { it.toStoryEntity() })
            } else {
                Log.e("SocialRepo", "getStories network failed: ${response.code()} ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            Log.e("SocialRepo", "getStories failed with exception", e)
        }
        
        emitAll(storyDao.getStories().flowMap { entities ->
            val models = entities.map { it.toUserStory() }.toMutableList()
            // Sort models: most recent story first
            models.sortByDescending { us: UserStory -> us.stories.maxByOrNull { s: StoryItem -> s.createdAt }?.createdAt ?: 0L }
            Resource.Success<List<UserStory>>(models)
        })
    }

    override fun createStory(content: String, contentUrl: String?, type: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = socialApi.createStory(com.example.firstapp.media.data.api.CreateStoryRequest(content, contentUrl, type))
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error: ${e.localizedMessage ?: "Connection failed"}"))
        }
    }

    override fun deleteStory(storyId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            if (socialApi.deleteStory(storyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun markStoryAsViewed(userId: String, storyId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val storiesList = storyDao.getStories().first()
            val entity = storiesList.find { it.id == userId }
            if (entity != null) {
                var awardPoints = false
                val updatedItems = entity.stories.map { item: StoryItem -> 
                    if (item.id == storyId) {
                        val sid = item.id
                        if (sid.length >= 3 && sid[0] == 'a' && sid[1] == 'd' && sid[2] == '_') {
                            awardPoints = true
                        }
                        item.copy(isViewed = true) 
                    } else item 
                }
                if (awardPoints) {
                    try {
                        RewardsManager.awardPoints(0.10, "STORY_AD", "Viewed ad story $storyId")
                    } catch (_: Exception) {}
                }
                storyDao.insertStories(listOf(entity.copy(stories = updatedItems)))
            }
            socialApi.markStoryViewed(storyId)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun muteStory(userId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val storiesList = storyDao.getStories().first()
            val entity = storiesList.find { it.id == userId }
            if (entity != null) {
                storyDao.insertStories(listOf(entity.copy(isMuted = isMuted)))
            }
            socialApi.muteUser(userId, com.example.firstapp.common.data.api.MuteRequest(isMuted))
            emit(Resource.Success(true))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error"))
        }
    }

    override fun viewPost(postId: String): Flow<Resource<Boolean>> = flow { try { socialApi.viewPost(postId); emit(Resource.Success(true)) } catch (_: Exception) {} }

    override fun trackEngagement(
        postId: String, 
        eventType: String, 
        category: String,
        watchTimeMs: Int?,
        completed: Boolean?
    ): Flow<Resource<Boolean>> = flow {
        try {
            socialApi.trackEngagement(
                com.example.firstapp.media.data.api.EngagementRequest(
                    postId, 
                    eventType, 
                    category,
                    watchTimeMs,
                    completed
                )
            )
            emit(Resource.Success(true))
        } catch (_: Exception) {}
    }

    override fun trackAdRevenue(postId: String, revenue: Double, adUnitId: String): Flow<Resource<Boolean>> = flow {
        try {
            socialApi.trackAdRevenue(com.example.firstapp.media.data.api.AdRevenueRequest(postId, revenue, adUnitId))
            emit(Resource.Success(true))
        } catch (_: Exception) {}
    }

    override fun deletePost(postId: String): Flow<Resource<Boolean>> = flow { try { socialApi.deletePost(postId); emit(Resource.Success(true)) } catch (_: Exception) {} }

    override fun getStoryViewers(storyId: String): Flow<Resource<List<com.example.firstapp.media.data.api.ApiViewer>>> = flow { emit(Resource.Loading()); try { val res = socialApi.getStoryViewers(storyId); if (res.isSuccessful) emit(Resource.Success(res.body()?.viewers ?: emptyList())) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Error")) } }

    override fun getPostById(postId: String): Flow<Resource<Post>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.getPost(postId)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(mapApiPost(response.body()!!)))
            } else {
                emit(Resource.Error("Post not found"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error: ${e.localizedMessage}"))
        }
    }
}
