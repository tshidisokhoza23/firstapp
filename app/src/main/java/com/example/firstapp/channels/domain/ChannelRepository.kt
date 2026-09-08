package com.example.firstapp.channels.domain

import com.example.firstapp.chat.model.Channel
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun observeChannels(): Flow<Resource<List<Channel>>>
    suspend fun syncChannels(): Boolean
    fun getChannels(): Flow<Resource<List<Channel>>>
    fun subscribeToChannel(channelId: String): Flow<Resource<Boolean>>
    fun muteChannel(channelId: String, isMuted: Boolean): Flow<Resource<Boolean>>
    fun createChannel(name: String, description: String): Flow<Resource<Boolean>>
    fun searchChannels(query: String): Flow<List<Channel>>
    fun addAdmin(channelId: String, userId: String): Flow<Resource<Boolean>>
    fun removeAdmin(channelId: String, userId: String): Flow<Resource<Boolean>>
    fun updateAdminPostingPermission(channelId: String, allowed: Boolean): Flow<Resource<Boolean>>
}
