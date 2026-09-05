package com.example.firstapp.chat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.ChatRepository

@HiltViewModel
class ContactPickerViewModel @Inject constructor(
    private val repository: ChatRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _contacts = MutableStateFlow<Resource<List<Contact>>>(Resource.Loading())
    val contacts: StateFlow<Resource<List<Contact>>> = _contacts.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Contact>>(emptyList())
    val searchResults: StateFlow<List<Contact>> = _searchResults.asStateFlow()

    init {
        // Continuously observe local Room contacts immediately so the UI displays cached contacts with zero delay.
        viewModelScope.launch {
            repository.getContacts().collect { resource ->
                _contacts.value = resource
            }
        }
        fetchContacts()
    }

    private var syncJob: Job? = null

    fun fetchContacts() {
        // Only set Loading if we don't already have contacts cached locally
        if ((_contacts.value.data ?: emptyList()).isEmpty()) {
            _contacts.value = Resource.Loading()
        }
        
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                // Background sync 1: Chats
                launch { repository.syncChats().collect { } }

                // Background sync 2: Phone contacts
                val phoneNumbers = com.example.firstapp.common.util.ContactUtils.getPhoneContacts(context)
                repository.syncContacts(phoneNumbers).collect { }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    if ((_contacts.value.data ?: emptyList()).isEmpty()) {
                        _contacts.value = Resource.Error(e.message ?: "Failed to sync contacts")
                    }
                }
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
