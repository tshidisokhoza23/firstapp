package com.tpsk.tj.media.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.media.domain.repository.SocialRepository
import com.tpsk.tj.media.model.SocialNotificationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SocialNotificationsViewModel @Inject constructor(
    private val socialRepository: SocialRepository
) : ViewModel() {

    private val _notificationsList = MutableStateFlow<List<SocialNotificationItem>>(emptyList())
    val notificationsList: StateFlow<List<SocialNotificationItem>> = _notificationsList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadNotifications()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            socialRepository.getSocialNotifications().collect { res ->
                when (res) {
                    is Resource.Loading -> {
                        _isLoading.value = true
                        _errorMessage.value = null
                    }
                    is Resource.Success -> {
                        _isLoading.value = false
                        _notificationsList.value = res.data.orEmpty()
                    }
                    is Resource.Error -> {
                        _isLoading.value = false
                        _errorMessage.value = res.message
                    }
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        // 1. Instant optimistic local state update
        _notificationsList.update { list ->
            list.map { item ->
                if (item.id == notificationId) item.copy(isRead = true) else item
            }
        }
        // 2. Sync read receipt with Go backend server
        viewModelScope.launch {
            socialRepository.markNotificationRead(notificationId).collect {}
        }
    }

    fun markAllAsRead() {
        _notificationsList.update { list ->
            list.map { it.copy(isRead = true) }
        }
        viewModelScope.launch {
            socialRepository.markAllNotificationsRead().collect {}
        }
    }
}
