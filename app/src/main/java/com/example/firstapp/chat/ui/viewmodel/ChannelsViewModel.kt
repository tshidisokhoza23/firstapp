package com.example.firstapp.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.model.Channel
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.chat.domain.repository.ChannelRepository

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val repository: ChannelRepository
) : ViewModel() {

    private val _channels = MutableStateFlow<Resource<List<Channel>>>(Resource.Loading())
    val channels: StateFlow<Resource<List<Channel>>> = _channels.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        fetchChannels()
    }

    fun fetchChannels() {
        viewModelScope.launch {
            repository.getChannels().collect { _channels.value = it }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun subscribeToChannel(channelId: String) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect {
                if (it is Resource.Success) fetchChannels()
                else if (it is Resource.Error) _toastMessage.value = it.message
            }
        }
    }

    fun muteChannel(channelId: String, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteChannel(channelId, isMuted).collect {
                if (it is Resource.Success) fetchChannels()
                else if (it is Resource.Error) _toastMessage.value = it.message
            }
        }
    }

    fun createChannel(name: String, description: String) {
        viewModelScope.launch {
            repository.createChannel(name, description).collect {
                if (it is Resource.Success) {
                    _toastMessage.value = "Channel created successfully"
                    fetchChannels()
                } else if (it is Resource.Error) {
                    _toastMessage.value = it.message
                }
            }
        }
    }

    fun addAdmin(channelId: String, userId: String) {
        viewModelScope.launch {
            repository.addAdmin(channelId, userId).collect {
                if (it is Resource.Success) {
                    _toastMessage.value = "Admin added"
                    fetchChannels()
                } else if (it is Resource.Error) {
                    _toastMessage.value = it.message
                }
            }
        }
    }

    fun removeAdmin(channelId: String, userId: String) {
        viewModelScope.launch {
            repository.removeAdmin(channelId, userId).collect {
                if (it is Resource.Success) {
                    _toastMessage.value = "Admin removed"
                    fetchChannels()
                } else if (it is Resource.Error) {
                    _toastMessage.value = it.message
                }
            }
        }
    }
}
