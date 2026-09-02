package com.example.firstapp.chat.data.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.data.api.*
import com.example.firstapp.chat.domain.repository.ChannelRepository
import com.example.firstapp.chat.model.Channel
import com.example.firstapp.chat.data.local.dao.ChannelDao
import com.example.firstapp.chat.data.local.entity.ChannelEntity
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map as flowMap
import javax.inject.Inject
import kotlin.collections.*

class ChannelRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val channelDao: ChannelDao,
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
            val response = socialApi.getChannels()
            if (response.isSuccessful && response.body() != null) {
                val apiChannels = response.body()!!.channels
                Log.d("ChannelRepo", "Fetched ${apiChannels.size} channels")
                val channels = apiChannels.map { apiChannel: com.example.firstapp.media.data.api.ApiChannel -> mapApiChannel(apiChannel) }.distinctBy { it.id }
                if (channels.isNotEmpty()) {
                    channelDao.replaceChannels(channels.map { it.toEntity() })
                }
            } else {
                Log.e("ChannelRepo", "getChannels failed: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ChannelRepo", "getChannels exception", e)
        }
        
        emitAll(channelDao.getChannels().flowMap { entities ->
            val models = entities.map { it.toModel() }
            Resource.Success<List<Channel>>(models)
        })
    }

    override fun subscribeToChannel(channelId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = socialApi.subscribeToChannel(channelId)
            if (response.isSuccessful) {
                // Join WebSocket for real-time messages if now subscribed
                webSocketManager.send("chat:$channelId", "join", op = "subscribe")

                // Update local cache - Endpoint is usually a toggle
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
                // Update local cache
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
