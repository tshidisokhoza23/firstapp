package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.model.Channel
import com.example.firstapp.media.model.Post
import com.example.firstapp.media.domain.repository.ChannelRepository
import com.example.firstapp.media.domain.repository.SocialRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelDetailViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _channel = MutableStateFlow<Resource<Channel>>(Resource.Loading())
    val channel: StateFlow<Resource<Channel>> = _channel.asStateFlow()

    private val _posts = MutableStateFlow<Resource<List<Post>>>(Resource.Loading())
    val posts: StateFlow<Resource<List<Post>>> = _posts.asStateFlow()

    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            repository.getChannels().collect { resource ->
                if (resource is Resource.Success) {
                    val ch = resource.data?.find { it.id == channelId }
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
            socialRepository.getChannelPosts(channelId).collect { _posts.value = it }
        }
    }

    fun subscribe(channelId: String) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect {
                if (it is Resource.Success) loadChannel(channelId)
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

    fun createPost(channelId: String, content: String, mediaUrls: List<String>) {
        viewModelScope.launch {
            socialRepository.createPost(content, mediaUrls, channelId = channelId).collect {
                if (it is Resource.Success) loadPosts(channelId)
            }
        }
    }
}
