package com.tpsk.tj.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.tpsk.tj.vocab.domain.repository.UserRepository
import com.tpsk.tj.domain.repository.SettingsRepository

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    val securityNotificationsEnabled: StateFlow<Boolean> = settingsRepository.securityNotificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun changePhoneNumber(newNumber: String) {
        viewModelScope.launch {
            _toastMessage.value = "Phone number changes are not available yet. Your login number stays the same."
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
        viewModelScope.launch {
            settingsRepository.setSecurityNotificationsEnabled(enabled)
            _toastMessage.value = if (enabled) "Security alerts enabled" else "Security alerts disabled"
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
