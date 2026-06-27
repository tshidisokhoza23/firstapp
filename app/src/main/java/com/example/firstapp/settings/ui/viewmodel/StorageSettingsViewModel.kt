package com.example.firstapp.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


@HiltViewModel
class StorageSettingsViewModel @Inject constructor() : ViewModel() {
    private val _autoplayGifs = MutableStateFlow(true)
    val autoplayGifs: StateFlow<Boolean> = _autoplayGifs

    private val _autoplayVideos = MutableStateFlow(true)
    val autoplayVideos: StateFlow<Boolean> = _autoplayVideos

    private val _streamingVideos = MutableStateFlow(true)
    val streamingVideos: StateFlow<Boolean> = _streamingVideos

    private val _storageUsage = MutableStateFlow("1.2 GB")
    val storageUsage: StateFlow<String> = _storageUsage

    private val _networkUsage = MutableStateFlow("14.5 GB")
    val networkUsage: StateFlow<String> = _networkUsage

    fun setAutoplayGifs(enabled: Boolean) { _autoplayGifs.value = enabled }
    fun setAutoplayVideos(enabled: Boolean) { _autoplayVideos.value = enabled }
    fun setStreamingVideos(enabled: Boolean) { _streamingVideos.value = enabled }
    
    fun clearCache() {
        // Logic to clear local cache
        _storageUsage.value = "0.0 GB"
    }
}
