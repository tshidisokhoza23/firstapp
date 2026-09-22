package com.tpsk.tj.settings.ui.viewmodel

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


data class DeviceSession(
    val id: String,
    val deviceName: String,
    val location: String,
    val lastActive: String,
    val isCurrent: Boolean = false
)

@HiltViewModel
class DevicesViewModel @Inject constructor() : ViewModel() {
    private val _sessions = MutableStateFlow(listOf(
        DeviceSession("1", "Samsung SM-A546E", "Pretoria, South Africa", "Active now", true),
        DeviceSession("2", "Windows PC", "Johannesburg, South Africa", "2 hours ago"),
        DeviceSession("3", "iPhone 13", "Cape Town, South Africa", "Last week")
    ))
    val sessions: StateFlow<List<DeviceSession>> = _sessions

    fun terminateSession(sessionId: String) {
        _sessions.value = _sessions.value.filter { it.id != sessionId }
    }
}
