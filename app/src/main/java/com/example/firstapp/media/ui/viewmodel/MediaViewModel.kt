package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.firstapp.media.data.api.TrendingTopic
import com.example.firstapp.common.data.realtime.PostSocketListener
import com.example.firstapp.common.data.realtime.PostWebSocketClient
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.account.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.HashMap
import java.util.ArrayList
import java.util.Locale

import com.example.firstapp.common.data.ads.AdUnitConfig

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val accountRepository: AccountRepository,
    private val adManager: com.example.firstapp.common.data.ads.AdManager,
    private val postWebSocketClient: com.example.firstapp.common.data.realtime.PostWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _shortReplies = MutableStateFlow<Resource<List<Post>>>(Resource.Success(emptyList()))
    val shortReplies: StateFlow<Resource<List<Post>>> = _shortReplies.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<Resource<List<Post>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<Post>>> = _searchResults.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null
    private val searchCache = HashMap<String, Pair<List<Post>, Long>>()
    private val searchTtlMs = 120_000L

    private val _preloadedAds = MutableStateFlow<List<Any>>(emptyList())
    val preloadedAds: StateFlow<List<Any>> = _preloadedAds.asStateFlow()

    private val _adsLoaded = MutableStateFlow(false)
    val adsLoaded: StateFlow<Boolean> = _adsLoaded.asStateFlow()

    init {
        preloadAds()
        loadFeed("standard")
        connectPostRealtime()
        fetchSuggestedUsers()
    }

    private fun preloadAds() {
        viewModelScope.launch {
            android.util.Log.d("MediaViewModel", "Starting native ad preload...")
            val context = try {
                com.example.firstapp.common.util.ContextProvider.context
            } catch (e: Exception) {
                android.util.Log.e("MediaViewModel", "Context provider error: ${e.message}")
                null
            }
            if (context == null) {
                android.util.Log.w("MediaViewModel", "No context available for ad preloading")
                _adsLoaded.value = true
                return@launch
            }
            adManager.loadNativeAds(
                context = context,
                adUnitId = AdUnitConfig.NATIVE_POSTS_ID,
                count = 8, // 30 posts / 4 = 7.5 -> 8 ads
                onAdsLoaded = { ads ->
                    android.util.Log.d("MediaViewModel", "Successfully preloaded ${ads.size} native ads")
                    _preloadedAds.value = ads
                    _adsLoaded.value = true
                },
                onAdFailed = { error ->
                    android.util.Log.e("MediaViewModel", "Native ad preload failed: $error")
                    _adsLoaded.value = true // Continue anyway
                }
            )
        }
    }

    private var currentFeedType: String? = null

    fun loadFeed(type: String) {
        if (currentFeedType == type) return
        currentFeedType = type

        _uiState.update { 
            it.copy(
                posts = socialRepository.getPostsPaging(
                    following = (type == "following"),
                    authorId = "",
                    feedType = type
                ).cachedIn(viewModelScope)
            )
        }
    }

    private var lastRealtimePostRefreshMs: Long = 0L

    private fun connectPostRealtime() {
        postWebSocketClient.connect(object : com.example.firstapp.common.data.realtime.PostSocketListener {
            override fun onOpen() {}
            override fun onPostsEvent(action: String, postId: String?) {
                val now = System.currentTimeMillis()
                if (now - lastRealtimePostRefreshMs < 1500) return
                lastRealtimePostRefreshMs = now
                _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
            }
            override fun onShortsEvent(action: String, shortId: String?) {
            }
            override fun onStoriesEvent(action: String, storyId: String?) {
                _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
            }
            override fun onError(error: String) {}
        })
    }

    fun refreshAll() {
        _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
        fetchSuggestedUsers()
        fetchTrendingTopics()
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun getUserPosts(userId: String, type: String = "standard"): Flow<PagingData<Post>> {
        val normalizedId = if (userId == "null") "" else userId
        return socialRepository.getPostsPaging(following = false, authorId = normalizedId, feedType = type).cachedIn(viewModelScope)
    }

    fun fetchTrendingTopics() {
        viewModelScope.launch {
            socialRepository.getTrendingTopics().collect { res ->
                if (res is Resource.Success) {
                    _uiState.update { it.copy(trendingTopics = res.data ?: emptyList()) }
                }
            }
        }
    }

    fun fetchSuggestedUsers() {
        viewModelScope.launch {
            socialRepository.getSuggestedUsers().collect { res ->
                if (res is Resource.Success) {
                    _uiState.update { it.copy(suggestedUsers = res.data ?: emptyList()) }
                }
            }
        }
    }

    fun search(query: String) {
        currentSearchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        val now = System.currentTimeMillis()
        val cached = searchCache.get(query)
        if (cached != null) {
            val ts = cached.second
            if ((now - ts) < searchTtlMs) {
                _searchResults.value = Resource.Success(cached.first)
                return
            }
        }

        currentSearchJob = viewModelScope.launch {
            _searchResults.value = Resource.Loading()
            socialRepository.getPosts(1, "standard").collect { res ->
                if (res is Resource.Success) {
                    val posts = res.data ?: emptyList()
                    val filtered = ArrayList<Post>()
                    val q = query.lowercase(Locale.getDefault())
                    for (p in posts) {
                        val content = p.content.lowercase(Locale.getDefault())
                        val author = p.authorName.lowercase(Locale.getDefault())
                        if (content.indexOf(q) >= 0 || author.indexOf(q) >= 0) {
                            filtered.add(p)
                        }
                    }
                    _searchResults.value = Resource.Success(filtered)
                    searchCache.put(query, Pair(filtered, System.currentTimeMillis()))
                } else if (res is Resource.Error) {
                    _searchResults.value = Resource.Error(res.message ?: "Search failed")
                }
            }
        }
    }

    fun createPost(
        content: String, 
        mediaUris: List<String> = emptyList(), 
        postType: String = "standard", 
        mediaType: String? = null,
        tags: List<String> = emptyList(), 
        channelId: String? = null, 
        pollOptions: List<String>? = null, 
        location: String? = null,
        parentPostId: String? = null,
        rootPostId: String? = null
    ) {
        if (content.isBlank() && mediaUris.isEmpty() && (pollOptions == null || pollOptions.isEmpty())) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val finalUrls = ArrayList<String>()
            for (uriStr in mediaUris) {
                android.util.Log.d("MediaViewModel", "Processing media URI: $uriStr")
                if (uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("/")) {
                    try {
                        val uri = if (uriStr.startsWith("/")) {
                             android.net.Uri.fromFile(java.io.File(uriStr))
                        } else {
                             android.net.Uri.parse(uriStr)
                        }
                        val context = com.example.firstapp.common.util.ContextProvider.context
                        val file = com.example.firstapp.media.ui.screens.copyUriToTempFile(context, uri)
                        if (file != null) {
                            android.util.Log.d("MediaViewModel", "File copied to temp: ${file.absolutePath}, size: ${file.length()}")
                            // Synchronously wait for each upload to complete and get the public URL
                            accountRepository.uploadFile(file, "posts").collect { res ->
                                if (res is Resource.Success && res.data != null) {
                                    android.util.Log.d("MediaViewModel", "Upload success: ${res.data.url}")
                                    finalUrls.add(res.data.url)
                                    file.delete()
                                } else if (res is Resource.Error) {
                                    android.util.Log.e("MediaViewModel", "Upload failed for ${file.name}: ${res.message}")
                                    _toastMessage.value = "Upload failed: ${res.message}"
                                }
                            }
                        } else {
                            android.util.Log.e("MediaViewModel", "Failed to copy URI to temp file: $uri")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MediaViewModel", "File error: ${e.message}")
                    }
                } else {
                    finalUrls.add(uriStr)
                }
            }
            
            // Auto-detect media type if not provided
            val finalMediaType = if (postType == "short") "VIDEO" else (mediaType ?: if (finalUrls.size > 0) {
                if (com.example.firstapp.common.util.MediaUtils.isVideo(finalUrls[0])) "VIDEO" else "IMAGE"
            } else "TEXT")
            
            android.util.Log.d("MediaViewModel", "READY TO CREATE POST: mediaCount=" + finalUrls.size + ", type=" + finalMediaType)
            var idx = 0
            while (idx < finalUrls.size) {
                android.util.Log.d("MediaViewModel", "  URL[" + idx + "]: " + finalUrls[idx])
                idx++
            }

            if (finalUrls.size == 0 && content.length == 0 && (pollOptions == null || pollOptions.size == 0)) {
                android.util.Log.w("MediaViewModel", "Aborting createPost: No content and no media")
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            socialRepository.createPost(content.trim(), finalUrls, postType, tags, channelId, pollOptions, location, finalMediaType, parentPostId, rootPostId).collect { res ->
                if (res is Resource.Success) {
                    android.util.Log.d("MediaViewModel", "Post created SUCCESS. Data: ${res.data}")
                    _toastMessage.value = if (parentPostId != null) "Reply posted" else "Post created successfully"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                } else if (res is Resource.Error) {
                    android.util.Log.e("MediaViewModel", "Post creation FAILED: ${res.message}")
                    _toastMessage.value = "Failed to post: ${res.message}"
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun trackEngagement(
        postId: String, 
        eventType: String, 
        category: String,
        watchTimeMs: Int? = null,
        completed: Boolean? = null
    ) {
        viewModelScope.launch {
            socialRepository.trackEngagement(postId, eventType, category, watchTimeMs, completed).collect { }
        }
    }

    fun likePost(postId: String, currentLikes: Long, isLiked: Boolean) {
        viewModelScope.launch {
            val nextLiked = !isLiked
            val nextLikesCount = if (nextLiked) currentLikes + 1 else if (currentLikes > 0) currentLikes - 1 else 0L
            
            socialRepository.updatePostLikeLocally(postId, nextLikesCount, nextLiked)
            
            val selected = _uiState.value.selectedPost
            if (selected != null && selected.id == postId) {
                _uiState.update { it.copy(selectedPost = selected.copy(isLiked = nextLiked, likes = nextLikesCount)) }
            }

            if (nextLiked) {
                trackEngagement(postId, "like", "entertainment")
            }

            socialRepository.likePost(postId).collect { resource ->
                if (resource is Resource.Error) {
                    socialRepository.updatePostLikeLocally(postId, currentLikes, isLiked)
                    if (selected != null && selected.id == postId) {
                        _uiState.update { it.copy(selectedPost = selected) }
                    }
                    _toastMessage.value = "Failed to like post: ${resource.message}"
                }
            }
        }
    }

    fun repostPost(postId: String) {
        viewModelScope.launch {
            // ... logic ...
            socialRepository.repostPost(postId).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "Reposted successfully"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                    trackEngagement(postId, "share", "entertainment")
                } else if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to repost: ${resource.message}"
                }
            }
        }
    }

    fun viewPost(postId: String) {
        viewModelScope.launch {
            socialRepository.viewPost(postId).collect { }
            trackEngagement(postId, "view", "entertainment")
        }
    }

    fun bookmarkPost(postId: String) {
        viewModelScope.launch {
            val selected = _uiState.value.selectedPost
            if (selected != null && selected.id == postId) {
                val nextBookmarked = !selected.isBookmarked
                
                socialRepository.updatePostBookmarkLocally(postId, nextBookmarked)
                _uiState.update { it.copy(selectedPost = selected.copy(isBookmarked = nextBookmarked)) }

                socialRepository.bookmarkPost(postId).collect { resource ->
                    if (resource is Resource.Error) {
                        socialRepository.updatePostBookmarkLocally(postId, selected.isBookmarked)
                        _uiState.update { it.copy(selectedPost = selected) }
                        _toastMessage.value = "Failed to bookmark: ${resource.message}"
                    } else if (resource is Resource.Success) {
                        _toastMessage.value = "Bookmarked successfully"
                    }
                }
                return@launch
            }

            socialRepository.bookmarkPost(postId).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "Bookmarked successfully"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                } else if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to bookmark: ${resource.message}"
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            socialRepository.deletePost(postId).collect { res ->
                if (res is Resource.Success) {
                    _toastMessage.value = "Post deleted"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                } else if (res is Resource.Error) {
                    _toastMessage.value = "Failed to delete: ${res.message}"
                }
            }
        }
    }

    fun likePostReply(replyId: String) { viewModelScope.launch { socialRepository.likePostReply(replyId).collect { } } }
    fun repostPostReply(replyId: String) { viewModelScope.launch { socialRepository.repostPostReply(replyId).collect { } } }
    fun bookmarkPostReply(replyId: String) { viewModelScope.launch { socialRepository.bookmarkPostReply(replyId).collect { } } }

    fun deletePostReply(replyId: String) {
        viewModelScope.launch {
            socialRepository.deletePostReply(replyId).collect { res ->
                if (res is Resource.Success) {
                    _toastMessage.value = "Reply deleted"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                } else if (res is Resource.Error) {
                    _toastMessage.value = "Failed to delete: ${res.message}"
                }
            }
        }
    }
    
    fun onAdRevenueLogged(postId: String, revenue: Double, adUnitId: String) {
        viewModelScope.launch {
            socialRepository.trackAdRevenue(postId, revenue, adUnitId).collect { }
        }
    }

    fun selectPost(post: Post?) {
        _uiState.update { it.copy(selectedPost = post) }
        if (post != null) {
            viewPost(post.id)
            fetchReplies(post.id)
        }
    }

    fun selectPostById(postId: String) {
        viewModelScope.launch {
            // If already loading or selected this post, don't restart
            if (_uiState.value.selectedPost?.id == postId) return@launch
            
            // Clear previous so UI shows loading state
            _uiState.update { it.copy(selectedPost = null) }
            
            socialRepository.getPostById(postId).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _uiState.update { it.copy(errorMessage = null) }
                    selectPost(res.data)
                } else if (res is Resource.Error) {
                    android.util.Log.e("MediaViewModel", "Failed to load post $postId: ${res.message}")
                    _uiState.update { it.copy(errorMessage = res.message ?: "Failed to load post") }
                } else if (res is Resource.Loading) {
                    _uiState.update { it.copy(errorMessage = null) }
                }
            }
        }
    }

    fun fetchShortReplies(postId: String) {
        viewModelScope.launch {
            _shortReplies.value = Resource.Loading()
            socialRepository.getReplies(postId).collect { resource ->
                _shortReplies.value = resource
            }
        }
    }

    private fun fetchReplies(postId: String) {
        viewModelScope.launch {
            socialRepository.getReplies(postId).collect { resource ->
                if (resource is Resource.Success) {
                    _uiState.update { state ->
                        val current = state.selectedPost
                        if (current != null && current.id == postId) {
                            state.copy(selectedPost = current.copy(replies = resource.data ?: emptyList()))
                        } else state
                    }
                }
            }
        }
    }
    fun followUser(userId: String) { viewModelScope.launch { socialRepository.followUser(userId).collect { if (it is Resource.Success) fetchSuggestedUsers() } } }
    fun unfollowUser(userId: String) { viewModelScope.launch { socialRepository.unfollowUser(userId).collect { if (it is Resource.Success) fetchSuggestedUsers() } } }
    fun votePoll(postId: String, optionId: String) { viewModelScope.launch { socialRepository.votePoll(postId, optionId).collect { } } }

    override fun onCleared() {
        // PostWebSocketClient.close()
        super.onCleared()
    }
}
