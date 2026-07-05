package com.example.firstapp.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.model.CallLog
import com.example.firstapp.chat.model.ChatMessage
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.ChatRepository

@HiltViewModel
class DirectMessagesViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _chats = MutableStateFlow<Resource<List<ChatMessage>>>(Resource.Loading())
    val chats: StateFlow<Resource<List<ChatMessage>>> = _chats.asStateFlow()

    private val _calls = MutableStateFlow<Resource<List<CallLog>>>(Resource.Loading())
    val calls: StateFlow<Resource<List<CallLog>>> = _calls.asStateFlow()

    init {
        fetchData()
    }

    fun fetchData() {
        viewModelScope.launch {
            repository.getChats().collect { _chats.value = it }
        }
        viewModelScope.launch {
            repository.getCallLogs().collect { _calls.value = it }
        }
    }

    fun pinChat(chatId: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.pinChat(chatId, isPinned).collect {
                if (it is Resource.Success) fetchData()
            }
        }
    }

    fun muteChat(chatId: String, isMuted: Boolean) {
        viewModelScope.launch {
            repository.muteChat(chatId, isMuted).collect {
                if (it is Resource.Success) fetchData()
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            repository.deleteChat(chatId).collect {
                if (it is Resource.Success) fetchData()
            }
        }
    }
}
