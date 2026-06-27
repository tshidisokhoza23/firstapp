package com.example.firstapp.common.data.repository

import com.example.firstapp.common.data.api.*
import com.example.firstapp.media.data.api.*
import com.example.firstapp.common.model.*
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(): Flow<Resource<List<Post>>>
    fun createPost(content: String, mediaUrls: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList()): Flow<Resource<Boolean>>
    fun likePost(postId: Int): Flow<Resource<Boolean>>
    fun repostPost(postId: Int): Flow<Resource<Boolean>>
    fun bookmarkPost(postId: Int): Flow<Resource<Boolean>>
    fun createPostReply(postId: Int, content: String, parentReplyId: Int? = null): Flow<Resource<Boolean>>
    fun likePostReply(replyId: Int): Flow<Resource<Boolean>>
    fun repostPostReply(replyId: Int): Flow<Resource<Boolean>>
    fun bookmarkPostReply(replyId: Int): Flow<Resource<Boolean>>
    fun votePoll(postId: Int, optionId: Int): Flow<Resource<Boolean>>
    fun viewPost(postId: Int): Flow<Resource<Boolean>>
    fun deletePost(postId: Int): Flow<Resource<Boolean>>
    
    fun getStories(): Flow<Resource<List<UserStory>>>
    fun createStory(content: String, contentUrl: String? = null, type: String = "text"): Flow<Resource<Boolean>>
    fun markStoryAsViewed(userId: Int, storyId: Int): Flow<Resource<Boolean>>
    fun muteStory(userId: Int, isMuted: Boolean): Flow<Resource<Boolean>>
    
    fun getShorts(): Flow<Resource<List<ShortMedia>>>
    fun searchShorts(query: String): Flow<Resource<List<ShortMedia>>>
    fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<Long>>
    fun repostShort(shortId: Int): Flow<Resource<Boolean>>
    fun bookmarkShort(shortId: Int): Flow<Resource<Boolean>>
    fun replyToShort(shortId: Int, content: String): Flow<Resource<Boolean>>
    fun getShortReplies(shortId: Int): Flow<Resource<List<ShortReply>>>
    
    fun followUser(userId: Int): Flow<Resource<Boolean>>
    fun unfollowUser(userId: Int): Flow<Resource<Boolean>>
    fun getSuggestedUsers(): Flow<Resource<List<Contact>>>
    fun getTrendingTopics(): Flow<Resource<List<TrendingTopic>>>
    
    fun reportUser(userId: Int, reason: String): Flow<Resource<Boolean>>
}
