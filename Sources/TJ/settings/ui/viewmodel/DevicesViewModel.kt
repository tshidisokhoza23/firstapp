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
        DeviceSession("this-device", android.os.Build.MODEL.ifBlank { "This phone" }, "This device only", "Active now", true)
    ))
    val sessions: StateFlow<List<DeviceSession>> = _sessions

    fun terminateSession(sessionId: String) {
        // Remote session logout is not connected. Never pretend another device was signed out.
        if (sessionId == "this-device") return
        _sessions.value = _sessions.value.filter { it.id != sessionId }
    }
}
