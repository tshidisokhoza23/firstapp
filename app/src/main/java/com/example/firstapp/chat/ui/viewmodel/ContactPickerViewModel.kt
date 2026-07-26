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
        fetchContacts()
    }

    private var fetchJob: Job? = null

    fun fetchContacts() {
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _contacts.value = Resource.Loading()
            try {
                // Wait for sync to complete before starting to collect from DB
                syncContacts()
                
                repository.getContacts().collect { resource ->
                    _contacts.value = resource
                }
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    _contacts.value = Resource.Error(e.message ?: "Unknown error")
                }
            }
        }
    }

    private suspend fun syncContacts() {
        val phoneNumbers = com.example.firstapp.common.util.ContactUtils.getPhoneContacts(context)
        // Collect all emissions from syncContacts to ensure it finishes
        repository.syncContacts(phoneNumbers).collect { }
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
