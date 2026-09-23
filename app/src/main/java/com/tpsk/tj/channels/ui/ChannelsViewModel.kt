package com.tpsk.tj.channels.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.chat.model.Channel
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.channels.domain.ChannelRepository

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val chatRepository: com.tpsk.tj.domain.repository.ChatRepository,
    private val accountRepository: com.tpsk.tj.account.domain.repository.AccountRepository
) : ViewModel() {

    private val _channels = MutableStateFlow<Resource<List<Channel>>>(Resource.Loading())
    val channels: StateFlow<Resource<List<Channel>>> = _channels.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        fetchChannels()
    }

    private var fetchJob: kotlinx.coroutines.Job? = null

    fun fetchChannels() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            repository.getChannels().collect { _channels.value = it }
        }
        viewModelScope.launch {
            runCatching {
                chatRepository.syncChats().collect {}
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun subscribeToChannel(channelId: String) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect {
                if (it is Resource.Success) {
                    fetchChannels()
                    chatRepository.syncChats().collect {}
                }
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

    fun createChannel(name: String, description: String, avatarUri: android.net.Uri? = null) {
        viewModelScope.launch {
            var avatarUrl: String? = null
            if (avatarUri != null) {
                val context = com.tpsk.tj.common.util.ContextProvider.context
                accountRepository.uploadUri(context, avatarUri, "avatars").collect { res ->
                    if (res is Resource.Success) {
                        avatarUrl = res.data?.url
                    } else if (res is Resource.Error) {
                        _toastMessage.value = res.message ?: "Failed to upload icon"
                    }
                }
                if (avatarUrl == null && _toastMessage.value != null) return@launch
            }
            repository.createChannel(name, description, avatarUrl).collect {
                if (it is Resource.Success) {
                    _toastMessage.value = "Channel created successfully"
                    fetchChannels()
                    chatRepository.syncChats().collect {}
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

    fun toggleAdminPosting(channelId: String, allowed: Boolean) {
        viewModelScope.launch {
            repository.updateAdminPostingPermission(channelId, allowed).collect {
                if (it is Resource.Success) {
                    _toastMessage.value = "Posting permission updated"
                    fetchChannels()
                } else if (it is Resource.Error) {
                    _toastMessage.value = it.message
                }
            }
        }
    }
}
