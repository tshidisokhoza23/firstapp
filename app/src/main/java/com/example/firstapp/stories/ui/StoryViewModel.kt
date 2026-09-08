package com.example.firstapp.stories.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.model.UserStory
import com.example.firstapp.common.util.ContextProvider
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.stories.domain.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val repository: StoryRepository,
    private val accountRepository: AccountRepository,
    chatRepository: ChatRepository
) : ViewModel() {

    val stories: StateFlow<Resource<List<UserStory>>> = repository.observeStories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading())

    val contacts: StateFlow<Resource<List<Contact>>> = chatRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Resource.Loading())

    private val _postResult = MutableStateFlow<Resource<Boolean>?>(null)
    val postResult: StateFlow<Resource<Boolean>?> = _postResult.asStateFlow()

    fun refreshStories() {
        viewModelScope.launch { repository.syncStories() }
    }

    fun fetchStories() = refreshStories()

    fun postStory(
        content: String,
        contentUrl: String?,
        type: String = "text",
        privacy: String = "contacts"
    ) {
        viewModelScope.launch {
            _postResult.value = Resource.Loading()
            try {
                val urlToUpload = contentUrl?.takeIf { it.isNotBlank() }
                val needsUpload = urlToUpload != null &&
                    !urlToUpload.startsWith("http://") &&
                    !urlToUpload.startsWith("https://")

                var finalContentUrl = contentUrl
                var finalType = type
                if (needsUpload) {
                    val uri = android.net.Uri.parse(urlToUpload)
                    var uploadedUrl: String? = null
                    var uploadError: String? = null
                    withTimeout(30_000) {
                        accountRepository.uploadUri(ContextProvider.context, uri, "stories").collect { res ->
                            when (res) {
                                is Resource.Success -> uploadedUrl = res.data?.url
                                is Resource.Error -> uploadError = res.message ?: "Upload failed"
                                else -> {}
                            }
                        }
                    }
                    if (!uploadError.isNullOrBlank() || uploadedUrl.isNullOrBlank()) {
                        _postResult.value = Resource.Error(uploadError ?: "Upload failed")
                        return@launch
                    }
                    finalContentUrl = uploadedUrl
                    if (type == "text" || type.isBlank()) finalType = "image"
                }

                val result = withTimeout(30_000) {
                    repository.createStory(content, finalContentUrl, finalType, privacy)
                        .first { it !is Resource.Loading }
                }
                _postResult.value = result
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                _postResult.value = Resource.Error(e.message ?: "Failed to post story")
            }
        }
    }

    fun clearPostResult() {
        _postResult.value = null
    }

    fun markStoryAsViewed(userId: String, storyId: String) {
        viewModelScope.launch { repository.markStoryAsViewed(userId, storyId).first() }
    }

    fun muteStory(userId: String, isMuted: Boolean) {
        viewModelScope.launch { repository.muteStory(userId, isMuted).first() }
    }

    fun deleteStory(storyId: String) {
        viewModelScope.launch { repository.deleteStory(storyId).first() }
    }
}
