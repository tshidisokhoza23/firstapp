package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.media.model.Channel
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.media.domain.repository.ChannelRepository

@HiltViewModel
class ChannelDetailViewModel @Inject constructor(
    private val repository: ChannelRepository
) : ViewModel() {

    private val _channel = MutableStateFlow<Resource<Channel>>(Resource.Loading())
    val channel: StateFlow<Resource<Channel>> = _channel.asStateFlow()

    fun loadChannel(channelId: Int) {
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
    }

    fun subscribe(channelId: Int) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect {
                if (it is Resource.Success) loadChannel(channelId)
            }
        }
    }

    fun mute(channelId: Int, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteChannel(channelId, isMuted).collect {
                if (it is Resource.Success) loadChannel(channelId)
            }
        }
    }
}
