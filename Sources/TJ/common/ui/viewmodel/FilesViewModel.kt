package com.tpsk.tj.common.ui.viewmodel

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
class FilesViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _files = MutableStateFlow<Resource<List<Message>>>(Resource.Loading())
    val files: StateFlow<Resource<List<Message>>> = _files.asStateFlow()

    init {
        fetchFiles()
    }

    fun fetchFiles() {
        viewModelScope.launch {
            repository.getAllFiles().collect {
                _files.value = it
            }
        }
    }
}
