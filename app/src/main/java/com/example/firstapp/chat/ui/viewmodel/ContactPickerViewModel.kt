package com.example.firstapp.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.ChatRepository

@HiltViewModel
class ContactPickerViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _contacts = MutableStateFlow<Resource<List<Contact>>>(Resource.Loading())
    val contacts: StateFlow<Resource<List<Contact>>> = _contacts.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults: StateFlow<List<Contact>> = _searchResults.asStateFlow()

    init {
        fetchContacts()
    }

    fun fetchContacts() {
        viewModelScope.launch {
            repository.getContacts().collect {
                _contacts.value = it
            }
        }
    }

    fun searchContacts(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            repository.searchLocalContacts(query).collect {
                _searchResults.value = it
            }
        }
    }
}
