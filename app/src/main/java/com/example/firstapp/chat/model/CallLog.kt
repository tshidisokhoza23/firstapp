package com.example.firstapp.chat.model

data class CallLog(
    val id: String, 
    val sessionId: String,
    val callerId: String,
    val calleeId: String,
    val contactId: String,
    val contactName: String,
    val contactAvatar: String? = null,
    val callType: String, // "voice" | "video"
    val status: String, // "missed" | "incoming" | "outgoing"
    val durationSeconds: Int,
    val timestamp: Long,
    val endedAt: String? = null
)
