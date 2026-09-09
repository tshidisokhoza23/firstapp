package com.example.firstapp.stories.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.chat.model.UserStory
import com.example.firstapp.common.util.Resource
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.stories.domain.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

@HiltViewModel
class StoryViewModel @Inject constructor(
    private val repository: StoryRepository,
    private val accountRepository: AccountRepository,
    chatRepository: ChatRepository,
    @param:ApplicationContext private val context: Context
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
                val trimmedUrl = contentUrl?.trim()?.takeIf { it.isNotBlank() }
                var finalContentUrl: String? = trimmedUrl
                var finalType = type.trim().lowercase().ifBlank { "text" }

                val isLocalUri = trimmedUrl != null &&
                    !trimmedUrl.startsWith("http://", ignoreCase = true) &&
                    !trimmedUrl.startsWith("https://", ignoreCase = true)

                if (isLocalUri) {
                    val uri = Uri.parse(trimmedUrl)

                    if (finalType == "text" || finalType.isBlank()) {
                        finalType = detectMediaTypeFromUri(uri) ?: "image"
                    }

                    var uploadedUrl: String? = null
                    var uploadError: String? = null

                    withTimeout(45_000) {
                        accountRepository.uploadUri(context, uri, "stories").collect { uploadRes ->
                            when (uploadRes) {
                                is Resource.Success -> {
                                    uploadedUrl = uploadRes.data?.url
                                }
                                is Resource.Error -> {
                                    uploadError = uploadRes.message ?: "Failed to upload media"
                                }
                                else -> {}
                            }
                        }
                    }

                    if (!uploadError.isNullOrBlank() || uploadedUrl.isNullOrBlank()) {
                        _postResult.value = Resource.Error(uploadError ?: "Failed to upload story media")
                        return@launch
                    }

                    finalContentUrl = uploadedUrl
                }

                if (!finalContentUrl.isNullOrBlank() && finalType == "text") {
                    finalType = "image"
                }

                val result = withTimeout(30_000) {
                    repository.createStory(
                        content = content.trim(),
                        contentUrl = finalContentUrl,
                        type = finalType,
                        privacy = privacy.ifBlank { "contacts" }
                    ).firstOrNull { it !is Resource.Loading }
                }

                _postResult.value = result ?: Resource.Error("Failed to post story: timed out")
            } catch (e: Exception) {
                if (e is CancellationException && e !is TimeoutCancellationException) throw e
                val errorMsg = when (e) {
                    is TimeoutCancellationException -> "Story posting timed out. Please try again."
                    else -> e.localizedMessage ?: "Failed to post story"
                }
                _postResult.value = Resource.Error(errorMsg)
            }
        }
    }

    private fun detectMediaTypeFromUri(uri: Uri): String? {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            when {
                mimeType?.startsWith("video/") == true -> "video"
                mimeType?.startsWith("image/") == true -> "image"
                else -> {
                    val uriString = uri.toString().lowercase()
                    when {
                        uriString.contains("video") || uriString.endsWith(".mp4") || uriString.endsWith(".mkv") || uriString.endsWith(".mov") -> "video"
                        uriString.contains("image") || uriString.endsWith(".jpg") || uriString.endsWith(".jpeg") || uriString.endsWith(".png") -> "image"
                        else -> null
                    }
                }
            }
        } catch (_: Exception) {
            null
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
