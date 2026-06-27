package com.example.firstapp.media.domain.repository

import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Channel
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getChannels(): Flow<Resource<List<Channel>>>
    fun subscribeToChannel(channelId: Int): Flow<Resource<Boolean>>
    fun muteChannel(channelId: Int, isMuted: Boolean): Flow<Resource<Boolean>>
    fun createChannel(name: String, description: String): Flow<Resource<Boolean>>
    fun searchChannels(query: String): Flow<List<Channel>>
}
