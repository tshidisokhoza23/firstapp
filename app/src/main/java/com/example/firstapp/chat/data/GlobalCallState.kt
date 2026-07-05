package com.example.firstapp.chat.data

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

    private val _activeParticipantId = MutableStateFlow<String?>(null)
    val activeParticipantId: StateFlow<String?> = _activeParticipantId.asStateFlow()

    private val _activeCallType = MutableStateFlow<CallType?>(null)
    val activeCallType: StateFlow<CallType?> = _activeCallType.asStateFlow()

    private val _isConnectionEstablished = MutableStateFlow(false)
    val isConnectionEstablished: StateFlow<Boolean> = _isConnectionEstablished.asStateFlow()

    private val _isRinging = MutableStateFlow(false)
    val isRinging: StateFlow<Boolean> = _isRinging.asStateFlow()

    fun setCallActive(active: Boolean, sessionId: String? = null, participantId: String? = null, callType: CallType? = null) {
        _isCallActive.value = active
        _activeSessionId.value = sessionId
        _activeParticipantId.value = participantId
        _activeCallType.value = callType
        if (!active) {
            _isConnectionEstablished.value = false
            _isRinging.value = false
        }
    }

    fun setConnectionEstablished(established: Boolean) {
        _isConnectionEstablished.value = established
        if (established) _isRinging.value = false
    }

    fun setRinging(ringing: Boolean) {
        _isRinging.value = ringing
    }

    /** Set by CallNotificationActionReceiver when user taps Accept in notification. */
    private val _autoAcceptSessionId = MutableStateFlow<String?>(null)
    val autoAcceptSessionId: StateFlow<String?> = _autoAcceptSessionId.asStateFlow()

    /** Simple id→name cache updated by the ViewModel whenever chats/contacts are loaded. */
    private val contactNames = mutableMapOf<String, String>()
    private val contactPhones = mutableMapOf<String, String>()

    fun updateContactInfo(userId: String, name: String, phone: String?) {
        contactNames[userId] = name
        if (phone != null) contactPhones[userId] = phone
    }

    fun getContactName(userId: String): String? = contactNames[userId]
    fun getContactPhone(userId: String): String? = contactPhones[userId]

    fun onCallInvite(event: CallEvent) {
        // Enrich with known name if not already set or if it's a generic "User " name
        val senderIdStr = event.senderId.toString()
        val enriched = if (event.senderName.isBlank() || event.senderName.startsWith("User ")) {
            val name = contactNames[senderIdStr] ?: contactPhones[senderIdStr] ?: event.senderName
            event.copy(senderName = name)
        } else event
        _incomingCall.value = enriched
    }

    fun clearIncomingCall() {
        _incomingCall.value = null
    }

    fun acceptCall(event: CallEvent, webRtcCallManager: WebRtcCallManager) {
        setCallActive(true, event.sessionId, event.senderId.toString(), event.callType)
        setConnectionEstablished(true)
        clearIncomingCall()
        webRtcCallManager.start(event.callType)
        CallWebSocketClient.accept(event.senderId, event.sessionId, event.callType)
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
