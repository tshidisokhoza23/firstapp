package com.tpsk.tj.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    val privateChatsEnabled: StateFlow<Boolean> = repository.privateChatsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val groupsEnabled: StateFlow<Boolean> = repository.groupsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val channelsEnabled: StateFlow<Boolean> = repository.channelsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val soundsEnabled: StateFlow<Boolean> = repository.soundsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val vibrateEnabled: StateFlow<Boolean> = repository.vibrateEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val previewsEnabled: StateFlow<Boolean> = repository.previewsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setPrivateChatsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPrivateChatsEnabled(enabled) }
    }
    fun setGroupsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGroupsEnabled(enabled) }
    }
    fun setChannelsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setChannelsEnabled(enabled) }
    }
    fun setSoundsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundsEnabled(enabled) }
    }
    fun setVibrateEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setVibrateEnabled(enabled) }
    }
    fun setPreviewsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setPreviewsEnabled(enabled) }
    }
}
