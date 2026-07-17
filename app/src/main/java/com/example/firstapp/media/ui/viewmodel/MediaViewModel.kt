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

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _searchResults = MutableStateFlow<Resource<List<Post>>>(Resource.Success(emptyList()))
    val searchResults: StateFlow<Resource<List<Post>>> = _searchResults.asStateFlow()

    private var currentSearchJob: kotlinx.coroutines.Job? = null
    private val searchCache = HashMap<String, Pair<List<Post>, Long>>()
    private val searchTtlMs = 120_000L

    init {
        _uiState.update { 
            it.copy(
                posts = socialRepository.getPostsPaging(following = false).cachedIn(viewModelScope)
            )
        }
        connectPostRealtime()
    }

    private fun connectPostRealtime() {
        PostWebSocketClient.connect(object : PostSocketListener {
            override fun onOpen() {}
            override fun onPostsEvent(action: String, postId: String?) {
                _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
            }
            override fun onShortsEvent(action: String, shortId: String?) {
                // Shorts are handled in ShortsViewModel
            }
            override fun onStoriesEvent(action: String, storyId: String?) {
                _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
            }
            override fun onError(error: String) {}
        })
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTab = index) }
    }

    fun getUserPosts(userId: String): Flow<PagingData<Post>> {
        return socialRepository.getPostsPaging(authorId = userId).cachedIn(viewModelScope)
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
            socialRepository.getPosts(1).collect { res ->
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

    fun createPost(content: String, mediaUris: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList(), channelId: String? = null, pollOptions: List<String>? = null, location: String? = null) {
        if (content.isBlank() && mediaUris.isEmpty() && (pollOptions == null || pollOptions.size == 0)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val finalUrls = ArrayList<String>()
            for (uriStr in mediaUris) {
                if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                    try {
                        val uri = android.net.Uri.parse(uriStr)
                        val context = com.example.firstapp.common.util.ContextProvider.context
                        val file = com.example.firstapp.media.ui.screens.copyUriToTempFile(context, uri)
                        if (file != null) {
                            accountRepository.uploadFile(file, "posts").collect { res ->
                                if (res is Resource.Success && res.data != null) {
                                    finalUrls.add(res.data.url)
                                    file.delete()
                                }
                            }
                        }
                    } catch (_: Exception) { }
                } else {
                    finalUrls.add(uriStr)
                }
            }
            socialRepository.createPost(content.trim(), finalUrls, postType, tags, channelId, pollOptions, location).collect { res ->
                if (res is Resource.Success) {
                    _toastMessage.value = "Post created successfully"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                } else if (res is Resource.Error) {
                    _toastMessage.value = "Failed to create post: ${res.message}"
                }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun likePost(postId: String, currentLikes: String, isLiked: Boolean) {
        viewModelScope.launch {
            val nextLiked = !isLiked
            val currentLikesLong = try { currentLikes.toLong() } catch (_: Exception) { 0L }
            val nextLikesCount = if (nextLiked) currentLikesLong + 1 else if (currentLikesLong > 0) currentLikesLong - 1 else 0L
            
            socialRepository.updatePostLikeLocally(postId, nextLikesCount.toString(), nextLiked)
            
            val selected = _uiState.value.selectedPost
            if (selected != null && selected.id == postId) {
                _uiState.update { it.copy(selectedPost = selected.copy(isLiked = nextLiked, likes = nextLikesCount.toString())) }
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
            // Check if it's the selected post (Detail View)
            val selected = _uiState.value.selectedPost
            if (selected != null && selected.id == postId) {
                val nextReposted = !selected.isReposted
                val currentShares = try { selected.shares.toLong() } catch (_: Exception) { 0L }
                val nextSharesCount = if (nextReposted) currentShares + 1 else if (currentShares > 0) currentShares - 1 else 0L
                
                socialRepository.updatePostRepostLocally(postId, nextSharesCount.toString(), nextReposted)
                _uiState.update { it.copy(selectedPost = selected.copy(isReposted = nextReposted, shares = nextSharesCount.toString())) }
                
                socialRepository.repostPost(postId).collect { resource ->
                    if (resource is Resource.Error) {
                        socialRepository.updatePostRepostLocally(postId, selected.shares, selected.isReposted)
                        _uiState.update { it.copy(selectedPost = selected) }
                        _toastMessage.value = "Failed to repost: ${resource.message}"
                    } else if (resource is Resource.Success) {
                        _toastMessage.value = "Reposted successfully"
                    }
                }
                return@launch
            }

            // If not selected, we assume it's from the feed. 
            // Paging library doesn't easily allow updating a single item without a full refresh 
            // unless we use a custom data structure or notify the adapter.
            // For now, we update local DB which the Pager (if using DB as source) might pick up on refresh
            // but we'll also trigger a network call and a refresh.
            socialRepository.repostPost(postId).collect { resource ->
                if (resource is Resource.Success) {
                    _toastMessage.value = "Reposted successfully"
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                } else if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to repost: ${resource.message}"
                }
            }
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

    fun createPostReply(postId: String, content: String, parentReplyId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            socialRepository.createPostReply(postId, content.trim(), parentReplyId).collect { resource ->
                if (resource is Resource.Success && resource.data == true) {
                    val selected = _uiState.value.selectedPost
                    if (selected != null && selected.id == postId) {
                        val currentCount = selected.repliesCount.toLongOrNull() ?: 0L
                        _uiState.update {
                            it.copy(selectedPost = selected.copy(repliesCount = (currentCount + 1).toString()))
                        }
                        fetchReplies(postId)
                    }
                    _uiState.update { it.copy(refreshTrigger = it.refreshTrigger + 1) }
                } else if (resource is Resource.Error) {
                    _toastMessage.value = "Failed to add reply: ${resource.message}"
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
    
    fun viewPost(postId: String) { 
        viewModelScope.launch { socialRepository.viewPost(postId).collect { } } 
    }

    fun selectPost(post: Post?) {
        _uiState.update { it.copy(selectedPost = post) }
        if (post != null) {
            viewPost(post.id)
            fetchReplies(post.id)
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
    fun votePoll(postId: String, optionId: String) { viewModelScope.launch { socialRepository.votePoll(postId, optionId).collect { } } }

    override fun onCleared() {
        // PostWebSocketClient.close()
        super.onCleared()
    }
}
