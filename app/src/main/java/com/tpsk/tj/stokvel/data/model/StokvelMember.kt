package com.tpsk.tj.stokvel.data.model

import java.util.Date

enum class MemberRole {
    ADMIN,
    TREASURER,
    MEMBER
}

data class StokvelMember(
    val id: String,
    val groupId: String,
    val userId: String,
    val role: MemberRole,
    val payoutOrder: Int?, // NULL for non-rotational, 1..N for umgalelo
    val reliabilityScore: Double = 1.0,
    val joinedAt: Date
)
