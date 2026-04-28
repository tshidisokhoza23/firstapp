package com.example.firstapp.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.AppContainer
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.realtime.ChatSocketListener
import com.example.firstapp.data.realtime.ChatWebSocketClient
import com.example.firstapp.data.realtime.PostSocketListener
import com.example.firstapp.data.realtime.PostWebSocketClient
import com.example.firstapp.data.repository.AppRepository
import com.example.firstapp.data.repository.SearchResult
import com.example.firstapp.model.*
import com.example.firstapp.ui.screens.JobProfileData
import com.example.firstapp.util.Resource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository: AppRepository = AppContainer.repository
    private val userRepository = AppContainer.userRepository

    private var currentSearchJob: kotlinx.coroutines.Job? = null
    private var currentShortsSearchJob: kotlinx.coroutines.Job? = null
    private var messagesFetchJob: kotlinx.coroutines.Job? = null
    private val fetchJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    private fun launchFetch(key: String, block: suspend () -> Unit) {
        fetchJobs[key]?.cancel()
        fetchJobs[key] = viewModelScope.launch {
            block()
        }
    }

    private val _jobProfile = MutableStateFlow<JobProfileData?>(null)
    val jobProfile: StateFlow<JobProfileData?> = _jobProfile.asStateFlow()

    private val _applications = MutableStateFlow<List<JobApplication>>(emptyList())
    val applications: StateFlow<List<JobApplication>> = _applications.asStateFlow()

    private val _chats = MutableStateFlow<Resource<List<ChatMessage>>>(Resource.Loading())
    val chats: StateFlow<Resource<List<ChatMessage>>> = _chats.asStateFlow()

    private val _callLogs = MutableStateFlow<Resource<List<CallLog>>>(Resource.Loading())
    val callLogs: StateFlow<Resource<List<CallLog>>> = _callLogs.asStateFlow()

    private val _jobs = MutableStateFlow<Resource<List<Job>>>(Resource.Loading())
    val jobs: StateFlow<Resource<List<Job>>> = _jobs.asStateFlow()

    private val _adverts = MutableStateFlow<Resource<List<Advert>>>(Resource.Loading())
    val adverts: StateFlow<Resource<List<Advert>>> = _adverts.asStateFlow()

    private val _channels = MutableStateFlow<Resource<List<Channel>>>(Resource.Loading())
    val channels: StateFlow<Resource<List<Channel>>> = _channels.asStateFlow()

    private val _shorts = MutableStateFlow<Resource<List<com.example.firstapp.model.Short>>>(Resource.Loading())
    val shorts: StateFlow<Resource<List<com.example.firstapp.model.Short>>> = _shorts.asStateFlow()

    private val _songs = MutableStateFlow<Resource<List<Song>>>(Resource.Loading())
    val songs: StateFlow<Resource<List<Song>>> = _songs.asStateFlow()

    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val posts: StateFlow<Resource<List<Post>>> = _posts.asStateFlow()

    private val _stories = MutableStateFlow<Resource<List<UserStory>>>(Resource.Loading())
    val stories: StateFlow<Resource<List<UserStory>>> = _stories.asStateFlow()

    private val _messages = MutableStateFlow<Resource<List<Message>>>(Resource.Loading())
    val messages: StateFlow<Resource<List<Message>>> = _messages.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _chatSearchResults = MutableStateFlow<List<Message>>(emptyList())
    val chatSearchResults: StateFlow<List<Message>> = _chatSearchResults.asStateFlow()

    private val _navigationRequest = MutableStateFlow<String?>(null)
    val navigationRequest: StateFlow<String?> = _navigationRequest.asStateFlow()

    private val _syncedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val syncedContacts: StateFlow<List<Contact>> = _syncedContacts.asStateFlow()

    private val _selectedContactsForGroup = MutableStateFlow<List<Contact>>(emptyList())
    val selectedContactsForGroup: StateFlow<List<Contact>> = _selectedContactsForGroup.asStateFlow()

    private val _shortReplies = MutableStateFlow<Resource<List<ShortReply>>>(Resource.Loading())
    val shortReplies: StateFlow<Resource<List<ShortReply>>> = _shortReplies.asStateFlow()

    private var activeShortIdForReplies: Int? = null
    private var activeChatId: Int? = null

    private var lastRealtimePostRefreshMs: Long = 0L
    private val shortLikeInFlight = mutableSetOf<Int>()
    private val shortSubscribeInFlight = mutableSetOf<Int>()
    private val shortLikeLastTapMs = mutableMapOf<Int, Long>()
    private val shortSubscribeLastTapMs = mutableMapOf<Int, Long>()
    private val shortsTapDebounceMs = 350L

    private val _destinations = MutableStateFlow<Resource<List<Destination>>>(Resource.Loading())
    val destinations: StateFlow<Resource<List<Destination>>> = _destinations.asStateFlow()

    private val _rideOptions = MutableStateFlow<Resource<List<RideOption>>>(Resource.Loading())
    val rideOptions: StateFlow<Resource<List<RideOption>>> = _rideOptions.asStateFlow()

    init {
        fetchCurrentUser()
        fetchChats()
        fetchCallLogs()
        fetchJobs()
        fetchAdverts()
        fetchChannels()
        fetchShorts()
        fetchSongs()
        fetchPosts()
        fetchStories()
        fetchContacts()
        fetchJobApplications()
        fetchRideOptions()
        fetchDestinations()
        connectPostRealtime()
        connectChatRealtime()
    }

    fun fetchRideOptions() {
        launchFetch("rideOptions") {
            repository.getRideOptions().collect { _rideOptions.value = it }
        }
    }

    fun fetchDestinations() {
        launchFetch("destinations") {
            repository.getDestinations().collect { _destinations.value = it }
        }
    }

    private fun connectChatRealtime() {
        ChatWebSocketClient.connect(-1, object : ChatSocketListener {
            override fun onOpen() {}
            override fun onMessage(contactId: Int, message: Message) {
                // If it's the active chat, add to messages list
                if (activeChatId == contactId) {
                    val currentRes = _messages.value
                    if (currentRes is Resource.Success) {
                        val currentList = currentRes.data ?: emptyList()
                        if (!currentList.any { it.id == message.id }) {
                            _messages.value = Resource.Success(currentList + message)
                        }
                    }
                    // Mark as read immediately if it's the active chat
                    ChatWebSocketClient.sendRead(contactId)
                }
                // Save to cache
                viewModelScope.launch {
                    repository.saveMessage(contactId, message)
                }
                // Update chat list summary
                updateChatLastMessage(contactId, message)
            }
            override fun onMessageDeleted(contactId: Int, messageId: Int) {
                if (activeChatId == contactId) {
                    val currentRes = _messages.value
                    if (currentRes is Resource.Success) {
                        val updated = (currentRes.data ?: emptyList()).filter { it.id != messageId }
                        _messages.value = Resource.Success(updated)
                    }
                }
                // Refresh chats to update last message if needed
                fetchChats()
            }
            override fun onMessageEdited(contactId: Int, message: Message) {
                if (activeChatId == contactId) {
                    val currentRes = _messages.value
                    if (currentRes is Resource.Success) {
                        val updated = (currentRes.data ?: emptyList()).map {
                            if (it.id == message.id) message else it
                        }
                        _messages.value = Resource.Success(updated)
                    }
                }
                // Update cache
                viewModelScope.launch {
                    repository.updateMessageInCache(contactId, message)
                }
                // Optionally update chat list summary if it was the last message
                val chatsRes = _chats.value
                if (chatsRes is Resource.Success) {
                    val lastMsgId = chatsRes.data?.find { it.id == contactId }?.lastMessage // this is text, not id.
                    // A better way is to always update if it's currently the last message text matches.
                    // But usually, refreshing chats or just updating the state is fine.
                    fetchChats()
                }
            }
            override fun onReaction(contactId: Int, messageId: Int, reactions: Map<String, Int>) {
                if (activeChatId == contactId) {
                    val currentRes = _messages.value
                    if (currentRes is Resource.Success) {
                        val updated = (currentRes.data ?: emptyList()).map {
                            if (it.id == messageId) {
                                val updatedMsg = it.copy(reactions = reactions)
                                // Update cache for reactions
                                viewModelScope.launch {
                                    repository.updateMessageInCache(contactId, updatedMsg)
                                }
                                updatedMsg
                            } else it
                        }
                        _messages.value = Resource.Success(updated)
                    }
                }
            }
            override fun onTyping(contactId: Int, isTyping: Boolean) {
                updateChatTyping(contactId, isTyping)
            }
            override fun onRead(contactId: Int) {
                if (activeChatId == contactId) {
                    val currentRes = _messages.value
                    if (currentRes is Resource.Success) {
                        val updated = (currentRes.data ?: emptyList()).map { it.copy(isRead = true) }
                        _messages.value = Resource.Success(updated)
                    }
                }
                fetchChats() // Refresh to clear unread counts
            }
            override fun onPresence(contactId: Int, isOnline: Boolean, lastSeen: String?) {
                updateChatPresence(contactId, isOnline, lastSeen)
            }
            override fun onError(error: String) {}
        })
    }

    private fun connectPostRealtime() {
        PostWebSocketClient.connect(object : PostSocketListener {
            override fun onOpen() {
                // No-op; realtime posts channel is connected.
            }

            override fun onPostsEvent(action: String, postId: Int?) {
                val now = System.currentTimeMillis()
                if (now - lastRealtimePostRefreshMs < 800) return
                lastRealtimePostRefreshMs = now
                fetchPosts()
            }

            override fun onShortsEvent(action: String, shortId: Int?) {
                if (action == "new_short" || action == "reply") {
                    fetchShorts()
                    if (action == "reply" && shortId != null && shortId == activeShortIdForReplies) {
                        fetchShortReplies(shortId)
                    }
                }
            }

            override fun onError(error: String) {
                // Realtime fallback is manual refresh and mutation-triggered fetches.
            }
        })
    }

    fun withdrawApplication(applicationId: Int) {
        // Optimistic UI remove
        _applications.value = _applications.value.filter { it.id != applicationId }
        viewModelScope.launch {
            repository.withdrawApplication(applicationId).collect { /* done */ }
        }
    }

    fun updateJobProfile(profile: JobProfileData) {
        _jobProfile.value = profile
    }

    fun fetchPosts() {
        launchFetch("posts") {
            repository.getPosts().collect { _posts.value = it }
        }
    }

    fun createPostReply(postId: Int, content: String, parentReplyId: Int? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.createPostReply(postId, content.trim(), parentReplyId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    fetchPosts()
                }
            }
        }
    }

    fun createPost(content: String, mediaUrls: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList()) {
        if (content.isBlank() && mediaUrls.isEmpty()) return
        viewModelScope.launch {
            repository.createPost(content.trim(), mediaUrls, postType, tags).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    fetchPosts()
                }
            }
        }
    }

    fun shareSong(song: Song, content: String) {
        val tags = listOf("song", "artist:${song.artist}", "song_id:${song.id}")
        createPost(content, emptyList(), "song", tags)
    }

    fun likePost(postId: Int) {
        viewModelScope.launch {
            repository.likePost(postId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Optimistic UI handles this without a full feed reload.
                }
            }
        }
    }

    fun repostPost(postId: Int) {
        viewModelScope.launch {
            repository.repostPost(postId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Optimistic UI handles this without a full feed reload.
                }
            }
        }
    }

    fun bookmarkPost(postId: Int) {
        viewModelScope.launch {
            repository.bookmarkPost(postId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Optimistic UI handles this without a full feed reload.
                }
            }
        }
    }

    fun likePostReply(replyId: Int) {
        viewModelScope.launch {
            repository.likePostReply(replyId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Optimistic UI handles this without a full feed reload.
                }
            }
        }
    }

    fun repostPostReply(replyId: Int) {
        viewModelScope.launch {
            repository.repostPostReply(replyId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Optimistic UI handles this without a full feed reload.
                }
            }
        }
    }

    fun bookmarkPostReply(replyId: Int) {
        viewModelScope.launch {
            repository.bookmarkPostReply(replyId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Optimistic UI handles this without a full feed reload.
                }
            }
        }
    }

    fun viewPost(postId: Int) {
        viewModelScope.launch {
            repository.viewPost(postId).collect { /* fire and forget */ }
        }
    }

    fun saveDestination(name: String, address: String, latitude: Double = 0.0, longitude: Double = 0.0, isFavorite: Boolean = false, type: String = "recent") {
        viewModelScope.launch {
            repository.saveDestination(name, address, latitude, longitude, isFavorite, type).collect { /* no UI feedback needed */ }
        }
    }

    fun fetchSongs() {
        launchFetch("songs") {
            repository.getSongs().collect { _songs.value = it }
        }
    }

    fun fetchShorts() {
        launchFetch("shorts") {
            repository.getShorts().collect { _shorts.value = it }
        }
    }

    fun fetchChats() {
        launchFetch("chats") {
            repository.getChats().collect { resource ->
                _chats.value = resource
                // Keep GlobalCallState name cache fresh so notifications show real names
                if (resource is Resource.Success) {
                    resource.data?.forEach { chat ->
                        com.example.firstapp.data.realtime.GlobalCallState.updateContactName(chat.id, chat.senderName)
                    }
                }
            }
        }
    }

    fun fetchCallLogs() {
        launchFetch("callLogs") {
            repository.getCallLogs().collect { _callLogs.value = it }
        }
    }

    fun fetchMessages(chatId: Int) {
        activeChatId = chatId
        messagesFetchJob?.cancel()
        messagesFetchJob = viewModelScope.launch {
            repository.getMessages(chatId).collect { resource ->
                _messages.value = resource
            }
        }
    }

    fun loadMoreMessages(chatId: Int, page: Int) {
        viewModelScope.launch {
            // We just need to trigger the network fetch. 
            // The first page observer in fetchMessages() will see the new DB entries.
            repository.getMessages(chatId, page).collect { /* trigger fetch */ }
        }
    }

    fun clearMessages() {
        activeChatId = null
        messagesFetchJob?.cancel()
        messagesFetchJob = null
        _messages.value = Resource.Loading()
    }

    fun fetchJobs() {
        launchFetch("jobs") {
            repository.getJobs().collect { _jobs.value = it }
        }
    }

    fun applyForJob(jobId: Int) {
        viewModelScope.launch {
            repository.applyForJob(jobId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    fetchJobs()
                    fetchJobApplications()
                }
            }
        }
    }

    fun fetchAdverts() {
        launchFetch("adverts") {
            repository.getAdverts().collect { _adverts.value = it }
        }
    }

    fun markAdvertAsViewed(advertId: Int) {
        val currentAdverts = _adverts.value
        if (currentAdverts is Resource.Success) {
            val updatedList = currentAdverts.data?.map {
                if (it.id == advertId) it.copy(isViewed = true) else it
            }
            _adverts.value = Resource.Success(updatedList ?: emptyList())
        }
    }

    fun fetchChannels() {
        launchFetch("channels") {
            repository.getChannels().collect { _channels.value = it }
        }
    }

    fun fetchContacts() {
        launchFetch("contacts") {
            repository.getContacts().collect { result ->
                if (result is Resource.Success) {
                    _contacts.value = result.data ?: emptyList()
                }
            }
        }
    }

    override fun onCleared() {
        PostWebSocketClient.close()
        ChatWebSocketClient.close()
        super.onCleared()
    }

    fun fetchStories() {
        launchFetch("stories") {
            repository.getStories().collect { _stories.value = it }
        }
    }

    fun fetchCurrentUser() {
        launchFetch("currentUser") {
            userRepository.getCurrentUser().collect { resource ->
                if (resource is Resource.Success) {
                    _currentUser.value = resource.data
                }
            }
        }
    }

    fun updateProfile(displayName: String?, bio: String?, location: String?, avatarUrl: String?) {
        viewModelScope.launch {
            userRepository.updateUser(displayName, bio, location, avatarUrl).collect { resource ->
                if (resource is Resource.Success) {
                    _currentUser.value = resource.data
                    _toastMessage.value = "Profile updated successfully"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to update profile"
                }
            }
        }
    }

    fun uploadProfilePicture(file: java.io.File) {
        viewModelScope.launch {
            repository.uploadFile(file).collect { resource ->
                if (resource is Resource.Success) {
                    val avatarUrl = resource.data?.url
                    updateProfile(null, null, null, avatarUrl)
                } else if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to upload image: ${resource.message}"
                }
            }
        }
    }

    fun forgotPassword(emailOrUsername: String) {
        viewModelScope.launch {
            userRepository.forgotPassword(emailOrUsername).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "Password reset instructions sent"
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Request failed"
                }
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.deleteAccount().collect { resource ->
                if (resource is Resource.Success) {
                    TokenManager.clear()
                    onSuccess()
                } else if (resource is Resource.Error) {
                    _toastMessage.value = resource.message ?: "Failed to delete account"
                }
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

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
        longitude: Double? = null,
        replyToId: Int? = null
    ) {
        // Optimistic UI update
        val tempId = -(System.currentTimeMillis() % 1000000).toInt()
        val tempMessage = Message(
            id = tempId,
            text = body,
            senderId = 0, // Placeholder
            time = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date()),
            isFromMe = true,
            type = when (messageType) {
                "image" -> MessageType.IMAGE
                "video" -> MessageType.VIDEO
                "audio" -> MessageType.AUDIO_MESSAGE
                "location" -> MessageType.LOCATION
                "contact" -> MessageType.CONTACT
                "document" -> MessageType.DOCUMENT
                else -> MessageType.TEXT
            },
            mediaUri = mediaUrl,
            fileName = fileName,
            fileSize = fileSize,
            contactName = contactName,
            contactPhone = contactPhone,
            latitude = latitude,
            longitude = longitude,
            replyToId = replyToId
        )

        val currentMessages = _messages.value.data ?: emptyList()
        _messages.value = Resource.Success(currentMessages + tempMessage)

        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                body = body,
                messageType = messageType,
                mediaUrl = mediaUrl,
                duration = duration,
                fileName = fileName,
                fileSize = fileSize,
                contactName = contactName,
                contactPhone = contactPhone,
                latitude = latitude,
                longitude = longitude,
                replyToId = replyToId
            ).collect { resource ->
                if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to send: ${resource.message}"
                    // Remove temp message on error
                    val updated = _messages.value.data?.filter { it.id != tempId }
                    if (updated != null) _messages.value = Resource.Success(updated)
                } else if (resource is Resource.Success) {
                    // Replace temp message with actual one
                    val updated = _messages.value.data?.map {
                        if (it.id == tempId) resource.data!! else it
                    }
                    if (updated != null) _messages.value = Resource.Success(updated)
                }
            }
        }
    }

    fun reactToMessage(messageId: Int, emoji: String) {
        viewModelScope.launch {
            repository.reactToMessage(messageId, emoji).collect { /* websocket handles update */ }
        }
    }

    fun updateChatTyping(contactId: Int, isTyping: Boolean) {
        viewModelScope.launch {
            repository.updateTyping(contactId, isTyping)
        }
    }

    fun updateChatPresence(contactId: Int, isOnline: Boolean, lastSeen: String?) {
        viewModelScope.launch {
            repository.updatePresence(contactId, isOnline, lastSeen ?: "")
        }
    }

    fun updateChatLastMessage(contactId: Int, message: Message) {
        viewModelScope.launch {
            repository.updateLastMessage(contactId, message.text, message.time)
        }
    }

    fun postStory(content: String, contentUrl: String?) {
        viewModelScope.launch {
            val type = if (contentUrl != null) "image" else "text"
            repository.createStory(content, contentUrl, type).collect { result ->
                if (result is Resource.Success) {
                    fetchStories()
                }
            }
        }
    }

    fun markStoryAsViewed(userId: Int, storyId: Int) {
        viewModelScope.launch {
            repository.markStoryAsViewed(userId, storyId).collect { /* local update */ }
        }
    }

    fun muteStory(userId: Int, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteStory(userId, isMuted).collect { /* local update */ }
        }
    }

    fun fetchJobApplications() {
        launchFetch("jobApplications") {
            repository.getJobApplications().collect { _applications.value = it.data ?: emptyList() }
        }
    }

    private var searchJob: Job? = null
    private var searchShortsJob: Job? = null

    fun search(query: String) {
        currentSearchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }

        currentSearchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            repository.search(query).collect { result ->
                if (result is Resource.Success) {
                    _searchResults.value = result.data
                }
                _isSearching.value = false
            }
        }
    }

    private var currentChatSearchJob: kotlinx.coroutines.Job? = null
    fun searchInChat(chatId: Int, query: String) {
        currentChatSearchJob?.cancel()
        if (query.isBlank()) {
            _chatSearchResults.value = emptyList()
            return
        }
        currentChatSearchJob = viewModelScope.launch {
            delay(300)
            repository.searchLocalMessages(chatId, query).collect { results ->
                _chatSearchResults.value = results
            }
        }
    }

    fun searchShorts(query: String) {
        currentShortsSearchJob?.cancel()
        if (query.isBlank()) {
            fetchShorts()
            return
        }
        currentShortsSearchJob = viewModelScope.launch {
            delay(300)
            repository.searchShorts(query).collect { resource ->
                _shorts.value = resource
            }
        }
    }

    fun clearSearch() {
        currentSearchJob?.cancel()
        currentShortsSearchJob?.cancel()
        _searchResults.value = null
        fetchShorts()
    }

    fun syncContacts(phoneNumbers: List<String>) {
        viewModelScope.launch {
            repository.syncContacts(phoneNumbers).collect { result ->
                if (result is Resource.Success) {
                    val synced = result.data ?: emptyList()
                    val current = _contacts.value.associateBy { it.id }.toMutableMap()
                    synced.forEach { current[it.id] = it }
                    _contacts.value = current.values.toList()
                }
            }
        }
    }

    fun createChat(contactId: Int) {
        viewModelScope.launch {
            repository.createChat(contactId)
            fetchChats()
        }
    }

    fun pinChat(chatId: Int, isPinned: Boolean) {
        viewModelScope.launch {
            repository.pinChat(chatId, isPinned).collect { result ->
                if (result is Resource.Success) fetchChats()
            }
        }
    }

    fun muteChat(chatId: Int, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteChat(chatId, isMuted).collect { result ->
                if (result is Resource.Success) fetchChats()
            }
        }
    }

    fun deleteChat(chatId: Int) {
        viewModelScope.launch {
            repository.deleteChat(chatId).collect { result ->
                if (result is Resource.Success) fetchChats()
            }
        }
    }

    fun clearChat(chatId: Int) {
        viewModelScope.launch {
            repository.clearMessages(chatId)
            // If server supports clearing without deleting chat, call it here.
            // For now, we clear locally.
            _messages.value = Resource.Success(emptyList())
            fetchChats()
        }
    }

    fun createGroup(name: String, contactIds: List<Int>, onSuccess: (ChatMessage) -> Unit) {
        viewModelScope.launch {
            repository.createGroup(name, contactIds).collect { resource ->
                if (resource is Resource.Success) {
                    resource.data?.let {
                        _selectedContactsForGroup.value = emptyList()
                        fetchChats()
                        onSuccess(it)
                    }
                }
            }
        }
    }

    fun setSelectedContactsForGroup(contacts: List<Contact>) {
        _selectedContactsForGroup.value = contacts
    }

    fun refreshAll() {
        fetchChats()
        fetchCallLogs()
        fetchJobs()
        fetchAdverts()
        fetchChannels()
        fetchShorts()
        fetchSongs()
        fetchPosts()
        fetchStories()
        fetchContacts()
        fetchJobApplications()
    }

    fun likeShort(shortId: Int) {
        val now = System.currentTimeMillis()
        val lastTap = shortLikeLastTapMs[shortId] ?: 0L
        if (now - lastTap < shortsTapDebounceMs) return
        shortLikeLastTapMs[shortId] = now
        if (shortLikeInFlight.contains(shortId)) return

        val current = (_shorts.value as? Resource.Success)?.data ?: return
        val previous = current
        val optimistic = current.map { s ->
            if (s.id != shortId) s else {
                val likes = s.likes.toLongOrNull() ?: 0L
                val nextLiked = !s.isLiked
                s.copy(
                    isLiked = nextLiked,
                    likes = (if (nextLiked) likes + 1 else (likes - 1).coerceAtLeast(0)).toString()
                )
            }
        }
        _shorts.value = Resource.Success(optimistic)
        shortLikeInFlight.add(shortId)

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.likeShort(shortId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val synced = ( (_shorts.value as? Resource.Success)?.data ?: optimistic ).map { s ->
                            if (s.id != shortId) s else s.copy(isLiked = body.is_liked, likes = body.likes.toString())
                        }
                        _shorts.value = Resource.Success(synced)
                    }
                } else {
                    _shorts.value = Resource.Success(previous)
                }
            } catch (_: Exception) {
                _shorts.value = Resource.Success(previous)
            } finally {
                shortLikeInFlight.remove(shortId)
            }
        }
    }

    fun subscribeToShortCreator(shortId: Int) {
        val now = System.currentTimeMillis()
        val lastTap = shortSubscribeLastTapMs[shortId] ?: 0L
        if (now - lastTap < shortsTapDebounceMs) return
        shortSubscribeLastTapMs[shortId] = now
        if (shortSubscribeInFlight.contains(shortId)) return

        val current = (_shorts.value as? Resource.Success)?.data ?: return
        val previous = current
        val optimistic = current.map { s ->
            if (s.id != shortId) s else s.copy(isSubscribed = !s.isSubscribed)
        }
        _shorts.value = Resource.Success(optimistic)
        shortSubscribeInFlight.add(shortId)

        viewModelScope.launch {
            try {
                val response = ApiClient.apiService.subscribeToShortCreator(shortId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val synced = ( (_shorts.value as? Resource.Success)?.data ?: optimistic ).map { s ->
                            if (s.id != shortId) s else s.copy(isSubscribed = body.is_subscribed)
                        }
                        _shorts.value = Resource.Success(synced)
                    }
                } else {
                    _shorts.value = Resource.Success(previous)
                }
            } catch (_: Exception) {
                _shorts.value = Resource.Success(previous)
            } finally {
                shortSubscribeInFlight.remove(shortId)
            }
        }
    }

    fun repostShort(shortId: Int) {
        viewModelScope.launch {
            repository.repostShort(shortId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    fetchShorts()
                }
            }
        }
    }

    fun bookmarkShort(shortId: Int) {
        viewModelScope.launch {
            repository.bookmarkShort(shortId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    // Optional: show toast or update UI state
                }
            }
        }
    }

    fun replyToShort(shortId: Int, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.replyToShort(shortId, content).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    fetchShorts()
                    fetchShortReplies(shortId)
                }
            }
        }
    }

    fun fetchShortReplies(shortId: Int) {
        activeShortIdForReplies = shortId
        viewModelScope.launch {
            _shortReplies.value = Resource.Loading()
            repository.getShortReplies(shortId).collect {
                _shortReplies.value = it
            }
        }
    }

    fun clearActiveShortReplies() {
        activeShortIdForReplies = null
    }

    fun subscribeToChannel(channelId: Int) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect { result ->
                if (result is Resource.Success) fetchChannels()
            }
        }
    }

    fun deletePost(postId: Int) {
        viewModelScope.launch {
            repository.deletePost(postId).collect { result ->
                if (result is Resource.Success) {
                    val currentPosts = (_posts.value as? Resource.Success)?.data ?: return@collect
                    _posts.value = Resource.Success(currentPosts.filter { it.id != postId })
                }
            }
        }
    }

    fun incrementSongPlay(songId: Int) {
        viewModelScope.launch {
            repository.incrementSongPlay(songId).collect { /* fire and forget */ }
        }
    }

    fun deleteMessage(messageId: Int) {
        viewModelScope.launch {
            repository.deleteMessage(messageId).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    val current = (_messages.value as? Resource.Success)?.data ?: return@collect
                    _messages.value = Resource.Success(current.filter { it.id != messageId })
                }
            }
        }
    }

    fun onNewMessage(message: Message) {
        val chatId = activeChatId ?: return
        val currentRes = _messages.value
        if (currentRes is Resource.Success) {
            val currentList = currentRes.data ?: emptyList()
            if (!currentList.any { it.id == message.id }) {
                _messages.value = Resource.Success(currentList + message)
            }
        }
        updateChatLastMessage(chatId, message)
    }

    fun starMessage(messageId: Int, isStarred: Boolean) {
        viewModelScope.launch {
            repository.starMessage(messageId, isStarred).collect { /* logic */ }
        }
    }

    fun requestNavigation(route: String) {
        _navigationRequest.value = route
    }

    fun clearNavigationRequest() {
        _navigationRequest.value = null
    }

    fun updateWallpaper(chatId: Int, wallpaperUrl: String?) {
        viewModelScope.launch {
            repository.updateWallpaper(chatId, wallpaperUrl)
            fetchChats()
        }
    }
}
