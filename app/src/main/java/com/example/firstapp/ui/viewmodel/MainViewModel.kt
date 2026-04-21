package com.example.firstapp.ui.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.data.TokenManager
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.realtime.PostSocketListener
import com.example.firstapp.data.realtime.PostWebSocketClient
import com.example.firstapp.data.repository.AppRepository
import com.example.firstapp.data.repository.AppRepositoryImpl
import com.example.firstapp.data.repository.SearchResult
import com.example.firstapp.model.*
import com.example.firstapp.ui.screens.JobProfileData
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {

    private val repository: AppRepository = AppRepositoryImpl()

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

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _searchResults = MutableStateFlow<SearchResult?>(null)
    val searchResults: StateFlow<SearchResult?> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _syncedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val syncedContacts: StateFlow<List<Contact>> = _syncedContacts.asStateFlow()
    private var lastRealtimePostRefreshMs: Long = 0L
    private val shortLikeInFlight = mutableSetOf<Int>()
    private val shortSubscribeInFlight = mutableSetOf<Int>()
    private val shortLikeLastTapMs = mutableMapOf<Int, Long>()
    private val shortSubscribeLastTapMs = mutableMapOf<Int, Long>()
    private val shortsTapDebounceMs = 350L

    init {
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
        connectPostRealtime()
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
        viewModelScope.launch {
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

    fun createPost(content: String, mediaUrls: List<String> = emptyList()) {
        if (content.isBlank() && mediaUrls.isEmpty()) return
        viewModelScope.launch {
            repository.createPost(content.trim(), mediaUrls).collect { result ->
                if (result is Resource.Success && result.data == true) {
                    fetchPosts()
                }
            }
        }
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
        viewModelScope.launch {
            repository.getSongs().collect { _songs.value = it }
        }
    }

    fun fetchShorts() {
        viewModelScope.launch {
            repository.getShorts().collect { _shorts.value = it }
        }
    }

    fun fetchChats() {
        viewModelScope.launch {
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
        viewModelScope.launch {
            repository.getCallLogs().collect { _callLogs.value = it }
        }
    }

    fun fetchJobs() {
        viewModelScope.launch {
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
        viewModelScope.launch {
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
        viewModelScope.launch {
            repository.getChannels().collect { _channels.value = it }
        }
    }

    fun fetchContacts() {
        viewModelScope.launch {
            repository.getContacts().collect { result ->
                if (result is Resource.Success) {
                    _contacts.value = result.data ?: emptyList()
                }
            }
        }
    }

    override fun onCleared() {
        PostWebSocketClient.close()
        super.onCleared()
    }

    fun fetchStories() {
        viewModelScope.launch {
            repository.getStories().collect { _stories.value = it }
        }
    }

    fun fetchJobApplications() {
        viewModelScope.launch {
            repository.getJobApplications().collect { _applications.value = it.data ?: emptyList() }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = null
            return
        }
        viewModelScope.launch {
            _isSearching.value = true
            repository.search(query).collect { result ->
                if (result is Resource.Success) {
                    _searchResults.value = result.data
                }
                _isSearching.value = false
            }
        }
    }

    fun clearSearch() {
        _searchResults.value = null
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
}
