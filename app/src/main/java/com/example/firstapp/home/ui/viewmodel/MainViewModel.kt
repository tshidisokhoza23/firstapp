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
import com.example.firstapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val autoNightMode = settingsRepository.autoNightMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isDarkMode = settingsRepository.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val animationsEnabled = settingsRepository.animationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _selectedBottomItem = MutableStateFlow(0)
    val selectedBottomItem: StateFlow<Int> = _selectedBottomItem.asStateFlow()

    private val tabHistory = mutableListOf<Int>()

    fun setSelectedBottomItem(index: Int) {
        if (_selectedBottomItem.value != index) {
            if (_selectedBottomItem.value != 5) {
                tabHistory.remove(index)
                tabHistory.add(_selectedBottomItem.value)
            }
            _selectedBottomItem.value = index
        }
    }

    fun popTabHistory(): Boolean {
        if (tabHistory.isNotEmpty()) {
            _selectedBottomItem.value = tabHistory.removeAt(tabHistory.size - 1)
            return true
        }
        return false
    }

    fun hasTabHistory(): Boolean = tabHistory.isNotEmpty()

    private val _selectedTabItem = MutableStateFlow(0)
    val selectedTabItem: StateFlow<Int> = _selectedTabItem.asStateFlow()

    private val _navigationRequest = MutableStateFlow<String?>(null)
    val navigationRequest: StateFlow<String?> = _navigationRequest.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun setSelectedTabItem(index: Int) {
        _selectedTabItem.value = index
    }

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
