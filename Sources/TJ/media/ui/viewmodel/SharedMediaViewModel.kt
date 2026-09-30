package com.tpsk.tj.media.ui.viewmodel

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
class SharedMediaViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _mediaMessages = MutableStateFlow<Resource<List<Message>>>(Resource.Loading())
    val mediaMessages: StateFlow<Resource<List<Message>>> = _mediaMessages.asStateFlow()

    fun loadSharedMedia(chatId: String) {
        viewModelScope.launch {
            repository.getSharedMedia(chatId).collect {
                _mediaMessages.value = it
            }
        }
    }
}
