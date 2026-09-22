package com.tpsk.tj.stories.data

import android.util.Log
import com.tpsk.tj.chat.data.local.dao.ContactDao
import com.tpsk.tj.chat.data.local.dao.StoryDao
import com.tpsk.tj.chat.data.local.entity.StoryEntity
import com.tpsk.tj.chat.data.repository.mapApiStory
import com.tpsk.tj.chat.data.repository.toStoryEntity
import com.tpsk.tj.chat.data.repository.toUserStory
import com.tpsk.tj.chat.model.StoryItem
import com.tpsk.tj.chat.model.StoryType
import com.tpsk.tj.chat.model.UserStory
import com.tpsk.tj.chat.rewards.data.RewardsManager
import com.tpsk.tj.common.domain.auth.SessionManager
import com.tpsk.tj.common.util.DisplayNameResolver
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.data.api.ApiStoryItem
import com.tpsk.tj.media.data.api.ApiViewer
import com.tpsk.tj.media.data.api.CreateStoryRequest
import com.tpsk.tj.stories.data.api.StoryApi
import com.tpsk.tj.stories.domain.StoryRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepositoryImpl @Inject constructor(
    private val storyApi: StoryApi,
    private val storyDao: StoryDao,
    private val contactDao: ContactDao,
    private val displayNameResolver: DisplayNameResolver,
    private val sessionManager: SessionManager
) : StoryRepository {

    override fun observeStories(): Flow<Resource<List<UserStory>>> = flow {
        val threshold = System.currentTimeMillis() - DAY_MS
        runCatching { storyDao.pruneStories(threshold) }

        val cached = storyDao.getStoriesOnce().map { it.toUserStory() }.filter { it.stories.isNotEmpty() }
        emit(if (cached.isNotEmpty()) Resource.Loading(cached) else Resource.Loading())

        coroutineScope {
            launch { syncStories() }
            emitAll(storyDao.getStories().map { entities ->
                val models = entities.map { it.toUserStory() }
                    .filter { it.stories.isNotEmpty() }
                    .sortedByDescending { it.stories.maxOfOrNull { item -> item.createdAt } ?: 0L }
                Resource.Success(models)
            })
        }
    }

    override suspend fun syncStories(): Boolean {
        return try {
            val response = withTimeout(15_000) { storyApi.getStories(null) }
            if (!response.isSuccessful) {
                Log.w(TAG, "GET stories HTTP ${response.code()}")
                return false
            }
            val incoming = mapBundles(response.body()?.stories.orEmpty())
            mergeIntoCache(incoming)
            true
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            Log.e(TAG, "syncStories failed", e)
            false
        }
    }

    override fun createStory(
        content: String,
        contentUrl: String?,
        type: String,
        privacy: String
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val normalizedType = type.trim().lowercase().ifBlank { "text" }
            val normalizedPrivacy = privacy.trim().ifBlank { "contacts" }
            val request = CreateStoryRequest(
                content = content.trim(),
                content_url = contentUrl?.trim()?.takeIf { it.isNotBlank() },
                story_type = normalizedType,
                privacy = normalizedPrivacy
            )
            val response = withTimeout(20_000) { storyApi.createStory(request) }
            if (!response.isSuccessful) {
                val errorMsg = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: "Server error: ${response.code()}"
                emit(Resource.Error(errorMsg))
                return@flow
            }
            cacheOwnPostedStory(response.body()?.story, content, contentUrl, normalizedType)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            Log.e(TAG, "createStory failed", e)
            emit(Resource.Error(e.localizedMessage ?: "Failed to post story"))
        }
    }

    override fun deleteStory(storyId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val ok = storyApi.deleteStory(storyId).isSuccessful
            if (ok) {
                removeStoryItem(storyId)
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to delete story"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Failed to delete story"))
        }
    }

    override fun markStoryAsViewed(userId: String, storyId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val entity = storyDao.getStoriesByUserId(userId).first()
                ?: storyDao.getStoriesOnce().find { it.id == userId || it.userId == userId }
            if (entity != null) {
                var awardPoints = false
                val updated = entity.stories.map { item ->
                    if (item.id == storyId) {
                        if (item.id.startsWith("ad_")) awardPoints = true
                        item.copy(isViewed = true)
                    } else item
                }
                if (awardPoints) {
                    runCatching { RewardsManager.awardPoints(0.10, "STORY_AD", "Viewed ad story $storyId") }
                }
                storyDao.insertStories(listOf(entity.copy(stories = updated)))
            }
            storyApi.markStoryViewed(storyId)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Failed to mark viewed"))
        }
    }

    override fun muteStory(userId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val entity = storyDao.getStoriesOnce().find { it.id == userId || it.userId == userId }
            if (entity != null) {
                storyDao.insertStories(listOf(entity.copy(isMuted = isMuted)))
            }
            storyApi.muteUser(userId, com.tpsk.tj.common.data.api.MuteRequest(isMuted))
            emit(Resource.Success(true))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Failed to mute"))
        }
    }

    override fun getStoryViewers(storyId: String): Flow<Resource<List<ApiViewer>>> = flow {
        emit(Resource.Loading())
        try {
            val res = storyApi.getStoryViewers(storyId)
            if (res.isSuccessful) emit(Resource.Success(res.body()?.viewers ?: emptyList()))
            else emit(Resource.Error("Failed to load viewers"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error("Failed to load viewers"))
        }
    }

    private suspend fun mapBundles(apiStories: List<com.tpsk.tj.media.data.api.ApiStoryResponse>): List<UserStory> {
        val localContacts = contactDao.getContacts().first().associateBy { it.id }
        return apiStories.map { apiStory ->
            val contact = localContacts[apiStory.user_id]
                ?: localContacts[apiStory.user_id.removePrefix("user_")]
            val model = mapApiStory(apiStory)
            val resolvedName = displayNameResolver.resolve(
                com.tpsk.tj.common.model.User(
                    id = apiStory.user_id,
                    displayName = contact?.displayName ?: apiStory.user_name,
                    username = ""
                ),
                contact?.phoneNumber ?: apiStory.phone_number ?: ""
            )
            val finalName = when {
                resolvedName.isBlank() || resolvedName == apiStory.user_id ->
                    contact?.phoneNumber?.takeIf { it.isNotBlank() }
                        ?: apiStory.phone_number?.takeIf { it.isNotBlank() }
                        ?: apiStory.user_name
                else -> resolvedName
            }
            model.copy(userName = finalName)
        }
    }

    private suspend fun mergeIntoCache(incoming: List<UserStory>) {
        val myId = sessionManager.getUserId()
        val existing = storyDao.getStoriesOnce().associateBy { it.userId.ifBlank { it.id } }
        val merged = linkedMapOf<String, StoryEntity>()

        existing.forEach { (key, entity) -> merged[key] = entity }

        incoming.forEach { bundle ->
            val key = bundle.userId.ifBlank { bundle.id }
            val old = merged[key]
            merged[key] = if (old == null) {
                bundle.toStoryEntity()
            } else {
                old.copy(
                    userName = bundle.userName.ifBlank { old.userName },
                    phoneNumber = bundle.phoneNumber ?: old.phoneNumber,
                    userAvatarUrl = bundle.userAvatarUrl ?: old.userAvatarUrl,
                    stories = (old.stories + bundle.stories).distinctBy { it.id }.sortedBy { it.createdAt },
                    lastUpdated = bundle.lastUpdated,
                    lastUpdatedMillis = maxOf(
                        old.lastUpdatedMillis,
                        bundle.stories.maxOfOrNull { it.createdAt } ?: 0L
                    ),
                    isMuted = bundle.isMuted
                )
            }
        }

        val cutoff = System.currentTimeMillis() - DAY_MS
        val pruned = merged.values.map { entity ->
            entity.copy(stories = entity.stories.filter { it.createdAt == 0L || it.createdAt >= cutoff })
        }.filter { it.stories.isNotEmpty() || (myId != null && (it.userId == myId || it.id == myId)) }

        storyDao.replaceStories(pruned)
    }

    private suspend fun cacheOwnPostedStory(
        apiItem: ApiStoryItem?,
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
            timestamp = com.tpsk.tj.chat.data.repository.formatTimeAgo(now),
            createdAt = now,
            backgroundColorArgb = 0xBF546E7A,
            isViewed = true,
            durationMs = apiItem?.duration_ms ?: 5000
        )
        val existing = storyDao.getStoriesOnce().find { it.userId == myId || it.id == myId }
        val mergedStories = ((existing?.stories ?: emptyList()) + item).distinctBy { it.id }.sortedBy { it.createdAt }
        storyDao.insertStories(
            listOf(
                StoryEntity(
                    id = myId,
                    userId = myId,
                    userName = me?.displayName?.takeIf { it.isNotBlank() } ?: existing?.userName ?: "My Story",
                    phoneNumber = me?.phoneNumber ?: existing?.phoneNumber,
                    userAvatarUrl = me?.avatarUrl ?: existing?.userAvatarUrl,
                    userAvatarColor = existing?.userAvatarColor ?: 0xFF455A64.toInt(),
                    stories = mergedStories,
                    lastUpdated = com.tpsk.tj.chat.data.repository.formatTimeAgo(now),
                    lastUpdatedMillis = now,
                    isMuted = existing?.isMuted ?: false
                )
            )
        )
    }

    private suspend fun removeStoryItem(storyId: String) {
        val updated = storyDao.getStoriesOnce().mapNotNull { entity ->
            val remaining = entity.stories.filterNot { it.id == storyId }
            if (remaining.isEmpty()) null else entity.copy(stories = remaining)
        }
        storyDao.replaceStories(updated)
    }

    companion object {
        private const val TAG = "StoryRepository"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}
