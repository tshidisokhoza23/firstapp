package com.example.firstapp.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@HiltViewModel
class NotificationSettingsViewModel @Inject constructor() : ViewModel() {
    private val _privateChatsEnabled = MutableStateFlow(true)
    val privateChatsEnabled: StateFlow<Boolean> = _privateChatsEnabled

    private val _groupsEnabled = MutableStateFlow(true)
    val groupsEnabled: StateFlow<Boolean> = _groupsEnabled

    private val _channelsEnabled = MutableStateFlow(true)
    val channelsEnabled: StateFlow<Boolean> = _channelsEnabled

    private val _soundsEnabled = MutableStateFlow(true)
    val soundsEnabled: StateFlow<Boolean> = _soundsEnabled

    private val _vibrateEnabled = MutableStateFlow(true)
    val vibrateEnabled: StateFlow<Boolean> = _vibrateEnabled

    private val _previewsEnabled = MutableStateFlow(true)
    val previewsEnabled: StateFlow<Boolean> = _previewsEnabled

    fun setPrivateChatsEnabled(enabled: Boolean) { _privateChatsEnabled.value = enabled }
    fun setGroupsEnabled(enabled: Boolean) { _groupsEnabled.value = enabled }
    fun setChannelsEnabled(enabled: Boolean) { _channelsEnabled.value = enabled }
    fun setSoundsEnabled(enabled: Boolean) { _soundsEnabled.value = enabled }
    fun setVibrateEnabled(enabled: Boolean) { _vibrateEnabled.value = enabled }
    fun setPreviewsEnabled(enabled: Boolean) { _previewsEnabled.value = enabled }
}
