package com.example.firstapp.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class StorageSettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {
    val autoplayGifs: StateFlow<Boolean> = repository.autoplayGifs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val autoplayVideos: StateFlow<Boolean> = repository.autoplayVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val streamingVideos: StateFlow<Boolean> = repository.streamingVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _storageUsage = MutableStateFlow("1.2 GB")
    val storageUsage: StateFlow<String> = _storageUsage

    private val _networkUsage = MutableStateFlow("14.5 GB")
    val networkUsage: StateFlow<String> = _networkUsage

    fun setAutoplayGifs(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoplayGifs(enabled) }
    }
    fun setAutoplayVideos(enabled: Boolean) {
        viewModelScope.launch { repository.setAutoplayVideos(enabled) }
    }
    fun setStreamingVideos(enabled: Boolean) {
        viewModelScope.launch { repository.setStreamingVideos(enabled) }
    }
    
    fun clearCache() {
        // Logic to clear local cache
        _storageUsage.value = "0.0 GB"
    }
}
