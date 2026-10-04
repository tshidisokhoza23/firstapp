package com.tpsk.tj.channels.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.chat.model.Channel
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.channels.domain.ChannelRepository
import com.tpsk.tj.stories.domain.StoryRepository
import com.tpsk.tj.common.domain.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelDetailViewModel @Inject constructor(
    private val repository: ChannelRepository,
    private val chatRepository: com.tpsk.tj.domain.repository.ChatRepository,
    private val sessionManager: SessionManager,
    private val accountRepository: com.tpsk.tj.account.domain.repository.AccountRepository,
    private val storyRepository: StoryRepository
) : ViewModel() {

    val currentUserId: String? get() = sessionManager.getUserId()

    private val _channel = MutableStateFlow<Resource<Channel>>(Resource.Loading())
    val channel: StateFlow<Resource<Channel>> = _channel.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _contacts = MutableStateFlow<List<com.tpsk.tj.chat.model.Contact>>(emptyList())
    val contacts: StateFlow<List<com.tpsk.tj.chat.model.Contact>> = _contacts.asStateFlow()

    private val _members = MutableStateFlow<List<com.tpsk.tj.common.data.api.ApiUser>>(emptyList())
    val members: StateFlow<List<com.tpsk.tj.common.data.api.ApiUser>> = _members.asStateFlow()

    fun loadChannel(channelId: String) {
        viewModelScope.launch {
            repository.getChannel(channelId).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _channel.value = res
                    val convId = res.data.conversationId ?: res.data.id
                    loadMessages(convId)
                    loadMembers(convId)
                } else if (res is Resource.Error) {
                    _channel.value = Resource.Error(res.message ?: "Channel not found")
                }
            }
        }
        viewModelScope.launch {
            repository.getChannels().collect { resource ->
                if (resource is Resource.Success) {
                    val ch = resource.data?.find { it.id == channelId || it.conversationId == channelId }
                    if (ch != null) {
                        _channel.value = Resource.Success(ch)
                        val convId = ch.conversationId ?: ch.id
                        loadMessages(convId)
                        loadMembers(convId)
                    } else if (_channel.value !is Resource.Success) {
                        val searched = repository.searchChannels(channelId).firstOrNull()
                            ?.find { it.id == channelId || it.conversationId == channelId }
                        if (searched != null) {
                            _channel.value = Resource.Success(searched)
                            val convId = searched.conversationId ?: searched.id
                            loadMessages(convId)
                            loadMembers(convId)
                        } else {
                            _channel.value = Resource.Error("Channel not found")
                        }
                    }
                } else if (resource is Resource.Error && _channel.value !is Resource.Success) {
                    _channel.value = Resource.Error(resource.message ?: "Failed to load")
                }
            }
        }
        loadMessages(channelId)
        viewModelScope.launch {
            chatRepository.getContacts().collect { res ->
                _contacts.value = res.data ?: emptyList()
            }
        }
    }

    private fun loadMembers(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getConversationMembers(conversationId).collect { res ->
                _members.value = res.data ?: emptyList()
            }
        }
    }

    fun loadMessages(conversationId: String) {
        viewModelScope.launch {
            chatRepository.getMessages(conversationId).collect { res ->
                _messages.value = res.data ?: emptyList()
            }
        }
        viewModelScope.launch {
            try {
                chatRepository.fetchMessages(conversationId).collect {}
            } catch (_: Exception) {}
        }
    }

    fun subscribe(channelId: String) {
        viewModelScope.launch {
            repository.subscribeToChannel(channelId).collect { res ->
                if (res is Resource.Success) {
                    val followed = res.data != false
                    _toast.value = if (followed) "Channel followed" else "Unfollowed channel"
                    loadChannel(channelId)
                    val convId = (_channel.value as? Resource.Success)?.data?.conversationId ?: channelId
                    try {
                        chatRepository.fetchMessages(convId).collect {}
                        chatRepository.syncChats().collect {}
                    } catch (_: Exception) {}
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Failed to follow channel"
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
            repository.addAdmin(channelId, userId).collect {
                if (it is Resource.Success) loadChannel(channelId)
            }
        }
    }

    fun removeAdmin(channelId: String, userId: String) {
        viewModelScope.launch {
            repository.removeAdmin(channelId, userId).collect {
                if (it is Resource.Success) loadChannel(channelId)
            }
        }
    }

    fun toggleAdminPosting(channelId: String, allowed: Boolean) {
        viewModelScope.launch {
            repository.updateAdminPostingPermission(channelId, allowed).collect {
                if (it is Resource.Success) loadChannel(channelId)
            }
        }
    }

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun clearToast() {
        _toast.value = null
    }

    fun sendMessage(
        channelId: String,
        content: String,
        mediaUrls: List<String> = emptyList(),
        messageType: String = "text",
        fileName: String? = null
    ) {
        viewModelScope.launch {
            val ch = (_channel.value as? Resource.Success)?.data
            val convId = ch?.conversationId ?: channelId
            val type = when {
                messageType.isNotBlank() && messageType != "text" -> messageType
                mediaUrls.isNotEmpty() -> "image"
                else -> "text"
            }
            
            chatRepository.sendMessage(
                chatId = convId,
                body = content,
                messageType = type,
                mediaUrls = mediaUrls,
                fileName = fileName
            ).collect { res ->
                if (res is Resource.Success) {
                    _toast.value = "Update sent"
                    loadMessages(convId)
                } else if (res is Resource.Error) {
                    _toast.value = res.message ?: "Only the owner and allowed admins can post"
                }
            }
        }
    }

    fun sendMediaPost(
        context: android.content.Context,
        channelId: String,
        content: String,
        mediaUrisOrUrls: List<String>,
        messageType: String = "text",
        fileName: String? = null
    ) {
        viewModelScope.launch {
            val uploadedUrls = mutableListOf<String>()
            var uploadFailed = false
            for (item in mediaUrisOrUrls) {
                if (item.startsWith("content://") || item.startsWith("file://")) {
                    val uri = android.net.Uri.parse(item)
                    accountRepository.uploadUri(context, uri, "channels").collect { res ->
                        if (res is Resource.Success && res.data?.url != null) {
                            uploadedUrls.add(res.data.url)
                        } else if (res is Resource.Error) {
                            _toast.value = res.message ?: "Failed to upload media"
                            uploadFailed = true
                        }
                    }
                } else {
                    uploadedUrls.add(item)
                }
            }

            if (uploadFailed) return@launch

            sendMessage(
                channelId = channelId,
                content = content,
                mediaUrls = if (uploadedUrls.isNotEmpty()) uploadedUrls else mediaUrisOrUrls,
                messageType = messageType,
                fileName = fileName
            )
        }
    }

    fun createPost(channelId: String, content: String, mediaUrls: List<String>) {
        sendMessage(channelId, content, mediaUrls, if (mediaUrls.isNotEmpty()) "image" else "text")
    }

    fun shareToStatus(name: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            storyRepository.createStory(
                content = "Check out $name channel on FirstApp!",
                type = "text",
                privacy = "contacts"
            ).collect { res ->
                if (res is Resource.Success) onDone(true)
                if (res is Resource.Error) onDone(false)
            }
        }
    }

    fun saveDescription(channelId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.updateDescription(channelId, trimmed).collect { res ->
                when (res) {
                    is Resource.Success -> _toast.value = "Description saved"
                    is Resource.Error -> {
                        sendMessage(channelId, trimmed, emptyList(), "text")
                        _toast.value = "Description posted to the channel. The server did not save it on the channel yet."
                    }
                    is Resource.Loading -> Unit
                }
            }
        }
    }
}
