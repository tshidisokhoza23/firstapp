package com.example.firstapp.settings.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.firstapp.domain.repository.ChatRepository

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {
    private val _fontSize = MutableStateFlow(16f)
    val fontSize: StateFlow<Float> = _fontSize

    private val _enterIsSend = MutableStateFlow(false)
    val enterIsSend: StateFlow<Boolean> = _enterIsSend

    private val _mediaVisibility = MutableStateFlow(true)
    val mediaVisibility: StateFlow<Boolean> = _mediaVisibility

    private val _animationsEnabled = MutableStateFlow(true)
    val animationsEnabled: StateFlow<Boolean> = _animationsEnabled

    private val _autoNightMode = MutableStateFlow(false)
    val autoNightMode: StateFlow<Boolean> = _autoNightMode

    fun setFontSize(size: Float) { _fontSize.value = size }
    fun setEnterIsSend(enabled: Boolean) { _enterIsSend.value = enabled }
    fun setMediaVisibility(enabled: Boolean) { _mediaVisibility.value = enabled }
    fun setAnimationsEnabled(enabled: Boolean) { _animationsEnabled.value = enabled }
    fun setAutoNightMode(enabled: Boolean) { _autoNightMode.value = enabled }

    fun clearAllChats() {
        viewModelScope.launch {
            repository.clearAllChats()
        }
    }
}
