package com.example.firstapp.data.repository

import com.example.firstapp.model.*
import com.example.firstapp.model.Short as AppShort
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getChats(): Flow<Resource<List<ChatMessage>>>
    fun getMessages(chatId: Int): Flow<Resource<List<Message>>>
    fun sendMessage(
        chatId: Int,
        body: String,
        messageType: String = "text",
        mediaUrl: String? = null,
        duration: String? = null,
        fileName: String? = null,
        fileSize: String? = null,
        contactName: String? = null,
        contactPhone: String? = null,
        latitude: Double? = null,
        longitude: Double? = null
    ): Flow<Resource<Message>>
    fun getContacts(): Flow<Resource<List<Contact>>>
    fun getJobs(): Flow<Resource<List<Job>>>
    fun applyForJob(jobId: Int): Flow<Resource<Boolean>>
    fun getJobApplications(): Flow<Resource<List<JobApplication>>>
    fun getAdverts(): Flow<Resource<List<Advert>>>
    fun getChannels(): Flow<Resource<List<Channel>>>
    fun getShorts(): Flow<Resource<List<com.example.firstapp.model.Short>>>
    fun getSongs(): Flow<Resource<List<Song>>>
    fun getPosts(): Flow<Resource<List<Post>>>
    fun createPost(content: String, mediaUrls: List<String> = emptyList()): Flow<Resource<Boolean>>
    fun likePost(postId: Int): Flow<Resource<Boolean>>
    fun repostPost(postId: Int): Flow<Resource<Boolean>>
    fun bookmarkPost(postId: Int): Flow<Resource<Boolean>>
    fun createPostReply(postId: Int, content: String, parentReplyId: Int? = null): Flow<Resource<Boolean>>
    fun likePostReply(replyId: Int): Flow<Resource<Boolean>>
    fun repostPostReply(replyId: Int): Flow<Resource<Boolean>>
    fun bookmarkPostReply(replyId: Int): Flow<Resource<Boolean>>
    fun getStories(): Flow<Resource<List<UserStory>>>
    fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>>
    fun search(query: String): Flow<Resource<SearchResult>>
}

data class SearchResult(
    val chats: List<ChatMessage>,
    val contacts: List<Contact>
)
