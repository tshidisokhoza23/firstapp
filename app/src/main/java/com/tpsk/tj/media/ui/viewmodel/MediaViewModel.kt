package com.tpsk.tj.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.tpsk.tj.media.data.api.TrendingTopic
import com.tpsk.tj.media.data.realtime.PostSocketListener
import com.tpsk.tj.media.data.realtime.PostWebSocketClient
import com.tpsk.tj.media.model.*
import com.tpsk.tj.chat.model.Contact
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.domain.repository.SocialRepository
import com.tpsk.tj.account.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.HashMap
import java.util.ArrayList
import java.util.Locale

import com.tpsk.tj.common.data.ads.AdUnitConfig

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val accountRepository: AccountRepository,
    private val adManager: com.tpsk.tj.common.data.ads.AdManager,
    private val postWebSocketClient: PostWebSocketClient
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _shortReplies = MutableStateFlow<Resource<List<Post>>>(Resource.Success(emptyList()))
    val shortReplies: StateFlow<Resource<List<Post>>> = _shortReplies.asStateFlow()
    private var shortRepliesPostId: String? = null

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
    private val _preloadedShortAds = MutableStateFlow<List<Any>>(emptyList())
    val preloadedShortAds: StateFlow<List<Any>> = _preloadedShortAds.asStateFlow()

    private val _adsLoaded = MutableStateFlow(false)
    val adsLoaded: StateFlow<Boolean> = _adsLoaded.asStateFlow()

    private val _isGlobalPlaybackPaused = MutableStateFlow(false)
    val isGlobalPlaybackPaused: StateFlow<Boolean> = _isGlobalPlaybackPaused.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    // Session-based unique view tracking to avoid redundant API calls
    private val viewedPostsThisSession = HashSet<String>()

    // Posts/shorts/stories broadcasts arrive over the dedicated /ws/posts socket
    // (PostWebSocketClient), not the chat socket. Declared before init{} below since
    // Kotlin runs property initializers/init blocks in textual order, and init{}
    // calls connectPostRealtime() which needs this already constructed.
    // Count updates are applied in-place by RealtimeEngagementManager; only
    // structural actions (new posts/shorts) should force a full feed refresh.
    private val feedRefreshListener = object : PostSocketListener {
        override fun onOpen() {}
        override fun onPostsEvent(action: String, postId: String?, likes: Long?, reposts: Long?, shares: Long?, views: Long?, replies: Long?) {
            patchSelectedPostCounts(postId, likes, reposts, shares, views, replies)
            if (action == "created") {
                _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
            }
        }
        override fun onShortsEvent(action: String, shortId: String?, likes: Long?, reposts: Long?, shares: Long?, views: Long?, replies: Long?) {
            patchSelectedPostCounts(shortId, likes, reposts, shares, views, replies)
            if (action == "created") {
                _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
            }
        }
        override fun onStoriesEvent(action: String, storyId: String?) {
            _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
        }
        override fun onError(error: String) {}
    }

    private fun patchSelectedPostCounts(
        postId: String?,
        likes: Long?,
        reposts: Long?,
        shares: Long?,
        views: Long?,
        replies: Long?
    ) {
        val selected = _uiState.value.selectedPost ?: return
        if (postId.isNullOrBlank() || selected.id != postId) return
        _uiState.update {
            it.copy(
                selectedPost = selected.copy(
                    likes = likes ?: selected.likes,
                    repostCount = reposts ?: selected.repostCount,
                    shares = shares ?: selected.shares,
                    views = views ?: selected.views,
                    repliesCount = replies ?: selected.repliesCount
                )
            )
        }
    }

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
                com.tpsk.tj.common.util.ContextProvider.context
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
            adManager.loadNativeAds(
                context = context,
                adUnitId = AdUnitConfig.NATIVE_SHORTS_ID,
                count = 8,
                onAdsLoaded = { ads -> _preloadedShortAds.value = ads },
                onAdFailed = { _preloadedShortAds.value = emptyList() }
            )
        }
    }

    private var currentFeedType: String? = null

    fun loadFeed(type: String, categoryId: String? = null, forceReload: Boolean = false) {
        val currentCategory = _uiState.value.selectedCategoryId
        if (!forceReload && currentFeedType == type && currentCategory == categoryId) return
        currentFeedType = type

        _uiState.update { 
            it.copy(
                selectedCategoryId = categoryId,
                posts = socialRepository.getPostsPaging(
                    following = (type == "following"),
                    authorId = "",
                    feedType = type,
                    categoryId = if (categoryId == "all") null else categoryId
                ).cachedIn(viewModelScope)
            )
        }
    }

    fun selectCategory(categoryId: String?) {
        loadFeed(currentFeedType ?: "standard", categoryId)
    }

    private fun connectPostRealtime() {
        postWebSocketClient.connect(feedRefreshListener)
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

    fun getSavedPostsPaging(): Flow<PagingData<Post>> {
        return socialRepository.getPostsPaging(following = false, authorId = "", feedType = "standard", categoryId = "saved").cachedIn(viewModelScope)
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

    fun search(query: String, isGlobal: Boolean = false) {
        currentSearchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = Resource.Success(emptyList())
            return
        }

        val cacheKey = "${query}_$isGlobal"
        val now = System.currentTimeMillis()
        val cached = searchCache.get(cacheKey)
        if (cached != null) {
            val ts = cached.second
            if ((now - ts) < searchTtlMs) {
                _searchResults.value = Resource.Success(cached.first)
                return
            }
        }

        currentSearchJob = viewModelScope.launch {
            _searchResults.value = Resource.Loading()
            socialRepository.searchPosts(query, isGlobal).collect { res ->
                _searchResults.value = res
                if (res is Resource.Success) {
                    searchCache.put(cacheKey, Pair(res.data ?: emptyList(), System.currentTimeMillis()))
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
        rootPostId: String? = null,
        repostOfId: String? = null
    ) {
        if (content.isBlank() && mediaUris.isEmpty() && (pollOptions == null || pollOptions.isEmpty()) && repostOfId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val finalUrls = ArrayList<String>()
            var thumbnailUrl: String? = null
            
            val context = com.tpsk.tj.common.util.ContextProvider.context

            for (uriStr in mediaUris) {
                android.util.Log.d("MediaViewModel", "Processing media URI: $uriStr")
                val uri = if (uriStr.startsWith("/")) {
                     android.net.Uri.fromFile(java.io.File(uriStr))
                } else {
                     android.net.Uri.parse(uriStr)
                }

                val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true ||
                    MediaUtils.isVideo(uriStr)

                // Generate a thumbnail for the first selected video.
                if (thumbnailUrl == null && isVideo) {
                    val thumbFile = MediaUtils.getVideoThumbnail(context, uri)
                    if (thumbFile != null) {
                        accountRepository.uploadFile(thumbFile, "thumbnails").collect { res ->
                            if (res is Resource.Success && res.data != null) {
                                thumbnailUrl = res.data.url
                                thumbFile.delete()
                            }
                        }
                    }
                }

                if (uriStr.startsWith("content://") || uriStr.startsWith("file://") || uriStr.startsWith("/")) {
                    try {
                        if (uri != null) {
                            var uploadSuccess = false
                            accountRepository.uploadUri(context, uri, "posts").collect { res ->
                                if (res is Resource.Success && res.data != null) {
                                    finalUrls.add(res.data.url)
                                    uploadSuccess = true
                                } else if (res is Resource.Error) {
                                    _toastMessage.value = "Upload failed: ${res.message}"
                                }
                            }
                            if (!uploadSuccess) {
                                _uiState.update { it.copy(isLoading = false) }
                                return@launch // Fail early if upload failed
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MediaViewModel", "File error: ${e.message}")
                    }
                } else {
                    finalUrls.add(uriStr)
                }
            }
            
            val finalMediaType = if (postType == "short") "VIDEO" else (mediaType ?: if (finalUrls.size > 0) {
                if (isMediaVideo(context, mediaUris.firstOrNull(), finalUrls[0])) "VIDEO" else "IMAGE"
            } else "TEXT")

            if (finalUrls.size == 0 && content.length == 0 && (pollOptions == null || pollOptions.size == 0) && repostOfId == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            socialRepository.createPost(content.trim(), finalUrls, postType, tags, channelId, pollOptions, location, finalMediaType, parentPostId, rootPostId, repostOfId = repostOfId, thumbnailUrl = thumbnailUrl).collect { res ->
                if (res is Resource.Success) {
                    _toastMessage.value = if (parentPostId != null) "Reply posted" else "Post created successfully"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                    loadFeed(type = postType, forceReload = true)
                    
                    // REQUIREMENT: Refresh thread immediately after replying
                    if (parentPostId != null || rootPostId != null) {
                        val countTarget = rootPostId ?: parentPostId
                        if (countTarget != null) {
                            socialRepository.incrementPostRepliesLocally(countTarget)
                            val selected = _uiState.value.selectedPost
                            if (selected != null && selected.id == countTarget) {
                                _uiState.update { it.copy(selectedPost = selected.copy(repliesCount = selected.repliesCount + 1)) }
                            }
                        }
                        val currentSelectedId = _uiState.value.selectedPost?.id
                        if (currentSelectedId != null) {
                            fetchReplies(currentSelectedId)
                        }
                    }
                } else if (res is Resource.Error) {
                    _toastMessage.value = "Failed to post: ${res.message}"
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    private fun isMediaVideo(context: android.content.Context, sourceUri: String?, uploadedUrl: String): Boolean {
        val uri = sourceUri?.let { android.net.Uri.parse(it) }
        return (uri != null && context.contentResolver.getType(uri)?.startsWith("video/") == true) ||
            MediaUtils.isVideo(sourceUri) ||
            MediaUtils.isVideo(uploadedUrl)
    }

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

    fun repostPost(postId: String, currentReposts: Long, isReposted: Boolean) {
        viewModelScope.launch {
            val nextReposted = !isReposted
            val nextRepostsCount = if (nextReposted) currentReposts + 1 else if (currentReposts > 0) currentReposts - 1 else 0L
            
            socialRepository.updatePostRepostLocally(postId, nextRepostsCount, nextReposted)
            
            val selected = _uiState.value.selectedPost
            if (selected != null && selected.id == postId) {
                _uiState.update { it.copy(selectedPost = selected.copy(isReposted = nextReposted, repostCount = nextRepostsCount)) }
            }

            socialRepository.repostPost(postId).collect { resource ->
                if (resource is Resource.Error) {
                    socialRepository.updatePostRepostLocally(postId, currentReposts, isReposted)
                    if (selected != null && selected.id == postId) {
                        _uiState.update { it.copy(selectedPost = selected) }
                    }
                    _toastMessage.value = "Failed to repost: ${resource.message}"
                } else if (resource is Resource.Success) {
                    _toastMessage.value = if (nextReposted) "Reposted successfully" else "Repost removed"
                    if (nextReposted) trackEngagement(postId, "share", "entertainment")
                }
            }
        }
    }

    fun sharePost(postId: String, currentShares: Long) {
        viewModelScope.launch {
            val nextSharesCount = currentShares + 1
            
            socialRepository.updatePostShareLocally(postId, nextSharesCount)
            
            val selected = _uiState.value.selectedPost
            if (selected != null && selected.id == postId) {
                _uiState.update { it.copy(selectedPost = selected.copy(shares = nextSharesCount)) }
            }

            socialRepository.sharePost(postId).collect { resource ->
                if (resource is Resource.Error) {
                    socialRepository.updatePostShareLocally(postId, currentShares)
                    if (selected != null && selected.id == postId) {
                        _uiState.update { it.copy(selectedPost = selected) }
                    }
                } else if (resource is Resource.Success) {
                    trackEngagement(postId, "share", "entertainment")
                }
            }
        }
    }

    fun viewPost(postId: String) {
        if (viewedPostsThisSession.contains(postId)) return
        viewedPostsThisSession.add(postId)
        
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
        android.util.Log.d("MediaViewModel", "fetchShortReplies called for post: $postId")
        shortRepliesPostId = postId
        viewModelScope.launch {
            _shortReplies.value = Resource.Loading()
            socialRepository.getReplies(postId).collect { resource ->
                android.util.Log.d("MediaViewModel", "fetchShortReplies received resource: ${resource::class.simpleName}, count=${resource.data?.size ?: 0}")
                if (shortRepliesPostId == postId) {
                    _shortReplies.value = resource
                }
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
    fun followUser(userId: String) {
        val currentUserId = try { 
            com.tpsk.tj.common.util.ContextProvider.context.getSharedPreferences("session", android.content.Context.MODE_PRIVATE).getString("user_id", "")
        } catch(_: Exception) { "" }
        
        if (userId == currentUserId) {
            android.util.Log.w("MediaViewModel", "Attempted to follow self: $userId")
            return
        }

        viewModelScope.launch {
            // Local optimistic update
            socialRepository.updateAuthorFollowStatusLocally(userId, true)
            
            socialRepository.followUser(userId).collect { resource ->
                if (resource is Resource.Success) {
                    fetchSuggestedUsers()
                    _toastMessage.value = "Followed user"
                } else if (resource is Resource.Error) {
                    // Rollback on error
                    socialRepository.updateAuthorFollowStatusLocally(userId, false)
                    _toastMessage.value = "Failed to follow: ${resource.message}"
                }
            }
        }
    }

    fun unfollowUser(userId: String) {
        viewModelScope.launch {
            // Local optimistic update
            socialRepository.updateAuthorFollowStatusLocally(userId, false)
            
            socialRepository.unfollowUser(userId).collect { resource ->
                if (resource is Resource.Success) {
                    fetchSuggestedUsers()
                    _toastMessage.value = "Unfollowed user"
                } else if (resource is Resource.Error) {
                    // Rollback on error
                    socialRepository.updateAuthorFollowStatusLocally(userId, true)
                    _toastMessage.value = "Failed to unfollow: ${resource.message}"
                }
            }
        }
    }
    fun votePoll(postId: String, optionId: String) { viewModelScope.launch { socialRepository.votePoll(postId, optionId).collect { } } }

    fun setGlobalPlaybackPaused(paused: Boolean) {
        _isGlobalPlaybackPaused.value = paused
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    override fun onCleared() {
        postWebSocketClient.disconnect(feedRefreshListener)
        super.onCleared()
    }
}
