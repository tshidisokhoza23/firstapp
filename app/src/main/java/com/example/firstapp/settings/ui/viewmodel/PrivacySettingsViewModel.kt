package com.example.firstapp.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.model.PrivacySettings
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.account.domain.repository.AccountRepository

@HiltViewModel
class PrivacySettingsViewModel @Inject constructor(
    private val repository: AccountRepository,
    private val chatRepository: com.example.firstapp.domain.repository.ChatRepository,
    private val settingsRepository: com.example.firstapp.domain.repository.SettingsRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<Resource<PrivacySettings>>(Resource.Loading())
    val settings: StateFlow<Resource<PrivacySettings>> = _settings.asStateFlow()

    private val _blockedUsers = MutableStateFlow<Resource<List<Contact>>>(Resource.Loading())
    val blockedUsers: StateFlow<Resource<List<Contact>>> = _blockedUsers.asStateFlow()

    val pinLockEnabled = settingsRepository.pinLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val biometricLockEnabled = settingsRepository.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    val appPin = settingsRepository.appPin
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val contacts: StateFlow<Resource<List<Contact>>> = chatRepository.getContacts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    init {
        fetchSettings()
        fetchBlockedUsers()
    }

    fun fetchSettings() {
        viewModelScope.launch {
            repository.getPrivacySettings().collect {
                _settings.value = it
            }
        }
    }

    fun fetchBlockedUsers() {
        viewModelScope.launch {
            repository.getBlockedUsers().collect {
                _blockedUsers.value = it
            }
        }
    }

    fun updateSettings(settings: PrivacySettings) {
        viewModelScope.launch {
            repository.updatePrivacySettings(settings).collect {
                if (it is Resource.Success) {
                    fetchSettings()
                }
            }
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            repository.unblockUser(userId).collect {
                if (it is Resource.Success) {
                    fetchBlockedUsers()
                }
            }
        }
    }

    fun setPinLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setPinLockEnabled(enabled) }
    }

    fun setBiometricLockEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setBiometricLockEnabled(enabled) }
    }

    fun setAppPin(pin: String?) {
        viewModelScope.launch { settingsRepository.setAppPin(pin) }
    }
}
