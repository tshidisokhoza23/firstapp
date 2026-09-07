package com.example.firstapp.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Channel
import com.example.firstapp.media.model.Post
import com.example.firstapp.chat.domain.repository.ChannelRepository
import com.example.firstapp.media.domain.repository.SocialRepository
import com.example.firstapp.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelDetailViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val socialRepository: SocialRepository,
    private val chatRepository: com.example.firstapp.domain.repository.ChatRepository
) : ViewModel() {

    private val authRepository = AuthRepository.getInstance()
    val currentUserId = authRepository.currentUser.value?.uid

    private val _channel = MutableStateFlow<Resource<Channel>>(Resource.Loading())
    val channel: StateFlow<Resource<Channel>> = _channel.asStateFlow()

    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val posts: StateFlow<Resource<List<Post>>> = _posts.asStateFlow()

    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            repository.getChannels().collect { resource ->
                if (resource is Resource.Success) {
                    val ch = resource.data?.find { it.id == channelId || it.conversationId == channelId }
                    if (ch != null) _channel.value = Resource.Success(ch)
                    else _channel.value = Resource.Error("Channel not found")
                } else if (resource is Resource.Error) {
                    _channel.value = Resource.Error(resource.message ?: "Failed to load")
                }
            }
        }
        loadPosts(channelId)
    }

    fun loadPosts(channelId: String) {
        viewModelScope.launch {
            socialRepository.getChannelPosts(channelId, 1, "standard").collect { _posts.value = it }
        }
    }

    fun subscribe(channelId: String) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect {
                if (it is Resource.Success) {
                    loadChannel(channelId)
                    chatRepository.syncChats().collect {}
                }
            }
        }
    }

    fun mute(channelId: String, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteChannel(channelId, isMuted).collect {
                if (it is Resource.Success) loadChannel(channelId)
            }
        }
    }

    fun addAdmin(channelId: String, userId: String) {
        viewModelScope.launch {
            repository.addAdmin(channelId, userId).collect { }
        }
    }

    fun toggleAdminPosting(channelId: String, allowed: Boolean) {
        viewModelScope.launch {
            repository.updateAdminPostingPermission(channelId, allowed).collect {
                if (it is Resource.Success) loadChannel(channelId)
            }
        }
    }

    fun createPost(channelId: String, content: String, mediaUrls: List<String>) {
        viewModelScope.launch {
            socialRepository.createPost(content, mediaUrls, channelId = channelId).collect {
                if (it is Resource.Success) loadPosts(channelId)
            }
        }
    }
}
