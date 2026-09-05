package com.example.firstapp.chat.data.repository

import android.util.Log
import com.example.firstapp.chat.data.local.dao.ChannelDao
import com.example.firstapp.chat.data.local.dao.ChatDao
import com.example.firstapp.chat.domain.repository.ChannelRepository
import com.example.firstapp.chat.model.Channel
import com.example.firstapp.common.model.ConversationType
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map as flowMap
import javax.inject.Inject

class ChannelRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val channelDao: ChannelDao,
    private val chatDao: ChatDao,
    private val webSocketManager: com.example.firstapp.common.data.realtime.WebSocketManager
) : ChannelRepository {

    override fun getChannels(): Flow<Resource<List<Channel>>> = flow {
        val cachedEntities = channelDao.getChannels().first()
        val cached = cachedEntities.map { it.toModel() }
        if (cached.isNotEmpty()) {
            emit(Resource.Loading(cached))
        } else {
            emit(Resource.Loading())
        }

        try {
            var response = socialApi.getChannels()
            if (!response.isSuccessful) {
                response = socialApi.getChatsChannels()
            }

            if (response.isSuccessful && response.body() != null) {
                val apiChannels = response.body()!!.channels
                Log.d("ChannelRepo", "Fetched ${apiChannels.size} channels")
                val channels = apiChannels.map { mapApiChannel(it) }.distinctBy { it.id }
                if (channels.isNotEmpty()) {
                    channelDao.replaceChannels(channels.map { it.toEntity() })
                }
            } else {
                Log.e("ChannelRepo", "getChannels failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ChannelRepo", "getChannels exception", e)
        }

        emitAll(
            combine(
                channelDao.getChannels(),
                chatDao.getChats()
            ) { channelEntities, chatEntities ->
                val fromChannelDao = channelEntities.map { it.toModel() }
                val fromChatDao = chatEntities
                    .filter { it.type == ConversationType.CHANNEL }
                    .map { chatEntity ->
                        Channel(
                            id = chatEntity.id,
                            name = chatEntity.senderName.ifBlank { "Channel" },
                            description = chatEntity.lastMessage,
                            followers = "0",
                            avatarUrl = chatEntity.avatarUrl,
                            avatarColor = com.example.firstapp.common.ui.theme.BoyishPurple,
                            isSubscribed = chatEntity.isSubscribed,
                            isAdmin = chatEntity.isAdmin,
                            isMuted = chatEntity.isMuted,
                            adminsCanPost = chatEntity.adminsCanPost,
                            conversationId = chatEntity.id,
                            createdBy = chatEntity.createdBy
                        )
                    }
                val merged = (fromChannelDao + fromChatDao).distinctBy { it.id }
                Resource.Success<List<Channel>>(merged)
            }
        )
    }

    override fun subscribeToChannel(channelId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.subscribeToChannel(channelId)
            if (response.isSuccessful) {
                // Join WebSocket for real-time messages if now subscribed
                val conversationId = channelDao.getChannels().first()
                    .find { it.id == channelId }
                    ?.conversationId
                    ?: channelId
                webSocketManager.send("chat:$conversationId", "join", op = "subscribe")

                val entities = channelDao.getChannels().first()
                val entity = entities.find { it.id == channelId }
                if (entity != null) {
                    val newStatus = !entity.isSubscribed
                    channelDao.updateSubscription(channelId, newStatus)
                }
                emit(Resource.Success(true))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to update subscription"
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Offline"))
        }
    }

    override fun muteChannel(channelId: String, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            if (socialApi.muteChannel(channelId, com.example.firstapp.common.data.api.MuteRequest(isMuted)).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun createChannel(name: String, description: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.createChannel(CreateChannelRequest(name, description))
            if (response.isSuccessful) {
                val createdApiChannel = response.body()
                val createdModel = createdApiChannel?.let { mapApiChannel(it) }?.copy(isSubscribed = true, isAdmin = true)

                if (createdModel != null) {
                    // Save created channel immediately to Room DB
                    channelDao.insertChannel(createdModel.toEntity())
                    val convId = createdModel.conversationId ?: createdModel.id
                    webSocketManager.send("chat:$convId", "join", op = "subscribe")
                }

                var channelsResponse = socialApi.getChannels()
                if (!channelsResponse.isSuccessful) {
                    channelsResponse = socialApi.getChatsChannels()
                }

                if (channelsResponse.isSuccessful && channelsResponse.body() != null) {
                    val serverChannels = channelsResponse.body()!!.channels
                        .map { mapApiChannel(it) }
                        .distinctBy { it.id }

                    val merged = if (createdModel != null) {
                        (listOf(createdModel) + serverChannels).distinctBy { it.id }
                    } else {
                        serverChannels
                    }

                    channelDao.insertChannels(merged.map { it.toEntity() })
                }
                emit(Resource.Success(true))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Failed to create channel"
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            Log.e("ChannelRepo", "createChannel exception", e)
            emit(Resource.Error(e.message ?: "Connection error"))
        }
    }

    override fun searchChannels(query: String): Flow<List<Channel>> =
        channelDao.searchChannels(query).flowMap { list -> list.map { it.toModel() } }

    override fun addAdmin(channelId: String, userId: String): Flow<Resource<Boolean>> = flow {
        try {
            if (socialApi.addChannelAdmin(channelId, UserIdRequest(userId)).isSuccessful) {
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun removeAdmin(channelId: String, userId: String): Flow<Resource<Boolean>> = flow {
        try {
            if (socialApi.removeChannelAdmin(channelId, UserIdRequest(userId)).isSuccessful) {
                emit(Resource.Success(true))
            } else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Offline")) }
    }

    override fun updateAdminPostingPermission(channelId: String, allowed: Boolean): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.toggleAdminPosting(channelId, ToggleAdminPostingRequest(allowed))
            if (response.isSuccessful) {
                val channel = channelDao.getChannels().first().find { it.id == channelId }
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
}
