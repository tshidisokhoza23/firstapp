package com.example.firstapp.data.repository

import com.example.firstapp.model.*
import com.example.firstapp.model.Short as AppShort
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getChats(): Flow<Resource<List<ChatMessage>>>
    fun getMessages(chatId: Int): Flow<Resource<List<Message>>>
    fun getJobs(): Flow<Resource<List<Job>>>
    fun getAdverts(): Flow<Resource<List<Advert>>>
    fun getChannels(): Flow<Resource<List<Channel>>>
    fun getShorts(): Flow<Resource<List<AppShort>>>
    fun getSongs(): Flow<Resource<List<Song>>>
    fun getPosts(): Flow<Resource<List<Post>>>
}
