package com.example.firstapp.home.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MainViewModel now only handles global UI state and navigation requests.
 * Domain-specific logic has been moved to specialized ViewModels:
 * - [com.example.firstapp.ui.viewmodel.ChatViewModel]
 * - [com.example.firstapp.ui.viewmodel.SocialViewModel]
 * - [com.example.firstapp.media.ui.screens.shorts.ShortsViewModel]
 * - [com.example.firstapp.ui.viewmodel.DiscoveryViewModel]
 * - [com.example.firstapp.ui.viewmodel.JobViewModel]
 * - [com.example.firstapp.ui.viewmodel.UserViewModel]
 * - [com.example.firstapp.drive.ui.screens.home.DriveViewModel]
 */
import com.example.firstapp.domain.repository.ChatRepository

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _navigationRequest = MutableStateFlow<String?>(null)
    val navigationRequest: StateFlow<String?> = _navigationRequest.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun requestNavigation(route: String) {
        _navigationRequest.value = route
    }

    fun clearNavigationRequest() {
        _navigationRequest.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun syncPendingMessages() {
        viewModelScope.launch {
            repository.syncPendingMessages()
        }
    }

    init {
        syncPendingMessages()
    }
}
