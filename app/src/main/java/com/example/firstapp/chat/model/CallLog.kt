package com.example.firstapp.chat.model

data class CallLog(
    val id: String, // Changed from Int to String (UUID) - Rule 14
    val sessionId: String,
    val callerId: Int,
    val calleeId: Int,
    val contactId: Int,
    val contactName: String,
    val contactAvatar: String? = null,
    val callType: String, // "voice" | "video"
    val status: String, // "missed" | "incoming" | "outgoing"
    val durationSeconds: Int,
    val timestamp: Long, // Changed from startedAt: String - Rule 14
    val endedAt: String? = null
)
