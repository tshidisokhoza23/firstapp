package com.example.firstapp.chat.domain.repository

import com.example.firstapp.media.data.api.ApiViewer
import com.example.firstapp.chat.model.UserStory
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun getStories(): Flow<Resource<List<UserStory>>>
    suspend fun syncStories(): Boolean
    fun createStory(content: String, contentUrl: String? = null, type: String = "text"): Flow<Resource<Boolean>>
    fun markStoryAsViewed(userId: String, storyId: String): Flow<Resource<Boolean>>
    fun deleteStory(storyId: String): Flow<Resource<Boolean>>
    fun muteStory(userId: String, isMuted: Boolean): Flow<Resource<Boolean>>
    fun getStoryViewers(storyId: String): Flow<Resource<List<ApiViewer>>>
}
