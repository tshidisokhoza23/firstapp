package com.example.firstapp.media.data.repository

import androidx.compose.ui.graphics.Color
import com.example.firstapp.media.domain.repository.ChannelRepository
import com.example.firstapp.media.data.api.SocialApi
import com.example.firstapp.common.data.local.dao.ChannelDao
import com.example.firstapp.common.data.repository.*
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Channel
import kotlinx.coroutines.flow.*

import javax.inject.Inject

class ChannelRepositoryImpl @Inject constructor(
    private val socialApi: SocialApi,
    private val channelDao: ChannelDao
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
        try {
            val newChannel = Channel(
                id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                name = name,
                description = description,
                followers = "0",
                avatarColor = Color.Gray,
                category = "General",
                isSubscribed = true
            )
            channelDao.insertChannel(newChannel.toEntity())
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error("Failed to save channel: ${e.message}"))
        }
    }

    override fun searchChannels(query: String): Flow<List<Channel>> = 
        channelDao.searchChannels(query).map { list -> list.map { it.toModel() } }
}
