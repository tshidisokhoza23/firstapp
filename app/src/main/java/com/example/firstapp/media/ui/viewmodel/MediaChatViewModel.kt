package com.example.firstapp.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.account.domain.repository.AccountRepository
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Message
import com.example.firstapp.chat.model.MessageType
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.media.model.MediaMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.collections.emptyList
import kotlin.collections.listOf
import kotlin.collections.firstOrNull
import javax.inject.Inject

@HiltViewModel
class MediaChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val userRepository: UserRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<MediaMessage>>(emptyList<MediaMessage>())
    val messages: StateFlow<List<MediaMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _receiverUser = MutableStateFlow<com.example.firstapp.common.model.User?>(null)
    val receiverUser: StateFlow<com.example.firstapp.common.model.User?> = _receiverUser.asStateFlow()

    private var activeChatId: String? = null

    fun loadMessages(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            // Load user info
            userRepository.getUserData(userId).collect { user ->
                _receiverUser.value = user
            }

            repository.initiateDM(userId).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    activeChatId = res.data
                    fetchChatMessages(res.data)
                } else if (res is Resource.Error) {
                    _error.value = res.message
                    _isLoading.value = false
                }
            }
        }
    }

    private fun fetchChatMessages(chatId: String) {
        viewModelScope.launch {
            repository.getMessages(chatId).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    _messages.value = res.data.map { it.toMediaMessage() }
                    _isLoading.value = false
                } else if (res is Resource.Error) {
                    _error.value = res.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun sendMessage(receiverId: String, content: String, type: String = "text", mediaUrls: List<String> = emptyList()) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            repository.sendMessage(chatId, content, type, mediaUrls).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    fetchChatMessages(chatId)
                }
            }
        }
    }

    fun sendMediaMessage(receiverId: String, file: File, type: String) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            accountRepository.uploadFile(file, "media_chat").collect { res ->
                if (res is Resource.Success && res.data != null) {
                    sendMessage(receiverId, "", type, listOf(res.data.url))
                    file.delete()
                } else if (res is Resource.Error) {
                    _error.value = res.message
                    _isLoading.value = false
                }
            }
        }
    }

    fun sendMediaMessage(receiverId: String, uri: android.net.Uri, context: android.content.Context, type: String) {
        val chatId = activeChatId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            accountRepository.uploadUri(context, uri, "media_chat").collect { res ->
                if (res is Resource.Success && res.data != null) {
                    sendMessage(receiverId, "", type, listOf(res.data.url))
                } else if (res is Resource.Error) {
                    _error.value = res.message
                    _isLoading.value = false
                }
            }
        }
    }

    private fun Message.toMediaMessage(): MediaMessage {
        return MediaMessage(
            id = id,
            senderId = senderId,
            receiverId = "", // Not strictly needed for UI bubble
            content = content,
            timestamp = timestamp,
            isRead = isRead,
            mediaUrl = mediaUrls.firstOrNull(),
            type = if (type == MessageType.IMAGE) "image" else if (type == MessageType.VIDEO) "video" else "text"
        )
    }
}
