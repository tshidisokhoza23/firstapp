package com.example.firstapp.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.vocab.domain.repository.UserRepository

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    fun changePhoneNumber(newNumber: String) {
        // Mocking for now, could be part of updateProfile
        viewModelScope.launch {
            _toastMessage.value = "Phone number update request sent for $newNumber"
        }
    }

    fun requestPasswordReset(email: String) {
        viewModelScope.launch {
            try {
                userRepository.forgotPassword(email).collect { resource ->
                    if (resource is Resource.Success) {
                        _toastMessage.value = "Password reset instructions sent to $email"
                    } else if (resource is Resource.Error) {
                        _toastMessage.value = resource.message ?: "Failed to request password reset"
                    }
                }
            } catch (e: Exception) {
                _toastMessage.value = "Failed to request password reset"
            }
        }
    }

    fun toggleSecurityNotifications(enabled: Boolean) {
        _toastMessage.value = if (enabled) "Security alerts enabled" else "Security alerts disabled"
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
