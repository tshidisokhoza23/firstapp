package com.example.firstapp.model

data class CallLog(
    val id: Int,
    val sessionId: String,
    val callerId: Int,
    val calleeId: Int,
    val contactId: Int,
    val contactName: String,
    val contactAvatar: String? = null,
    val callType: String,
    val status: String,
    val durationSeconds: Int,
    val startedAt: String,
    val endedAt: String? = null
)
