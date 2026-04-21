package com.example.firstapp.data.realtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide state for a pending incoming call.
 * Updated by CallListenerService (and ChatScreen) so the overlay in MainScreen
 * can show an in-app incoming-call UI regardless of which screen is visible.
 */
object GlobalCallState {

    private val _incomingCall = MutableStateFlow<CallEvent?>(null)
    val incomingCall: StateFlow<CallEvent?> = _incomingCall.asStateFlow()

    /** Set by CallNotificationActionReceiver when user taps Accept in notification. */
    private val _autoAcceptSessionId = MutableStateFlow<String?>(null)
    val autoAcceptSessionId: StateFlow<String?> = _autoAcceptSessionId.asStateFlow()

    /** Simple id→name cache updated by the ViewModel whenever chats/contacts are loaded. */
    private val contactNames = mutableMapOf<Int, String>()

    fun updateContactName(userId: Int, name: String) {
        contactNames[userId] = name
    }

    fun getContactName(userId: Int): String? = contactNames[userId]

    fun onCallInvite(event: CallEvent) {
        // Enrich with known name if not already set
        val enriched = if (event.senderName.isBlank()) {
            val name = contactNames[event.senderId] ?: ""
            event.copy(senderName = name)
        } else event
        _incomingCall.value = enriched
    }

    fun onCallEnded(sessionId: String) {
        if (_incomingCall.value?.sessionId == sessionId) {
            _incomingCall.value = null
        }
        if (_autoAcceptSessionId.value == sessionId) {
            _autoAcceptSessionId.value = null
        }
    }

    fun setAutoAccept(sessionId: String) {
        _autoAcceptSessionId.value = sessionId
    }

    fun clearAutoAccept() {
        _autoAcceptSessionId.value = null
    }

    fun clear() {
        _incomingCall.value = null
        _autoAcceptSessionId.value = null
    }
}
