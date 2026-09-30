package com.tpsk.tj.stories.domain

import com.tpsk.tj.chat.model.UserStory
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.api.ApiViewer
import kotlinx.coroutines.flow.Flow

interface StoryRepository {
    fun observeStories(): Flow<Resource<List<UserStory>>>
    suspend fun syncStories(): Boolean
    fun createStory(
        content: String,
        contentUrl: String? = null,
        type: String = "text",
        privacy: String = "contacts"
    ): Flow<Resource<Boolean>>
    fun markStoryAsViewed(userId: String, storyId: String): Flow<Resource<Boolean>>
    fun deleteStory(storyId: String): Flow<Resource<Boolean>>
    fun muteStory(userId: String, isMuted: Boolean): Flow<Resource<Boolean>>
    fun getStoryViewers(storyId: String): Flow<Resource<List<ApiViewer>>>
}
