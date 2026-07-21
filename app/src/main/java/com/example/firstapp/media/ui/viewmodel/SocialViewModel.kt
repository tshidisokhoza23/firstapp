package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.media.data.api.TrendingTopic
import com.example.firstapp.common.data.realtime.PostSocketListener
import com.example.firstapp.common.data.realtime.PostWebSocketClient
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.account.domain.repository.AccountRepository

@HiltViewModel
class SocialViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val posts: StateFlow<Resource<List<Post>>> = _posts.asStateFlow()

    private val _trendingTopics = MutableStateFlow<Resource<List<TrendingTopic>>>(Resource.Loading())
    val trendingTopics: StateFlow<Resource<List<TrendingTopic>>> = _trendingTopics.asStateFlow()

    private val _suggestedUsers = MutableStateFlow<Resource<List<Contact>>>(Resource.Loading())
    val suggestedUsers: StateFlow<Resource<List<Contact>>> = _suggestedUsers.asStateFlow()

    private val _stories = MutableStateFlow<Resource<List<UserStory>>>(Resource.Loading())
    val stories: StateFlow<Resource<List<UserStory>>> = _stories.asStateFlow()

    private var lastRealtimePostRefreshMs: Long = 0L

    init {
        fetchPosts()
        fetchTrendingTopics()
        fetchSuggestedUsers()
        fetchStories()
        connectPostRealtime()
    }

    private fun connectPostRealtime() {
        PostWebSocketClient.connect(object : PostSocketListener {
            override fun onOpen() {}
            override fun onPostsEvent(action: String, postId: String?) {
                val now = System.currentTimeMillis()
                if (now - lastRealtimePostRefreshMs < 800) return
                lastRealtimePostRefreshMs = now
                fetchPosts()
            }
            override fun onShortsEvent(action: String, shortId: String?) {
            }
            override fun onStoriesEvent(action: String, storyId: String?) {
                fetchStories()
            }
            override fun onError(error: String) {}
        })
    }

    fun fetchPosts() {
        viewModelScope.launch {
            socialRepository.getPosts().collect { _posts.value = it }
        }
    }

    fun fetchTrendingTopics() {
        viewModelScope.launch {
            socialRepository.getTrendingTopics().collect { _trendingTopics.value = it }
        }
    }

    fun fetchSuggestedUsers() {
        viewModelScope.launch {
            socialRepository.getSuggestedUsers().collect { _suggestedUsers.value = it }
        }
    }

    fun fetchStories() {
        viewModelScope.launch {
            socialRepository.getStories().collect { _stories.value = it }
        }
    }

    fun createPost(content: String, mediaUris: List<String> = emptyList(), postType: String? = null, tags: List<String> = emptyList()) {
        if (content.isBlank() && mediaUris.isEmpty()) return
        viewModelScope.launch {
            val finalUrls = mutableListOf<String>()
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
                    } catch (e: Exception) { }
                } else {
                    finalUrls.add(uriStr)
                }
            }
            socialRepository.createPost(content.trim(), finalUrls, postType, tags).collect { result ->
                if (result is Resource.Success && result.data == true) fetchPosts()
            }
        }
    }

    fun likePost(postId: String) { viewModelScope.launch { socialRepository.likePost(postId).collect { } } }
    fun repostPost(postId: String) { viewModelScope.launch { socialRepository.repostPost(postId).collect { } } }
    fun bookmarkPost(postId: String) { viewModelScope.launch { socialRepository.bookmarkPost(postId).collect { } } }

    fun createPostReply(postId: String, content: String, parentReplyId: String? = null) {
        if (content.isBlank()) return
        viewModelScope.launch {
            socialRepository.createPostReply(postId, content.trim(), parentReplyId).collect { result ->
                if (result is Resource.Success && result.data == true) fetchPosts()
            }
        }
    }

    fun likePostReply(replyId: String) { viewModelScope.launch { socialRepository.likePostReply(replyId).collect { } } }
    fun repostPostReply(replyId: String) { viewModelScope.launch { socialRepository.repostPostReply(replyId).collect { } } }
    fun bookmarkPostReply(replyId: String) { viewModelScope.launch { socialRepository.bookmarkPostReply(replyId).collect { } } }
    
    fun viewPost(postId: String) { viewModelScope.launch { socialRepository.viewPost(postId).collect { } } }
    fun deletePost(postId: String) {
        viewModelScope.launch {
            socialRepository.deletePost(postId).collect { result ->
                if (result is Resource.Success) {
                    val current = _posts.value.data ?: return@collect
                    _posts.value = Resource.Success(current.filter { it.id != postId })
                }
            }
        }
    }

    fun followUser(userId: String) { viewModelScope.launch { socialRepository.followUser(userId).collect { if (it is Resource.Success) { fetchSuggestedUsers(); fetchPosts() } } } }
    fun unfollowUser(userId: String) { viewModelScope.launch { socialRepository.unfollowUser(userId).collect { if (it is Resource.Success) { fetchSuggestedUsers(); fetchPosts() } } } }
    fun votePoll(postId: String, optionId: String) { viewModelScope.launch { socialRepository.votePoll(postId, optionId).collect { if (it is Resource.Success) fetchPosts() } } }

    fun postStory(content: String, contentUrl: String?) {
        viewModelScope.launch {
            var finalUrl = contentUrl
            if (contentUrl != null && (contentUrl.startsWith("content://") || contentUrl.startsWith("file://"))) {
                try {
                    val uri = android.net.Uri.parse(contentUrl)
                    val context = com.example.firstapp.common.util.ContextProvider.context
                    val file = com.example.firstapp.media.ui.screens.copyUriToTempFile(context, uri)
                    if (file != null) {
                        val res = accountRepository.uploadFile(file, "stories").first()
                        if (res is Resource.Success && res.data != null) {
                            finalUrl = res.data.url
                            file.delete()
                        } else if (res is Resource.Error) {
                            // If upload fails, we shouldn't proceed to create the story with a local URL
                            _stories.value = Resource.Error(res.message ?: "Upload failed")
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    _stories.value = Resource.Error("File error: ${e.message}")
                    return@launch
                }
            }
            val type = if (finalUrl != null) "image" else "text"
            socialRepository.createStory(content, finalUrl, type).collect { 
                if (it is Resource.Success) {
                    fetchStories()
                } else if (it is Resource.Error) {
                    _stories.value = Resource.Error(it.message ?: "Failed to post story")
                }
            }
        }
    }

    fun markStoryAsViewed(userId: String, storyId: String) { viewModelScope.launch { socialRepository.markStoryAsViewed(userId, storyId).collect { } } }
    fun muteStory(userId: String, isMuted: Boolean) { viewModelScope.launch { socialRepository.muteStory(userId, isMuted).collect { } } }

    override fun onCleared() {
        // PostWebSocketClient.close() - using disconnect if appropriate or nothing
        super.onCleared()
    }
}
