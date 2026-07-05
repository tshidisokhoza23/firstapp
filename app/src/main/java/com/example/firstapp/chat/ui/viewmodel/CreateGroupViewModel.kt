package com.example.firstapp.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class CreateGroupViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _createResult = MutableStateFlow<Resource<ChatMessage>?>(null)
    val createResult: StateFlow<Resource<ChatMessage>?> = _createResult.asStateFlow()

    fun createGroup(name: String, contactIds: List<String>) {
        viewModelScope.launch {
            repository.createGroup(name, contactIds).collect {
                _createResult.value = it
            }
        }
    }

    fun resetState() {
        _createResult.value = null
    }
}
