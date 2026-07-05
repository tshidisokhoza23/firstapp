package com.example.firstapp.rewards.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RewardsManager {
    private val _balance = MutableStateFlow(0.0)
    val balance = _balance.asStateFlow()

    /**
     * Smart Sync: Called by WebSocketHub when a 'balance_update' event arrives.
     */
    fun onBalanceUpdate(newBalance: Double) {
        _balance.value = newBalance
    }

    /**
     * Optimistic UI: Update balance locally while waiting for server confirmation
     */
    fun addOptimisticReward(amount: Double) {
        _balance.value += amount
    }
}
