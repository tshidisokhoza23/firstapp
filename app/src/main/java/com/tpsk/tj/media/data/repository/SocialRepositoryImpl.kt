package com.tpsk.tj.media.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map as pagingMap
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.data.local.AppDatabase
import com.tpsk.tj.media.data.local.dao.PostDao
import com.tpsk.tj.media.data.local.entity.PostEntity
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.realtime.RealtimeEngagementManager
import com.tpsk.tj.media.data.api.SocialApi
import com.tpsk.tj.media.data.api.*
import com.tpsk.tj.media.data.paging.PostRemoteMediator
import com.tpsk.tj.media.domain.repository.SocialRepository
import com.tpsk.tj.media.model.*
import com.tpsk.tj.chat.rewards.data.RewardsManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map as flowMap
import javax.inject.Inject
import kotlin.collections.*

class SocialRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val userApi: com.tpsk.tj.common.data.api.UserApi,
    private val postDao: PostDao,
    private val contactDao: com.tpsk.tj.chat.data.local.dao.ContactDao,
    private val appDatabase: AppDatabase,
    private val displayNameResolver: com.tpsk.tj.common.util.DisplayNameResolver,
    private val realtimeEngagementManager: RealtimeEngagementManager
) : SocialRepository {

    init {
        realtimeEngagementManager.start()
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getPostsPaging(following: Boolean, authorId: String, feedType: String, categoryId: String?): Flow<PagingData<Post>> {
        val finalFeedType = when {
            categoryId != null -> "${feedType}_cat_$categoryId"
            authorId.isNotBlank() -> "${feedType}_user_$authorId"
            following -> "following"
            else -> feedType
        }
        
        Log.d("SocialRepo", "getPostsPaging: following=$following, authorId=$authorId, feedType=$feedType, final=$finalFeedType")
        
        val pagingSourceFactory = { postDao.getPostsPaging(finalFeedType) }
        
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            remoteMediator = PostRemoteMediator(
                socialApi = socialApi,
                appDatabase = appDatabase,
                following = following,
                authorId = authorId,
                apiFeedType = feedType,
                dbFeedType = finalFeedType,
                categoryId = categoryId
            ),
            pagingSourceFactory = pagingSourceFactory
        ).flow.flowMap { pagingData ->
            pagingData.pagingMap { entity -> entity.toPost() }
        }
    }

    override suspend fun updatePostLikeLocally(postId: String, likes: Long, isLiked: Boolean) {
        postDao.updateLikeStatus(postId, likes, isLiked)
    }

    override suspend fun updatePostRepostLocally(postId: String, repostCount: Long, isReposted: Boolean) {
        postDao.updateRepostStatus(postId, repostCount, isReposted)
    }

    override suspend fun updatePostShareLocally(postId: String, shares: Long) {
        postDao.updateShareStatus(postId, shares)
    }

    override suspend fun incrementPostRepliesLocally(postId: String) {
        postDao.incrementRepliesCount(postId)
    }

    override suspend fun updatePostBookmarkLocally(postId: String, isBookmarked: Boolean) {
        postDao.updateBookmarkStatus(postId, isBookmarked)
    }

    override suspend fun updateAuthorFollowStatusLocally(authorId: String, isFollowed: Boolean) {
        postDao.updateAuthorFollowStatus(authorId, isFollowed)
    }

    private suspend fun syncPostsInternal(
        page: Int,
        dbFeedType: String,
        following: Boolean = false,
        authorId: String = "",
        channelId: String? = null,
        apiFeedType: String? = null // Optional override for backend parameter
    ) {
        try {
            val response = socialApi.getPosts(
                page = page,
                following = following,
                authorId = authorId,
                channelId = channelId,
                feedType = apiFeedType ?: dbFeedType
            )
            if (response.isSuccessful) {
                val apiPosts = response.body() ?: emptyList()
                val effectiveFeedType = apiFeedType ?: dbFeedType
                val mappedPosts = apiPosts.map { mapApiPost(it, if (effectiveFeedType == "short") "short" else null) }
                val posts = if (effectiveFeedType == "short" || dbFeedType.startsWith("short")) {
                    mappedPosts.filter { 
                        (it.type == PostType.SHORT || it.type == PostType.INTERNAL_AD || it.type == PostType.NATIVE_AD || it.type == PostType.AD_UNIT) &&
                        (it.mediaUrls.isNotEmpty() || it.type == PostType.AD_UNIT)
                    }
                } else mappedPosts

                if (posts.isNotEmpty()) {
                    if (page == 1) postDao.clearPosts(dbFeedType)
                    postDao.insertPosts(posts.map { it.toPostEntity(dbFeedType) })
                }
            } else {
                Log.e("SocialRepo", "syncPostsInternal failed: ${response.code()}")
            }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                Log.e("SocialRepo", "syncPostsInternal exception", e)
            }
        }
    }

    private fun getPostsFlowInternal(feedType: String): Flow<Resource<List<Post>>> = 
        postDao.getPosts(feedType).flowMap { entities ->
            Resource.Success(entities.map { it.toPost() })
        }

    override fun getUserPosts(userId: String, feedType: String, page: Int): Flow<Resource<List<Post>>> = flow {
        // Feed type for Room must be unique per user and per tab (Posts vs Shorts vs Media)
        val roomFeedType = if (feedType.startsWith("user_")) feedType else "${feedType}_user_$userId"
        
        val cached = postDao.getPosts(roomFeedType).first()
        if (cached.isNotEmpty()) emit(Resource.Loading(cached.map { it.toPost() }))
        else emit(Resource.Loading())

        // Pass the actual feed type name ("short", "standard", etc.) to the API, not the room key
        syncPostsInternal(page, roomFeedType, authorId = userId, apiFeedType = feedType)
        emitAll(getPostsFlowInternal(roomFeedType))
    }

    override fun getPosts(page: Int, feedType: String): Flow<Resource<List<Post>>> = flow {
        val cached = postDao.getPosts(feedType).first()
        if (cached.isNotEmpty()) emit(Resource.Loading(cached.map { it.toPost() }))
        else emit(Resource.Loading())

        syncPostsInternal(page, feedType)
        emitAll(getPostsFlowInternal(feedType))
    }

    override fun getFollowingPosts(page: Int, feedType: String): Flow<Resource<List<Post>>> = flow {
        val type = if (feedType == "following") feedType else "following"
        val cached = postDao.getPosts(type).first()
        if (cached.isNotEmpty()) emit(Resource.Loading(cached.map { it.toPost() }))
        else emit(Resource.Loading())

        syncPostsInternal(page, type, following = true)
        emitAll(getPostsFlowInternal(type))
    }

    override fun getChannelPosts(channelId: String, page: Int, feedType: String): Flow<Resource<List<Post>>> = flow {
        val type = "channel_$channelId"
        val cached = postDao.getPosts(type).first()
        if (cached.isNotEmpty()) emit(Resource.Loading(cached.map { it.toPost() }))
        else emit(Resource.Loading())

        syncPostsInternal(page, type, channelId = channelId)
        emitAll(getPostsFlowInternal(type))
    }

    override fun createPost(content: String, mediaUrls: List<String>, postType: String?, tags: List<String>, channelId: String?, pollOptions: List<String>?, location: String?, mediaType: String?, parentPostId: String?, rootPostId: String?, categoryId: String?, repostOfId: String?, thumbnailUrl: String?): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val pollRequest = if (pollOptions != null && pollOptions.size > 0) {
                com.tpsk.tj.media.data.api.CreatePollRequest(options = pollOptions)
            } else null
            
            val request = com.tpsk.tj.media.data.api.CreatePostRequest(
                content = content,
                media_urls = mediaUrls,
                post_type = postType ?: "text_only",
                media_type = mediaType,
                location = location,
                tags = tags,
                channel_id = channelId,
                poll = pollRequest,
                parent_post_id = parentPostId,
                root_post_id = rootPostId,
                category_id = categoryId,
                repost_of_id = repostOfId,
                thumbnail_url = thumbnailUrl
            )
            val response = socialApi.createPost(request)
            if (response.isSuccessful && response.body() != null) {
                val apiPost = response.body()!!
                val mappedPost = mapApiPost(apiPost, postType)
                runCatching {
                    postDao.insertPosts(listOf(mappedPost.toPostEntity(feedType = postType ?: "standard")))
                }
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to create post"))
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

    override fun likePostReply(replyId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.likePostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun repostPostReply(replyId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.repostPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun bookmarkPostReply(replyId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.bookmarkPostReply(replyId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun deletePostReply(replyId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            postDao.deletePostById(replyId)
            val response = socialApi.deletePost(replyId)
            if (response.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Failed to delete reply"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Offline"))
        }
    }
    override fun votePoll(postId: String, optionId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.votePoll(postId, com.tpsk.tj.media.data.api.VotePollRequest(optionId)).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun followUser(userId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.followUser(userId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun unfollowUser(userId: String): Flow<Resource<Boolean>> = flow { try { emit(Resource.Loading()); if (socialApi.unfollowUser(userId).isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail")) } catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; emit(Resource.Error("Offline")) } }
    override fun getTrendingTopics(): Flow<Resource<List<com.tpsk.tj.media.data.api.TrendingTopic>>> = flow { 
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
                val users = response.body()?.let { it.contacts } ?: emptyList()
                val contacts = users.map { user: com.tpsk.tj.common.data.api.ApiUser -> 
                    val userModel = com.tpsk.tj.common.model.User(
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
                        publicKey = user.public_key,
                        isFollowing = user.is_following
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

    override fun viewPost(postId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = socialApi.viewPost(postId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Failed to track view"))
        }
    }

    override fun sharePost(postId: String): Flow<Resource<Boolean>> = flow {
        try {
            val response = socialApi.sharePost(postId)
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Offline"))
        }
    }

    override fun trackEngagement(
        postId: String, 
        eventType: String, 
        category: String,
        watchTimeMs: Int?,
        completed: Boolean?
    ): Flow<Resource<Boolean>> = flow {
        try {
            socialApi.trackEngagement(
                com.tpsk.tj.media.data.api.EngagementRequest(
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
            socialApi.trackAdRevenue(com.tpsk.tj.media.data.api.AdRevenueRequest(postId, revenue, adUnitId))
            emit(Resource.Success(true))
        } catch (_: Exception) {}
    }

    override fun deletePost(postId: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            postDao.deletePostById(postId)
            val response = socialApi.deletePost(postId)
            if (response.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Failed to delete post"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Delete error"))
        }
    }

    override fun toggleRepliesLock(postId: String, lock: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            postDao.updateRepliesLockStatus(postId, lock)
            val response = socialApi.setRepliesLock(postId, com.tpsk.tj.media.data.api.ToggleRepliesLockRequest(lock))
            if (response.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Failed to update comment settings"))
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Comment lock error"))
        }
    }

    override fun reportPost(postId: String, reason: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.reportPost(postId, com.tpsk.tj.common.data.api.ReportRequest(reason))
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to submit report"))
            }
        } catch (e: Exception) {
            if (e !is kotlinx.coroutines.CancellationException) {
                emit(Resource.Error("Report error"))
            }
        }
    }

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
            if (e !is kotlinx.coroutines.CancellationException) emit(Resource.Error("Error: ${e.localizedMessage}"))
        }
    }

    override fun searchPosts(query: String, isGlobal: Boolean): Flow<Resource<List<Post>>> = flow {
        emit(Resource.Loading())
        if (isGlobal) {
            try {
                val response = userApi.search(query, "posts")
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val postsList = body.posts.map { apiPost -> mapApiPost(apiPost) }
                        emit(Resource.Success(postsList))
                    } else {
                        emit(Resource.Success(emptyList()))
                    }
                } else {
                    emit(Resource.Error("Search failed on server: ${response.code()}"))
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) emit(Resource.Error("Offline"))
            }
        } else {
            // Local search in cache
            try {
                val cached = postDao.searchPostsInCache("%$query%").first()
                emit(Resource.Success(cached.map { it.toPost() }))
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) emit(Resource.Error("Local search failed"))
            }
        }
    }

    override fun getSocialNotifications(): Flow<Resource<List<SocialNotificationItem>>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.getNotifications()
            if (response.isSuccessful && response.body() != null) {
                val mapped = response.body()!!.map { mapApiSocialNotification(it) }
                emit(Resource.Success(mapped))
            } else if (response.code() == 404 || response.code() == 405) {
                // Backend endpoint not deployed yet on server -> return empty list gracefully
                emit(Resource.Success(emptyList()))
            } else {
                emit(Resource.Error("Failed to fetch notifications (${response.code()})"))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Success(emptyList()))
        }
    }

    override fun markNotificationRead(id: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.markNotificationRead(id)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Success(true))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Success(true))
        }
    }

    override fun markAllNotificationsRead(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.markAllNotificationsRead()
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Success(true))
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Success(true))
        }
    }
}
