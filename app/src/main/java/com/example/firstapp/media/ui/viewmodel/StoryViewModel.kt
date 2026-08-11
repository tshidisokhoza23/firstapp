package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.media.model.UserStory
import com.example.firstapp.common.util.Resource
import com.example.firstapp.common.util.ContextProvider
import com.example.firstapp.common.data.realtime.PostWebSocketClient
import com.example.firstapp.common.data.realtime.PostSocketListener
import com.example.firstapp.media.ui.screens.copyUriToTempFile
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.account.domain.repository.AccountRepository

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val repository: SocialRepository,
    private val accountRepository: AccountRepository,
    private val chatRepository: com.example.firstapp.domain.repository.ChatRepository,
    private val postWebSocketClient: com.example.firstapp.common.data.realtime.PostWebSocketClient
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val stories: StateFlow<Resource<List<UserStory>>> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { repository.getStories() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    val contacts: StateFlow<Resource<List<com.example.firstapp.chat.model.Contact>>> = chatRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    private val socketListener = object : PostSocketListener {
        override fun onOpen() {}
        override fun onPostsEvent(action: String, postId: String?) {}
        override fun onShortsEvent(action: String, shortId: String?) {}
        override fun onStoriesEvent(action: String, storyId: String?) {
            refreshStories()
        }
        override fun onError(error: String) {}
    }

    init {
        postWebSocketClient.connect(socketListener)
    }

    private val _postResult = MutableStateFlow<Resource<Boolean>?>(null)
    val postResult: StateFlow<Resource<Boolean>?> = _postResult.asStateFlow()

    fun refreshStories() {
        viewModelScope.launch {
            refreshTrigger.emit(Unit)
        }
    }

    fun fetchStories() {
        refreshStories()
    }

    fun postStory(content: String, contentUrl: String?, type: String = "text") {
        viewModelScope.launch {
            _postResult.value = Resource.Loading()
            
            if (contentUrl?.startsWith("content://") == true) {
                try {
                    val uri = android.net.Uri.parse(contentUrl)
                    val context = ContextProvider.context
                    val file = copyUriToTempFile(context, uri)
                    if (file != null) {
                        accountRepository.uploadFile(file, "stories").collect { res ->
                            if (res is Resource.Success && res.data != null) {
                                val finalUrl = res.data.url
                                val finalType = if (type == "text") "image" else type
                                file.delete()
                                createStoryEntry(content, finalUrl, finalType)
                            } else if (res is Resource.Error) {
                                _postResult.value = Resource.Error(res.message ?: "Upload failed")
                            }
                        }
                    } else {
                        _postResult.value = Resource.Error("Could not process file")
                    }
                } catch (e: Exception) {
                    _postResult.value = Resource.Error("File error: ${e.message}")
                }
            } else {
                createStoryEntry(content, contentUrl, type)
            }
        }
    }

    private fun createStoryEntry(content: String, contentUrl: String?, type: String) {
        viewModelScope.launch {
            repository.createStory(content, contentUrl, type).collect {
                _postResult.value = it
                if (it is Resource.Success) {
                    refreshStories()
                }
            }
        }
    }

    fun clearPostResult() {
        _postResult.value = null
    }

    fun markStoryAsViewed(userId: String, storyId: String) {
        viewModelScope.launch {
            repository.markStoryAsViewed(userId, storyId).collect {
                // flow emits but we rely on repository internal update
            }
        }
    }

    fun muteStory(userId: String, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteStory(userId, isMuted).collect {
                if (it is Resource.Success) {
                    refreshStories()
                }
            }
        }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch {
            repository.deleteStory(storyId).collect {
                if (it is Resource.Success) {
                    refreshStories()
                }
            }
        }
    }

    fun sendStoryReply(userId: String, storyId: String, text: String) {
        viewModelScope.launch {
            // Step 1: Ensure a DM session exists with this user
            chatRepository.initiateDM(userId).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    val chatId = res.data
                    // Step 2: Send the reply as a message in that chat
                    chatRepository.sendMessage(
                        chatId = chatId,
                        body = "Replied to story: $text",
                        messageType = "text"
                    ).collect { /* result handled by chat system */ }
                }
            }
        }
    }

    override fun onCleared() {
        postWebSocketClient.disconnect(socketListener)
        super.onCleared()
    }
}
