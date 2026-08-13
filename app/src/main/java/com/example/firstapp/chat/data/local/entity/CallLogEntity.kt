package com.example.firstapp.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String, 
    val sessionId: String,
    val callerId: String,
    val calleeId: String,
    val contactId: String,
    val contactName: String,
    val contactAvatar: String?,
    val callType: String,
    val status: String,
    val durationSeconds: Int,
    val timestamp: Long,
    val endedAt: String?
)
