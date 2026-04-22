package com.example.firstapp.data.repository

import com.example.firstapp.model.*
import com.example.firstapp.model.Short as AppShort
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getChats(): Flow<Resource<List<ChatMessage>>>
    fun getCallLogs(): Flow<Resource<List<CallLog>>>
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
    fun createShort(description: String, videoUrl: String, thumbnailUrl: String): Flow<Resource<Long>>
    fun repostShort(shortId: Int): Flow<Resource<Boolean>>
    fun bookmarkShort(shortId: Int): Flow<Resource<Boolean>>
    fun replyToShort(shortId: Int, content: String): Flow<Resource<Boolean>>
    fun getShortReplies(shortId: Int): Flow<Resource<List<ShortReply>>>
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
    fun createStory(content: String, contentUrl: String? = null, type: String = "text"): Flow<Resource<Boolean>>
    fun syncContacts(phoneNumbers: List<String>): Flow<Resource<List<Contact>>>
    suspend fun createChat(contactId: Int)
    fun search(query: String): Flow<Resource<SearchResult>>
    fun searchShorts(query: String): Flow<Resource<List<AppShort>>>
    fun viewPost(postId: Int): Flow<Resource<Boolean>>
    fun deletePost(postId: Int): Flow<Resource<Boolean>>
    fun saveDestination(name: String, address: String, latitude: Double, longitude: Double, isFavorite: Boolean, type: String): Flow<Resource<Boolean>>
    fun withdrawApplication(applicationId: Int): Flow<Resource<Boolean>>
    fun subscribeToChannel(channelId: Int): Flow<Resource<Boolean>>
    fun incrementSongPlay(songId: Int): Flow<Resource<Boolean>>
    fun likeSong(songId: Int): Flow<Resource<Boolean>>
    fun getCurrentUser(): Flow<Resource<User>>
}

data class SearchResult(
    val chats: List<ChatMessage>,
    val contacts: List<Contact>
)
