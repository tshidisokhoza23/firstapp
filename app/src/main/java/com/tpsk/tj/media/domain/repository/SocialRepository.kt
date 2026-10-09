package com.tpsk.tj.media.domain.repository

import androidx.paging.PagingData
import com.tpsk.tj.media.data.api.TrendingTopic
import com.tpsk.tj.media.data.api.ApiViewer
import com.tpsk.tj.media.model.*
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(page: Int, feedType: String): Flow<Resource<List<Post>>>
    fun getFollowingPosts(page: Int, feedType: String): Flow<Resource<List<Post>>>
    fun getChannelPosts(channelId: String, page: Int, feedType: String): Flow<Resource<List<Post>>>
    fun getUserPosts(userId: String, feedType: String, page: Int): Flow<Resource<List<Post>>>
    fun getPostsPaging(following: Boolean, authorId: String, feedType: String, categoryId: String? = null, qualityMode: String? = null): Flow<PagingData<Post>>
    suspend fun updatePostLikeLocally(postId: String, likes: Long, isLiked: Boolean)
    suspend fun updatePostRepostLocally(postId: String, repostCount: Long, isReposted: Boolean)
    suspend fun updatePostShareLocally(postId: String, shares: Long)
    suspend fun incrementPostRepliesLocally(postId: String)
    suspend fun updatePostBookmarkLocally(postId: String, isBookmarked: Boolean)
    suspend fun updateAuthorFollowStatusLocally(authorId: String, isFollowed: Boolean)
    fun createPost(content: String, mediaUrls: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList(), channelId: String? = null, pollOptions: List<String>? = null, location: String? = null, mediaType: String? = null, parentPostId: String? = null, rootPostId: String? = null, categoryId: String? = null, repostOfId: String? = null, thumbnailUrl: String? = null, compose: com.tpsk.tj.common.data.api.ComposeMetaDto? = null): Flow<Resource<Boolean>>
    fun likePost(postId: String): Flow<Resource<Boolean>>
    fun repostPost(postId: String): Flow<Resource<Boolean>>
    fun bookmarkPost(postId: String): Flow<Resource<Boolean>>
    fun getReplies(postId: String): Flow<Resource<List<Post>>>
    fun likePostReply(replyId: String): Flow<Resource<Boolean>>
    fun repostPostReply(replyId: String): Flow<Resource<Boolean>>
    fun bookmarkPostReply(replyId: String): Flow<Resource<Boolean>>
    fun deletePostReply(replyId: String): Flow<Resource<Boolean>>
    fun votePoll(postId: String, optionId: String): Flow<Resource<Boolean>>
    fun followUser(userId: String): Flow<Resource<Boolean>>
    fun unfollowUser(userId: String): Flow<Resource<Boolean>>
    fun getTrendingTopics(): Flow<Resource<List<TrendingTopic>>>
    fun getSuggestedUsers(): Flow<Resource<List<Contact>>>
    fun viewPost(postId: String): Flow<Resource<Boolean>>
    fun sharePost(postId: String): Flow<Resource<Boolean>>
    fun trackAdRevenue(postId: String, revenue: Double, adUnitId: String): Flow<Resource<Boolean>>
    fun trackEngagement(
        postId: String, 
        eventType: String, 
        category: String,
        watchTimeMs: Int? = null,
        completed: Boolean? = null
    ): Flow<Resource<Boolean>>
    fun deletePost(postId: String): Flow<Resource<Boolean>>
    suspend fun hidePostLocally(postId: String)
    fun toggleRepliesLock(postId: String, lock: Boolean): Flow<Resource<Boolean>>
    fun reportPost(postId: String, reason: String): Flow<Resource<Boolean>>
    fun getPostById(postId: String): Flow<Resource<Post>>
    fun searchPosts(query: String, isGlobal: Boolean = false): Flow<Resource<List<Post>>>
    fun getUnreadNotificationCount(): Flow<Resource<Int>>
    fun getSocialNotifications(): Flow<Resource<List<SocialNotificationItem>>>
    fun markNotificationRead(id: String): Flow<Resource<Boolean>>
    fun markAllNotificationsRead(): Flow<Resource<Boolean>>
}
