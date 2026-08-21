package com.example.firstapp.media.data.repository

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map as pagingMap
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.data.local.AppDatabase
import com.example.firstapp.media.data.local.dao.PostDao
import com.example.firstapp.media.data.local.entity.PostEntity
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
    private val contactDao: com.example.firstapp.chat.data.local.dao.ContactDao,
    private val appDatabase: AppDatabase,
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver
) : SocialRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getPostsPaging(following: Boolean, authorId: String, feedType: String): Flow<PagingData<Post>> {
        val finalFeedType = when {
            authorId.isNotBlank() -> "user_$authorId"
            following -> "following"
            else -> feedType
        }
        
        val pagingSourceFactory = { postDao.getPostsPaging(finalFeedType) }
        
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            remoteMediator = PostRemoteMediator(socialApi, appDatabase, following, authorId, finalFeedType),
            pagingSourceFactory = pagingSourceFactory
        ).flow.flowMap { pagingData ->
            pagingData.pagingMap { entity -> entity.toPost() }
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

    override suspend fun updateAuthorFollowStatusLocally(authorId: String, isFollowed: Boolean) {
        postDao.updateAuthorFollowStatus(authorId, isFollowed)
    }

    private suspend fun syncPostsInternal(
        page: Int,
        feedType: String,
        following: Boolean = false,
        authorId: String = "",
        channelId: String? = null
    ) {
        try {
            val response = socialApi.getPosts(
                page = page,
                following = following,
                authorId = authorId,
                channelId = channelId,
                feedType = feedType
            )
            if (response.isSuccessful) {
                val apiPosts = response.body() ?: emptyList()
                val posts = apiPosts.map { mapApiPost(it) }
                if (posts.isNotEmpty()) {
                    if (page == 1) postDao.clearPosts(feedType)
                    postDao.insertPosts(posts.map { it.toPostEntity(feedType) })
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
        val type = if (feedType.startsWith("user_")) feedType else "user_$userId"
        val cached = postDao.getPosts(type).first()
        if (cached.isNotEmpty()) emit(Resource.Loading(cached.map { it.toPost() }))
        else emit(Resource.Loading())

        syncPostsInternal(page, type, authorId = userId)
        emitAll(getPostsFlowInternal(type))
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

    override fun createPost(content: String, mediaUrls: List<String>, postType: String?, tags: List<String>, channelId: String?, pollOptions: List<String>?, location: String?, mediaType: String?, parentPostId: String?, rootPostId: String?, categoryId: String?): Flow<Resource<Boolean>> = flow {
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
                poll = pollRequest,
                parent_post_id = parentPostId,
                root_post_id = rootPostId,
                category_id = categoryId
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
                val users = response.body()?.let { it.contacts } ?: emptyList()
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
