package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String, // Changed from Int to String (UUID)
    val sessionId: String,
    val callerId: Int,
    val calleeId: Int,
    val contactId: Int,
    val contactName: String,
    val contactAvatar: String?,
    val callType: String,
    val status: String,
    val durationSeconds: Int,
    val timestamp: Long, // Changed from startedAt: String
    val endedAt: String?
)
