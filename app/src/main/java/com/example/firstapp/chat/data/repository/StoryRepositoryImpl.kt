package com.example.firstapp.chat.data.repository

import android.util.Log
import com.example.firstapp.chat.domain.repository.StoryRepository
import com.example.firstapp.chat.data.local.dao.StoryDao
import com.example.firstapp.chat.data.local.entity.*
import com.example.firstapp.chat.rewards.data.RewardsManager
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.data.realtime.WebSocketManager
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.api.ApiStoryResponse
import com.example.firstapp.media.data.api.CreateStoryRequest
import com.example.firstapp.media.data.api.ApiViewer
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.domain.auth.SessionManager
import com.example.firstapp.common.util.MediaUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.*
import javax.inject.Inject

class StoryRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val storyDao: StoryDao,
    private val contactDao: com.example.firstapp.chat.data.local.dao.ContactDao,
    private val displayNameResolver: com.example.firstapp.common.util.DisplayNameResolver,
    private val webSocketManager: WebSocketManager,
    private val sessionManager: SessionManager
) : StoryRepository {

    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        webSocketManager.connectionState
            .filter { it == WebSocketManager.ConnectionState.CONNECTED }
            .onEach { syncStories() }
            .catch { error -> android.util.Log.w("StoryRepository", "story reconnect sync failed", error) }
            .launchIn(syncScope)
    }

    override fun getStories(): Flow<Resource<List<UserStory>>> = flow {
        // Periodic cleanup: 24h threshold
        val threshold = System.currentTimeMillis() - (24 * 60 * 60 * 1000L)
        try { storyDao.pruneStories(threshold) } catch (_: Exception) {}

        val cachedEntities = storyDao.getStories().first()
        val cached = cachedEntities.map { it.toUserStory() }
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        } else {
            emit(Resource.Loading())
        }

        val fetchFailed = !refreshStories(null, replace = true)
        
        if (fetchFailed && storyDao.getStories().first().isEmpty()) {
            emit(Resource.Error<List<UserStory>>("Could not load stories"))
            return@flow
        }

        emitAll(storyDao.getStories().flowMap { entities: List<StoryEntity> ->
            val models = entities.map { entity ->
                val us = entity.toUserStory()
                us.copy(stories = us.stories.sortedBy { it.createdAt })
            }.filter { it.stories.isNotEmpty() }.toMutableList()
            models.sortByDescending { us: UserStory -> us.stories.maxByOrNull { s: StoryItem -> s.createdAt }?.createdAt ?: 0L }
            Resource.Success<List<UserStory>>(models)
        })
    }

    override suspend fun syncStories(): Boolean {
        val since = storyDao.getLatestStoryTimestamp() ?: 0L
        return refreshStories(since, replace = false)
    }

    private suspend fun refreshStories(since: Long?, replace: Boolean): Boolean {
        return try {
            val response = socialApi.getStories(since?.takeIf { it > 0L })
            if (!response.isSuccessful) {
                android.util.Log.w("StoryRepository", "sync stories: HTTP ${response.code()}")
                return false
            }

            val apiStories = response.body()?.stories ?: emptyList<ApiStoryResponse>()
            val localContacts = contactDao.getContacts().first().associateBy { it.id }
            val fetched = apiStories.map { apiStory: ApiStoryResponse ->
                val contact = localContacts[apiStory.user_id]
                    ?: localContacts[apiStory.user_id.removePrefix("user_")]
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
                    contact?.phoneNumber?.takeIf { it.isNotBlank() }
                        ?: apiStory.phone_number?.takeIf { it.isNotBlank() }
                        ?: apiStory.user_name
                } else resolvedName
                model.copy(userName = finalName)
            }

            if (replace) {
                val myId = sessionManager.getUserId()
                val existingMine = if (myId.isNullOrBlank()) null else {
                    storyDao.getStories().first().find { it.userId == myId || it.id == myId }
                }
                val entities = fetched.map { it.toStoryEntity() }.toMutableList()
                if (existingMine != null && existingMine.stories.isNotEmpty()) {
                    val idx = entities.indexOfFirst { it.userId == myId || it.id == myId }
                    if (idx < 0) {
                        entities.add(0, existingMine)
                    } else if (entities[idx].stories.isEmpty()) {
                        entities[idx] = existingMine
                    }
                }
                storyDao.replaceStories(entities)
            } else if (fetched.isEmpty()) {
                return true
            } else {
                val existing = storyDao.getStories().first().associateBy { it.userId }
                val merged = fetched.map { incoming ->
                    val old = existing[incoming.userId]
                    if (old == null) incoming.toStoryEntity()
                    else old.copy(
                        userName = incoming.userName,
                        phoneNumber = incoming.phoneNumber ?: old.phoneNumber,
                        userAvatarUrl = incoming.userAvatarUrl ?: old.userAvatarUrl,
                        stories = (old.stories + incoming.stories).distinctBy { it.id }.sortedBy { it.createdAt },
                        lastUpdated = incoming.lastUpdated,
                        lastUpdatedMillis = maxOf(old.lastUpdatedMillis, incoming.stories.maxOfOrNull { it.createdAt } ?: 0L),
                        isMuted = incoming.isMuted
                    )
                }
                storyDao.insertStories(merged)
            }
            true
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            android.util.Log.e("StoryRepository", "sync stories failed", e)
            false
        }
    }

    private suspend fun cacheOwnPostedStory(
        apiItem: com.example.firstapp.media.data.api.ApiStoryItem?,
        content: String,
        contentUrl: String?,
        type: String
    ) {
        val myId = sessionManager.getUserId() ?: return
        val me = sessionManager.currentUser.value
        val now = apiItem?.created_at ?: System.currentTimeMillis()
        val item = StoryItem(
            id = apiItem?.id?.takeIf { it.isNotBlank() } ?: "local_$now",
            content = apiItem?.content ?: content,
            contentUrl = MediaUtils.formatUrl(apiItem?.content_url ?: contentUrl),
            type = when ((apiItem?.story_type ?: type).lowercase()) {
                "video" -> StoryType.VIDEO
                "image" -> StoryType.IMAGE
                else -> StoryType.TEXT
            },
            timestamp = formatStoryTimestamp(now),
            createdAt = now,
            backgroundColorArgb = 0xBF546E7A,
            isViewed = true,
            durationMs = apiItem?.duration_ms ?: 5000
        )
        val existing = storyDao.getStories().first().find { it.userId == myId || it.id == myId }
        val mergedStories = ((existing?.stories ?: emptyList()) + item).distinctBy { it.id }.sortedBy { it.createdAt }
        val entity = StoryEntity(
            id = myId,
            userId = myId,
            userName = me?.displayName?.takeIf { it.isNotBlank() } ?: existing?.userName ?: "My Story",
            phoneNumber = me?.phoneNumber ?: existing?.phoneNumber,
            userAvatarUrl = me?.avatarUrl ?: existing?.userAvatarUrl,
            userAvatarColor = existing?.userAvatarColor ?: 0xFF455A64.toInt(),
            stories = mergedStories,
            lastUpdated = formatTimeAgo(now),
            lastUpdatedMillis = now,
            isMuted = existing?.isMuted ?: false
        )
        storyDao.insertStories(listOf(entity))
    }

    override fun createStory(content: String, contentUrl: String?, type: String): Flow<Resource<Boolean>> = flow {
        try {
            emit(Resource.Loading())
            val response = socialApi.createStory(CreateStoryRequest(content, contentUrl, type))
            if (response.isSuccessful) {
                cacheOwnPostedStory(response.body()?.story, content, contentUrl, type)
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
            val entity = storyDao.getStoriesByUserId(userId).first()
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
