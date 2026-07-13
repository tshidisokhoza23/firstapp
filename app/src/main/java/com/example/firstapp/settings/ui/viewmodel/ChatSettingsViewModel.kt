package com.example.firstapp.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.ChatRepository
import com.example.firstapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val fontSize: StateFlow<Float> = settingsRepository.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 16f)

    val enterIsSend: StateFlow<Boolean> = settingsRepository.enterIsSend
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val mediaVisibility: StateFlow<Boolean> = settingsRepository.mediaVisibility
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val animationsEnabled: StateFlow<Boolean> = settingsRepository.animationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoNightMode: StateFlow<Boolean> = settingsRepository.autoNightMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setFontSize(size: Float) {
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    fun setEnterIsSend(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setEnterIsSend(enabled) }
    }

    fun setMediaVisibility(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMediaVisibility(enabled) }
    }

    fun setAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAnimationsEnabled(enabled) }
    }

    fun setAutoNightMode(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoNightMode(enabled) }
    }

    fun clearAllChats() {
        viewModelScope.launch {
            chatRepository.clearAllChats()
        }
    }
}
