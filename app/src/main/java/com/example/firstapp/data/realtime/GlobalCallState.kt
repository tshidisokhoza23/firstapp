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

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _activeParticipantId = MutableStateFlow<Int?>(null)
    val activeParticipantId: StateFlow<Int?> = _activeParticipantId.asStateFlow()

    private val _activeCallType = MutableStateFlow<CallType?>(null)
    val activeCallType: StateFlow<CallType?> = _activeCallType.asStateFlow()

    private val _isConnectionEstablished = MutableStateFlow(false)
    val isConnectionEstablished: StateFlow<Boolean> = _isConnectionEstablished.asStateFlow()

    fun setCallActive(active: Boolean, sessionId: String? = null, participantId: Int? = null, callType: CallType? = null) {
        _isCallActive.value = active
        _activeSessionId.value = sessionId
        _activeParticipantId.value = participantId
        _activeCallType.value = callType
        if (!active) _isConnectionEstablished.value = false
    }

    fun setConnectionEstablished(established: Boolean) {
        _isConnectionEstablished.value = established
    }

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
        if (_incomingCall.value?.sessionId == sessionId || sessionId == "force_all") {
            _incomingCall.value = null
        }
        if (_autoAcceptSessionId.value == sessionId || sessionId == "force_all") {
            _autoAcceptSessionId.value = null
        }
        if (_activeSessionId.value == sessionId || sessionId == "force_all") {
            _isCallActive.value = false
            _activeSessionId.value = null
            _activeParticipantId.value = null
            _activeCallType.value = null
            _isConnectionEstablished.value = false
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
        _isCallActive.value = false
        _activeSessionId.value = null
        _activeParticipantId.value = null
        _activeCallType.value = null
        _isConnectionEstablished.value = false
    }
}
