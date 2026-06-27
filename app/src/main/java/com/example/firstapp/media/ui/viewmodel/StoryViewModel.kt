package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.media.model.UserStory
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.util.ContextProvider
import com.example.firstapp.common.data.realtime.PostWebSocketClient
import com.example.firstapp.common.data.realtime.PostSocketListener
import com.example.firstapp.media.ui.screens.copyUriToTempFile
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
class StoryViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _stories = MutableStateFlow<Resource<List<UserStory>>>(Resource.Loading())
    val stories: StateFlow<Resource<List<UserStory>>> = _stories.asStateFlow()

    private val socketListener = object : PostSocketListener {
        override fun onOpen() {}
        override fun onPostsEvent(action: String, postId: Int?) {}
        override fun onShortsEvent(action: String, shortId: Int?) {}
        override fun onStoriesEvent(action: String, storyId: Int?) {
            fetchStories()
        }
        override fun onError(error: String) {}
    }

    init {
        fetchStories()
        PostWebSocketClient.connect(socketListener)
    }

    private val _postResult = MutableStateFlow<Resource<Boolean>?>(null)
    val postResult: StateFlow<Resource<Boolean>?> = _postResult.asStateFlow()

    fun fetchStories() {
        viewModelScope.launch {
            repository.getStories().collect {
                _stories.value = it
            }
        }
    }

    fun postStory(content: String, contentUrl: String?, type: String = "text") {
        viewModelScope.launch {
            _postResult.value = Resource.Loading()
            var finalUrl = contentUrl
            var finalType = type
            
            if (contentUrl?.startsWith("content://") == true) {
                try {
                    val uri = android.net.Uri.parse(contentUrl)
                    val context = ContextProvider.context
                    val file = copyUriToTempFile(context, uri)
                    if (file != null) {
                        val res = accountRepository.uploadFile(file, "stories").first()
                        if (res is Resource.Success && res.data != null) {
                            finalUrl = res.data.url
                            if (finalType == "text") finalType = "image"
                            file.delete()
                        } else if (res is Resource.Error) {
                            _postResult.value = Resource.Error(res.message ?: "Upload failed")
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    _postResult.value = Resource.Error("File error: ${e.message}")
                    return@launch
                }
            }
            
            repository.createStory(content, finalUrl, finalType).collect {
                _postResult.value = it
                if (it is Resource.Success) {
                    fetchStories()
                }
            }
        }
    }

    fun clearPostResult() {
        _postResult.value = null
    }

    fun markStoryAsViewed(userId: Int, storyId: Int) {
        viewModelScope.launch {
            repository.markStoryAsViewed(userId, storyId).collect {
                // Update local state if needed
            }
        }
    }

    fun muteStory(userId: Int, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteStory(userId, isMuted).collect {
                if (it is Resource.Success) {
                    fetchStories()
                }
            }
        }
    }

    fun deleteStory(storyId: Int) {
        viewModelScope.launch {
            repository.deleteStory(storyId).collect {
                if (it is Resource.Success) {
                    fetchStories()
                }
            }
        }
    }

    override fun onCleared() {
        PostWebSocketClient.disconnect(socketListener)
        super.onCleared()
    }
}
