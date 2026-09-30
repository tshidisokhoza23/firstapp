package com.tpsk.tj.groups.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.chat.model.ChatMessage
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.groups.domain.GroupRepository

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val repository: GroupRepository
) : ViewModel() {

    private val _createResult = MutableStateFlow<Resource<ChatMessage>?>(null)
    val createResult: StateFlow<Resource<ChatMessage>?> = _createResult.asStateFlow()

    fun createGroup(name: String, contactIds: List<String>) {
        val sanitizedName = name.trim()
        if (sanitizedName.isEmpty()) {
            _createResult.value = Resource.Error("Group name is required")
            return
        }
        
        if (contactIds.isEmpty()) {
            _createResult.value = Resource.Error("Please select at least one participant")
            return
        }

        viewModelScope.launch {
            _createResult.value = Resource.Loading()
            repository.createGroup(sanitizedName, contactIds).collect {
                _createResult.value = it
            }
        }
    }

    fun resetState() {
        _createResult.value = null
    }
}
