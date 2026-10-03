package com.tpsk.tj.settings.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.common.data.api.RefreshTokenRequest
import com.tpsk.tj.common.data.api.UserApi
import com.tpsk.tj.common.domain.auth.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DeviceSession(
    val id: String,
    val deviceName: String,
    val location: String,
    val lastActive: String,
    val isCurrent: Boolean = false
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val userApi: UserApi,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _sessions = MutableStateFlow(
        listOf(DeviceSession("this-device", Build.MODEL.ifBlank { "This phone" }, "This phone", "Active now", true))
    )
    val sessions: StateFlow<List<DeviceSession>> = _sessions
    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            try {
                val response = userApi.listSessions()
                if (!response.isSuccessful) {
                    _note.value = "Other phones were not returned."
                    return@launch
                }
                val others = response.body()?.sessions.orEmpty().map { row ->
                    val whenSigned = if (row.created_at > 0) {
                        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(row.created_at))
                    } else {
                        "time not returned"
                    }
                    DeviceSession(row.id, "Signed in", "", whenSigned, false)
                }
                _sessions.value = listOf(
                    DeviceSession("this-device", Build.MODEL.ifBlank { "This phone" }, "This phone", "Active now", true)
                ) + others
                _note.value = if (others.isEmpty()) "No other sessions were returned." else ""
            } catch (_: Exception) {
                _note.value = "Other phones were not returned."
            }
        }
    }

    fun terminateSession(sessionId: String) {
        if (sessionId == "this-device" || sessionId.isBlank()) return
        viewModelScope.launch {
            try {
                val response = userApi.revokeSession(sessionId)
                if (response.isSuccessful) refresh() else _note.value = "That session was not signed out."
            } catch (_: Exception) {
                _note.value = "That session was not signed out."
            }
        }
    }

    fun revokeOthers() {
        val token = sessionManager.getRefreshToken()
        if (token.isNullOrBlank()) {
            _note.value = "This phone has no refresh token to keep."
            return
        }
        viewModelScope.launch {
            try {
                val response = userApi.revokeOtherSessions(RefreshTokenRequest(token))
                _note.value = if (response.isSuccessful) "Other phones were signed out." else "Other phones were not signed out."
                if (response.isSuccessful) refresh()
            } catch (_: Exception) {
                _note.value = "Other phones were not signed out."
            }
        }
    }
}
