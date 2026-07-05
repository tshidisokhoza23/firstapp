package com.example.firstapp.common.data.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(): Flow<Resource<List<Post>>>
    fun createPost(content: String, mediaUrls: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList()): Flow<Resource<Boolean>>
    fun likePost(postId: String): Flow<Resource<Boolean>>
    fun repostPost(postId: String): Flow<Resource<Boolean>>
    fun bookmarkPost(postId: String): Flow<Resource<Boolean>>
    fun createPostReply(postId: String, content: String, parentReplyId: String? = null): Flow<Resource<Boolean>>
    fun likePostReply(replyId: String): Flow<Resource<Boolean>>
    fun repostPostReply(replyId: String): Flow<Resource<Boolean>>
    fun bookmarkPostReply(replyId: String): Flow<Resource<Boolean>>
    fun votePoll(postId: String, optionId: String): Flow<Resource<Boolean>>
    fun viewPost(postId: String): Flow<Resource<Boolean>>
    fun deletePost(postId: String): Flow<Resource<Boolean>>
    
    fun getStories(): Flow<Resource<List<UserStory>>>
    fun createStory(content: String, contentUrl: String? = null, type: String = "text"): Flow<Resource<Boolean>>
    fun markStoryAsViewed(userId: String, storyId: String): Flow<Resource<Boolean>>
    fun muteStory(userId: String, isMuted: Boolean): Flow<Resource<Boolean>>
    
    fun getShorts(): Flow<Resource<List<ShortMedia>>>
    fun searchShorts(query: String): Flow<Resource<List<ShortMedia>>>
    fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<String>>
    fun repostShort(shortId: String): Flow<Resource<Boolean>>
    fun bookmarkShort(shortId: String): Flow<Resource<Boolean>>
    fun replyToShort(shortId: String, content: String): Flow<Resource<Boolean>>
    fun getShortReplies(shortId: String): Flow<Resource<List<ShortReply>>>
    
    fun followUser(userId: String): Flow<Resource<Boolean>>
    fun unfollowUser(userId: String): Flow<Resource<Boolean>>
    fun getSuggestedUsers(): Flow<Resource<List<Contact>>>
}
