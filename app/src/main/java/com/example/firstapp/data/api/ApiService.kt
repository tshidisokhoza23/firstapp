package com.example.firstapp.data.api

import com.example.firstapp.model.*

/**
 * Interface for API calls. 
 * Base URL: https://firstapp.com/api/
 */
interface ApiService {
    suspend fun getChats(): List<ChatMessage>
    suspend fun getMessages(chatId: Int): List<Message>
    suspend fun getJobs(): List<Job>
    suspend fun getAdverts(): List<Advert>
    suspend fun getChannels(): List<Channel>
}
