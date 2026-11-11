package com.tpsk.tj.stokvel.data.model

import java.util.Date

enum class StokvelType {
    UMGALELO_ROTATION,
    UMTSHELO_PARTY,
    BULK_GROCERIES
}

data class StokvelGroup(
    val id: String,
    val name: String,
    val type: StokvelType,
    val monthlyTargetAmount: Double,
    val cycleInterval: String = "monthly",
    val creatorUserId: String,
    val bankName: String? = null,
    val accountNumberHint: String? = null,
    val payshapId: String? = null,
    val currentCycleIndex: Int = 1,
    val createdAt: Date
)
