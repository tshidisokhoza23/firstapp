package com.tpsk.tj.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.chat.model.Message
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.domain.repository.ChatRepository

@HiltViewModel
class StarredMessagesViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _starredMessages = MutableStateFlow<Resource<List<Message>>>(Resource.Loading())
    val starredMessages: StateFlow<Resource<List<Message>>> = _starredMessages.asStateFlow()

    init {
        fetchStarredMessages()
    }

    fun fetchStarredMessages() {
        viewModelScope.launch {
            repository.getStarredMessages().collect {
                _starredMessages.value = it
            }
        }
    }

    fun unstarMessage(messageId: String) {
        viewModelScope.launch {
            repository.starMessage(messageId, false).collect { result ->
                if (result is Resource.Success) fetchStarredMessages()
            }
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId).collect { result ->
                if (result is Resource.Success) fetchStarredMessages()
            }
        }
    }
}
