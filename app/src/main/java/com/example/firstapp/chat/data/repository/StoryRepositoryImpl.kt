package com.example.firstapp.chat.data.repository

import android.util.Log
import com.example.firstapp.chat.domain.repository.StoryRepository
import com.example.firstapp.chat.data.local.dao.StoryDao
import com.example.firstapp.chat.data.local.entity.*
import com.example.firstapp.chat.rewards.data.RewardsManager
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.api.ApiStoryResponse
import com.example.firstapp.media.data.api.CreateStoryRequest
import com.example.firstapp.media.data.api.ApiViewer
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map as flowMap
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val storyDao: StoryDao,
    private val contactDao: com.example.firstapp.chat.data.local.dao.ContactDao,
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver
) : StoryRepository {

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
                val apiStories = response.body()?.stories ?: emptyList<ApiStoryResponse>()
                
                val localContacts = contactDao.getContacts().first().associateBy { it.id }

                val stories = apiStories.map { apiStory: ApiStoryResponse -> 
                    val contact = localContacts[apiStory.user_id] ?: localContacts[apiStory.user_id.removePrefix("user_")]
                    val model = mapApiStory(apiStory)
                    
                    val resolvedName = displayNameResolver.resolve(
                        com.example.firstapp.common.model.User(
                            id = apiStory.user_id,
                            displayName = contact?.displayName ?: apiStory.user_name,
                            username = "" 
                        ),
                        contact?.phoneNumber ?: apiStory.phone_number ?: ""
                    )
                    
                    val finalName = if (resolvedName.isBlank() || resolvedName == apiStory.user_id) {
                         contact?.phoneNumber?.takeIf { it.isNotBlank() } ?: apiStory.phone_number?.takeIf { it.isNotBlank() } ?: apiStory.user_name
                    } else resolvedName

                    model.copy(userName = finalName)
                }
                storyDao.clearStories()
                storyDao.insertStories(stories.map { it.toStoryEntity() })
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
        }
        
        emitAll(storyDao.getStories().flowMap { entities: List<StoryEntity> ->
            val models = entities.map { it.toUserStory() }.toMutableList()
            models.sortByDescending { us: UserStory -> us.stories.maxByOrNull { s: StoryItem -> s.createdAt }?.createdAt ?: 0L }
            Resource.Success<List<UserStory>>(models)
        })
    }

    override fun createStory(content: String, contentUrl: String?, type: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = socialApi.createStory(CreateStoryRequest(content, contentUrl, type))
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

    override fun getStoryViewers(storyId: String): Flow<Resource<List<ApiViewer>>> = flow { 
        emit(Resource.Loading())
        try { 
            val res = socialApi.getStoryViewers(storyId)
            if (res.isSuccessful) emit(Resource.Success(res.body()?.viewers ?: emptyList())) 
            else emit(Resource.Error("Fail")) 
        } catch (e: Exception) { 
            if (e is kotlinx.coroutines.CancellationException) throw e
            emit(Resource.Error("Error")) 
        } 
    }
}
