package com.example.firstapp.channels.data

import android.util.Log
import com.example.firstapp.channels.data.api.ChannelApi
import com.example.firstapp.channels.domain.ChannelRepository
import com.example.firstapp.chat.data.local.dao.ChannelDao
import com.example.firstapp.chat.data.local.dao.ChatDao
import com.example.firstapp.chat.data.repository.mapApiChannel
import com.example.firstapp.chat.data.repository.toEntity
import com.example.firstapp.chat.data.repository.toModel
import com.example.firstapp.chat.model.Channel
import com.example.firstapp.common.data.api.MuteRequest
import com.example.firstapp.common.data.api.UserApi
import com.example.firstapp.common.data.realtime.WebSocketManager
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.common.ui.theme.BoyishPurple
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.CreateChannelRequest
import com.example.firstapp.media.data.api.ToggleAdminPostingRequest
import com.example.firstapp.media.data.api.UserIdRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
        syncChannels()
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

    override suspend fun syncChannels(): Boolean {
        return try {
            val fetched = mutableListOf<Channel>()
            runCatching { channelApi.getChannels() }.getOrNull()?.takeIf { it.isSuccessful }?.body()?.channels
                ?.map { mapApiChannel(it) }?.let { fetched += it }
            runCatching { channelApi.getChatsChannels() }.getOrNull()?.takeIf { it.isSuccessful }?.body()?.channels
                ?.map { mapApiChannel(it) }?.let { fetched += it }
            val channels = fetched.distinctBy { it.conversationId ?: it.id }
            if (channels.isNotEmpty()) {
                val existing = channelDao.getChannelsOnce().associateBy { it.conversationId ?: it.id }
                val merged = channels.map { incoming ->
                    val old = existing[incoming.conversationId ?: incoming.id]
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
            if (e is CancellationException) throw e
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

    override fun createChannel(name: String, description: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = channelApi.createChannel(CreateChannelRequest(name, description))
            if (!response.isSuccessful) {
                emit(Resource.Error(response.errorBody()?.string() ?: "Failed to create channel"))
                return@flow
            }
            val created = response.body()?.let { mapApiChannel(it) }?.copy(isSubscribed = true, isAdmin = true)
            if (created != null) {
                channelDao.insertChannel(created.toEntity())
                val convId = created.conversationId ?: created.id
                webSocketManager.send("subscribe", buildJsonObject { put("topic", "chat:$convId") })
            }
            emit(Resource.Success(true))
            runCatching { syncChannels() }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            emit(Resource.Error(e.message ?: "Connection error"))
        }
    }

    override fun searchChannels(query: String): Flow<List<Channel>> = flow {
        val local = channelDao.searchChannels(query).first().map { it.toModel() }
        emit(local)
        try {
            val response = userApi.search(query, "channels")
            if (response.isSuccessful) {
                val remote = response.body()?.channels.orEmpty().map { mapApiChannel(it) }
                if (remote.isNotEmpty()) {
                    channelDao.insertChannels(remote.map { it.toEntity() })
                }
                emit((remote + local).distinctBy { it.conversationId ?: it.id })
            }
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
