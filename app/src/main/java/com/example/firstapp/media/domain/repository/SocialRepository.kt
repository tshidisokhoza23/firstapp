package com.example.firstapp.media.domain.repository

import androidx.paging.PagingData
import com.example.firstapp.media.data.api.TrendingTopic
import com.example.firstapp.media.data.api.ApiViewer
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(page: Int, feedType: String): Flow<Resource<List<Post>>>
    fun getFollowingPosts(page: Int, feedType: String): Flow<Resource<List<Post>>>
    fun getChannelPosts(channelId: String, page: Int, feedType: String): Flow<Resource<List<Post>>>
    fun getUserPosts(userId: String, feedType: String, page: Int): Flow<Resource<List<Post>>>
    fun getPostsPaging(following: Boolean, authorId: String, feedType: String, categoryId: String? = null): Flow<PagingData<Post>>
    suspend fun updatePostLikeLocally(postId: String, likes: Long, isLiked: Boolean)
    suspend fun updatePostRepostLocally(postId: String, shares: Long, isReposted: Boolean)
    suspend fun updatePostBookmarkLocally(postId: String, isBookmarked: Boolean)
    suspend fun updateAuthorFollowStatusLocally(authorId: String, isFollowed: Boolean)
    fun createPost(content: String, mediaUrls: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList(), channelId: String? = null, pollOptions: List<String>? = null, location: String? = null, mediaType: String? = null, parentPostId: String? = null, rootPostId: String? = null, categoryId: String? = null): Flow<Resource<Boolean>>
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
    fun trackAdRevenue(postId: String, revenue: Double, adUnitId: String): Flow<Resource<Boolean>>
    fun trackEngagement(
        postId: String, 
        eventType: String, 
        category: String,
        watchTimeMs: Int? = null,
        completed: Boolean? = null
    ): Flow<Resource<Boolean>>
    fun deletePost(postId: String): Flow<Resource<Boolean>>
    fun getPostById(postId: String): Flow<Resource<Post>>
}
