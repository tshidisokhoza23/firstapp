package com.tpsk.tj.channels.data

import android.util.Log
import com.tpsk.tj.channels.data.api.ChannelApi
import com.tpsk.tj.channels.domain.ChannelRepository
import com.tpsk.tj.chat.data.local.dao.ChannelDao
import com.tpsk.tj.chat.data.local.dao.ChatDao
import com.tpsk.tj.chat.data.repository.mapApiChannel
import com.tpsk.tj.chat.data.repository.toEntity
import com.tpsk.tj.chat.data.repository.toModel
import com.tpsk.tj.chat.model.Channel
import com.tpsk.tj.common.data.api.MuteRequest
import com.tpsk.tj.common.data.api.UserApi
import com.tpsk.tj.common.data.realtime.WebSocketManager
import com.tpsk.tj.common.model.ConversationType
import com.tpsk.tj.common.ui.theme.BoyishPurple
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.chat.data.api.CreateChannelRequest
import com.tpsk.tj.chat.data.api.ToggleAdminPostingRequest
import com.tpsk.tj.media.data.api.UserIdRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepositoryImpl @Inject constructor(
    private val channelApi: ChannelApi,
    private val channelDao: ChannelDao,
    private val chatDao: ChatDao,
    private val webSocketManager: WebSocketManager,
    private val userApi: UserApi
) : ChannelRepository {

    override fun getChannels(): Flow<Resource<List<Channel>>> = observeChannels()

    override fun observeChannels(): Flow<Resource<List<Channel>>> = flow {
        val cached = channelDao.getChannelsOnce().map { it.toModel() }
        emit(if (cached.isNotEmpty()) Resource.Loading(cached) else Resource.Loading())
        coroutineScope {
            launch {
                runCatching { syncChannels() }
            }
            emitAll(
                combine(channelDao.getChannels(), chatDao.getChatsByType(ConversationType.CHANNEL)) { channelEntities, chatEntities ->
                    val fromChannelDao = channelEntities.map { it.toModel() }
                    val fromChatDao = chatEntities.map { chatEntity ->
                        Channel(
                            id = chatEntity.id,
                            name = chatEntity.senderName.ifBlank { "Channel" },
                            description = chatEntity.lastMessage,
                            followers = "0",
                            avatarUrl = chatEntity.avatarUrl,
                            avatarColor = BoyishPurple,
                            isSubscribed = chatEntity.isSubscribed,
                            isAdmin = chatEntity.isAdmin,
                            isMuted = chatEntity.isMuted,
                            adminsCanPost = chatEntity.adminsCanPost,
                            conversationId = chatEntity.id,
                            createdBy = chatEntity.createdBy
                        )
                    }
                    Resource.Success((fromChannelDao + fromChatDao).distinctBy { it.conversationId ?: it.id })
                }
            )
        }
    }

    override suspend fun syncChannels(): Boolean {
        return try {
            val response = kotlinx.coroutines.withTimeout(15_000) {
                val primary = runCatching { channelApi.getChatsChannels() }.getOrNull()
                if (primary != null && primary.isSuccessful && primary.body() != null) {
                    primary
                } else {
                    channelApi.getChannels()
                }
            }
            if (!response.isSuccessful || response.body() == null) return false
            val rawChannels = response.body()?.channels.orEmpty()
            val channels = rawChannels
                .map { mapApiChannel(it) }
                .distinctBy { it.conversationId ?: it.id }
            if (channels.isNotEmpty()) {
                val existing = channelDao.getChannelsOnce().associateBy { it.id }
                val merged = channels.map { incoming ->
                    val old = existing[incoming.id]
                    if (old == null) incoming.toEntity() else incoming.toEntity().copy(
                        isSubscribed = incoming.isSubscribed || old.isSubscribed,
                        isAdmin = incoming.isAdmin || old.isAdmin,
                        isMuted = incoming.isMuted || old.isMuted
                    )
                }
                channelDao.insertChannels(merged)
            }
            true
        } catch (e: Exception) {
            if (e is CancellationException && e !is TimeoutCancellationException) throw e
            Log.e(TAG, "syncChannels failed", e)
            false
        }
    }

    override fun subscribeToChannel(channelId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = channelApi.subscribeToChannel(channelId)
            if (response.isSuccessful) {
                val conversationId = channelDao.getChannelsOnce()
                    .find { it.id == channelId || it.conversationId == channelId }
                    ?.conversationId
                    ?: channelId
                webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat:$conversationId") })
                val entity = channelDao.getChannelsOnce().find { it.id == channelId || it.conversationId == channelId }
                if (entity != null) {
                    channelDao.updateSubscription(entity.id, !entity.isSubscribed)
                }
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(response.errorBody()?.string() ?: "Failed to update subscription"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Offline"))
        }
    }

    override fun muteChannel(channelId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            if (channelApi.muteChannel(channelId, MuteRequest(isMuted)).isSuccessful) {
                channelDao.updateMuted(channelId, isMuted)
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun createChannel(name: String, description: String, avatarUrl: String?): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = channelApi.createChannel(
                CreateChannelRequest(
                    name = name,
                    description = description,
                    avatar_url = avatarUrl,
                    is_public = true,
                    is_private = false,
                    type = "public",
                    visibility = "public"
                )
            )
            if (!response.isSuccessful) {
                emit(Resource.Error(response.errorBody()?.string() ?: "Failed to create channel"))
                return@flow
            }
            val created = response.body()?.let { mapApiChannel(it) }?.copy(isSubscribed = true, isAdmin = true)
            if (created != null) {
                channelDao.insertChannel(created.toEntity())
                val convId = created.conversationId ?: created.id
                webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat:$convId") })
                webSocketManager.send("broadcast", buildJsonObject {
                    put("type", "channel_created")
                    put("channel_id", created.id)
                    put("conversation_id", convId)
                    put("name", name)
                    put("description", description)
                    put("avatar_url", avatarUrl)
                })
                webSocketManager.send("broadcast", buildJsonObject {
                    put("type", "new_channel")
                    put("topic", "public_channels")
                    put("channel_id", created.id)
                    put("conversation_id", convId)
                    put("name", name)
                    put("description", description)
                    put("avatar_url", avatarUrl)
                })
            }
            emit(Resource.Success(true))
            runCatching { syncChannels() }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Connection error"))
        }
    }

    override fun getChannel(channelId: String): Flow<Resource<Channel>> = flow {
        emit(Resource.Loading())
        try {
            val response = channelApi.getChannel(channelId)
            if (response.isSuccessful && response.body() != null) {
                val channel = mapApiChannel(response.body()!!)
                channelDao.insertChannel(channel.toEntity())
                emit(Resource.Success(channel))
            } else {
                emit(Resource.Error(response.errorBody()?.string() ?: "Channel not found"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Offline"))
        }
    }

    override fun updateChannelAvatar(channelId: String, avatarUrl: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = channelApi.updateChannelAvatar(
                channelId,
                com.tpsk.tj.chat.data.api.AvatarUrlRequest(avatarUrl)
            )
            if (response.isSuccessful) {
                channelDao.updateAvatar(channelId, avatarUrl)
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error(response.errorBody()?.string() ?: "Failed to update icon"))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Offline"))
        }
    }

    override fun searchChannels(query: String): Flow<List<Channel>> = flow {
        val local = channelDao.searchChannels(query).first().map { it.toModel() }
        emit(local)
        try {
            val dedicated = runCatching { channelApi.searchChannels(query) }
                .getOrNull()?.takeIf { it.isSuccessful }?.body()?.channels.orEmpty()
                .map { mapApiChannel(it) }
            val global = runCatching { userApi.search(query, "channels") }
                .getOrNull()?.takeIf { it.isSuccessful }?.body()?.channels.orEmpty()
                .map { mapApiChannel(it) }
            val remote = (dedicated + global).distinctBy { it.conversationId ?: it.id }
            if (remote.isNotEmpty()) {
                channelDao.insertChannels(remote.map { it.toEntity() })
            }
            emit((remote + local).distinctBy { it.conversationId ?: it.id })
        } catch (e: Exception) {
            Log.e(TAG, "searchChannels failed", e)
        }
    }

    override fun addAdmin(channelId: String, userId: String): Flow<Resource<Boolean>> = flow {
        try {
            if (channelApi.addChannelAdmin(channelId, UserIdRequest(userId)).isSuccessful) {
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun removeAdmin(channelId: String, userId: String): Flow<Resource<Boolean>> = flow {
        try {
            if (channelApi.removeChannelAdmin(channelId, UserIdRequest(userId)).isSuccessful) {
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun updateAdminPostingPermission(channelId: String, allowed: Boolean): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = channelApi.toggleAdminPosting(channelId, ToggleAdminPostingRequest(allowed))
            if (response.isSuccessful) {
                val channel = channelDao.getChannelsOnce().find { it.id == channelId }
                if (channel != null) {
                    channelDao.insertChannels(listOf(channel.copy(adminsCanPost = allowed)))
                }
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to update permission"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Offline"))
        }
    }

    companion object {
        private const val TAG = "ChannelRepository"
    }
}
