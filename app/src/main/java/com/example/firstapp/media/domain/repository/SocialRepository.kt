package com.example.firstapp.media.domain.repository

import androidx.paging.PagingData
import com.example.firstapp.media.data.api.TrendingTopic
import com.example.firstapp.media.data.api.ApiViewer
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(page: Int = 1): Flow<Resource<List<Post>>>
    fun getFollowingPosts(page: Int = 1): Flow<Resource<List<Post>>>
    fun getPostsPaging(following: Boolean = false): Flow<PagingData<Post>>
    suspend fun updatePostLikeLocally(postId: Int, likes: String, isLiked: Boolean)
    fun createPost(content: String, mediaUrls: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList()): Flow<Resource<Boolean>>
    fun likePost(postId: Int): Flow<Resource<Boolean>>
    fun repostPost(postId: Int): Flow<Resource<Boolean>>
    fun bookmarkPost(postId: Int): Flow<Resource<Boolean>>
    fun createPostReply(postId: Int, content: String, parentReplyId: Int? = null): Flow<Resource<Boolean>>
    fun likePostReply(replyId: Int): Flow<Resource<Boolean>>
    fun repostPostReply(replyId: Int): Flow<Resource<Boolean>>
    fun bookmarkPostReply(replyId: Int): Flow<Resource<Boolean>>
    fun votePoll(postId: Int, optionId: Int): Flow<Resource<Boolean>>
    fun followUser(userId: Int): Flow<Resource<Boolean>>
    fun unfollowUser(userId: Int): Flow<Resource<Boolean>>
    fun getTrendingTopics(): Flow<Resource<List<TrendingTopic>>>
    fun getSuggestedUsers(): Flow<Resource<List<Contact>>>
    fun getStories(): Flow<Resource<List<UserStory>>>
    fun createStory(content: String, contentUrl: String? = null, type: String = "text"): Flow<Resource<Boolean>>
    fun markStoryAsViewed(userId: Int, storyId: Int): Flow<Resource<Boolean>>
    fun deleteStory(storyId: Int): Flow<Resource<Boolean>>
    fun muteStory(userId: Int, isMuted: Boolean): Flow<Resource<Boolean>>
    fun viewPost(postId: Int): Flow<Resource<Boolean>>
    fun deletePost(postId: Int): Flow<Resource<Boolean>>
    fun getStoryViewers(storyId: Int): Flow<Resource<List<ApiViewer>>>
}
