package com.example.firstapp.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.domain.repository.SystemRepository

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val repository: SystemRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        fetchCurrentUser()
    }

    fun resetAndSyncContacts(context: android.content.Context) {
        viewModelScope.launch {
            _toastMessage.value = "Cleaning server and local database..."
            try {
                val contacts = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    com.example.firstapp.common.util.ContactUtils.getPhoneContactsWithNames(context)
                }
                repository.devResetAndSeed(contacts).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val data = result.data
                            _toastMessage.value = "Successfully reset. Use username: ${data?.username}, password: ${data?.password}"
                            // OLD TOKENS ARE INVALID. Clear local auth state.
                            TokenManager.clear()
                        }
                        is Resource.Error -> {
                            _toastMessage.value = "Server reset failed: ${result.message}"
                        }
                        is Resource.Loading -> {}
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "Sync failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun fetchCurrentUser() {
        viewModelScope.launch {
            userRepository.getCurrentUser().collect { resource ->
                if (resource is Resource.Success) _currentUser.value = resource.data
            }
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            userRepository.deleteAccount().collect { resource ->
                if (resource is Resource.Success) {
                    TokenManager.clear()
                    onSuccess()
                }
            }
        }
    }
}
