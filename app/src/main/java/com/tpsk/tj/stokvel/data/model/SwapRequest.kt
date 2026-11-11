package com.tpsk.tj.stokvel.data.model

import java.util.Date

data class SwapRequest(
    val id: String,
    val groupId: String,
    val requesterMemberId: String,
    val targetMemberId: String,
    val status: String, // "pending_confirmation", "confirmed", "rejected"
    val createdAt: Date
)
