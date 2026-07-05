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

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MediaUiState())
    val uiState: StateFlow<MediaUiState> = _uiState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

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
            override fun onPostsEvent(action: String, postId: String?) {}
            override fun onShortsEvent(action: String, shortId: String?) {}
            override fun onStoriesEvent(action: String, storyId: String?) {}
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

    fun createPost(content: String, mediaUris: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList(), channelId: String? = null) {
        if (content.isBlank() && mediaUris.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val finalUrls = mutableListOf<String>()
            mediaUris.forEach { uriStr ->
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
            socialRepository.createPost(content.trim(), finalUrls, postType, tags, channelId).collect { res ->
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
            val nextLikes = if (nextLiked) currentLikesLong + 1 else if (currentLikesLong > 0) currentLikesLong - 1 else 0L
            
            socialRepository.updatePostLikeLocally(postId, nextLikes.toString(), nextLiked)
            socialRepository.likePost(postId).collect { resource ->
                if (resource is Resource.Error) {
                    socialRepository.updatePostLikeLocally(postId, currentLikes, isLiked)
                }
            }
        }
    }

    fun repostPost(postId: String) { viewModelScope.launch { socialRepository.repostPost(postId).collect { } } }
    fun bookmarkPost(postId: String) { viewModelScope.launch { socialRepository.bookmarkPost(postId).collect { } } }

    fun createPostReply(postId: String, content: String, parentReplyId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            socialRepository.createPostReply(postId, content.trim(), parentReplyId).collect { }
        }
    }

    fun likePostReply(replyId: String) { viewModelScope.launch { socialRepository.likePostReply(replyId).collect { } } }
    fun repostPostReply(replyId: String) { viewModelScope.launch { socialRepository.repostPostReply(replyId).collect { } } }
    fun bookmarkPostReply(replyId: String) { viewModelScope.launch { socialRepository.bookmarkPostReply(replyId).collect { } } }
    
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
