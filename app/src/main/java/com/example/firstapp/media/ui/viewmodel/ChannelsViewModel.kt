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
class ChannelsViewModel @Inject constructor(
    private val repository: ChannelRepository
) : ViewModel() {

    private val _channels = MutableStateFlow<Resource<List<Channel>>>(Resource.Loading())
    val channels: StateFlow<Resource<List<Channel>>> = _channels.asStateFlow()

    init {
        fetchChannels()
    }

    fun fetchChannels() {
        viewModelScope.launch {
            repository.getChannels().collect { _channels.value = it }
        }
    }

    fun subscribeToChannel(channelId: Int) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect {
                if (it is Resource.Success) fetchChannels()
            }
        }
    }

    fun muteChannel(channelId: Int, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteChannel(channelId, isMuted).collect {
                if (it is Resource.Success) fetchChannels()
            }
        }
    }

    fun createChannel(name: String, description: String) {
        viewModelScope.launch {
            repository.createChannel(name, description).collect {
                if (it is Resource.Success) fetchChannels()
            }
        }
    }
}
