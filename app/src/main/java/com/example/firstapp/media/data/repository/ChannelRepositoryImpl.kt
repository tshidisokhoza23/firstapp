package com.example.firstapp.media.data.repository

import androidx.compose.ui.graphics.Color
import com.example.firstapp.media.domain.repository.ChannelRepository
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.media.data.api.CreateChannelRequest
import com.example.firstapp.media.data.api.UserIdRequest
import com.example.firstapp.common.data.local.dao.ChannelDao
import com.example.firstapp.common.data.repository.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Channel
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.common.model.ConversationType
import java.util.ArrayList
import kotlinx.coroutines.flow.*

import javax.inject.Inject

class ChannelRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val channelDao: ChannelDao,
    private val chatDao: com.example.firstapp.common.data.local.dao.ChatDao
) : ChannelRepository {

    override fun getChannels(): Flow<Resource<List<Channel>>> = flow {
        val cached = channelDao.getChannels().first().map { it.toModel() }
        emit(Resource.Loading(cached))
        try {
            val response = socialApi.getChannels()
            if (response.isSuccessful) {
                val channels = response.body()?.channels?.map { mapApiChannel(it) } ?: emptyList()
                channelDao.clearChannels()
                channelDao.insertChannels(channels.map { it.toEntity() })
            }
        } catch (_: Exception) {}
        
        channelDao.getChannels().collect { entities ->
            val models = entities.map { it.toModel() }
            emit(Resource.Success(models))
        }
    }

    override fun subscribeToChannel(channelId: Int): Flow<Resource<Boolean>> = flow {
        try {
            if (socialApi.subscribeToChannel(channelId).isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Fail"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Offline"))
        }
    }

    override fun muteChannel(channelId: Int, isMuted: Boolean): Flow<Resource<Boolean>> = flow {
        try {
            channelDao.updateMuted(channelId, isMuted)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Fail"))
        }
    }

    override fun createChannel(name: String, description: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.createChannel(CreateChannelRequest(name, description))
            if (response.isSuccessful && response.body() != null) {
                val apiCh = response.body()!!
                val model = mapApiChannel(apiCh)
                
                // Insert into channels table
                channelDao.insertChannel(model.toEntity())
                
                // Also insert into chats table (unified view)
                val chatMsg = ChatMessage(
                    id = model.id,
                    type = ConversationType.CHANNEL,
                    senderName = model.name,
                    lastMessage = "",
                    timestamp = System.currentTimeMillis(),
                    avatarColor = model.avatarColor,
                    avatarUrl = model.avatarUrl,
                    isGroup = false,
                    isAdmin = true,
                    isSubscribed = true,
                    createdBy = model.createdBy
                )
                val entities = ArrayList<com.example.firstapp.common.data.local.entity.ChatEntity>()
                entities.add(chatMsg.toEntity())
                chatDao.insertChats(entities)

                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Server error: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Failed to create channel: ${e.message}"))
        }
    }

    override fun searchChannels(query: String): Flow<List<Channel>> = 
        channelDao.searchChannels(query).map { list -> list.map { it.toModel() } }

    override fun addAdmin(channelId: Int, userId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.addChannelAdmin(channelId, UserIdRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }

    override fun removeAdmin(channelId: Int, userId: Int): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.removeChannelAdmin(channelId, UserIdRequest(userId))
            if (response.isSuccessful) emit(Resource.Success(true)) else emit(Resource.Error("Fail"))
        } catch (e: Exception) { emit(Resource.Error("Error")) }
    }
}
