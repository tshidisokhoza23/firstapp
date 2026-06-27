package com.example.firstapp.media.domain.repository

import com.example.firstapp.search.model.SearchResult
import com.example.firstapp.media.model.*
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow

interface DiscoveryRepository {
    fun getAdverts(): Flow<Resource<List<Advert>>>
    fun getChannels(): Flow<Resource<List<Channel>>>
    fun subscribeToChannel(channelId: Int): Flow<Resource<Boolean>>
    fun muteChannel(channelId: Int, isMuted: Boolean): Flow<Resource<Boolean>>
    fun search(query: String): Flow<Resource<SearchResult>>
}
