package com.example.firstapp.chat.rewards.data

import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.chat.rewards.data.api.AwardPointsRequest
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

    /**
     * Fetches the latest balance from backend and updates local state.
     */
    suspend fun refreshBalance(): Boolean {
        return try {
            val response = ApiClient.rewardsApi.getBalance()
            if (response.isSuccessful && response.body() != null) {
                _balance.value = response.body()!!.balance
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Awards points through backend, then refreshes authoritative balance.
     */
    suspend fun awardPoints(amount: Double, source: String, description: String): Boolean {
        if (amount <= 0.0) return false
        return try {
            val response = ApiClient.rewardsApi.awardPoints(
                AwardPointsRequest(
                    amount = amount,
                    source = source,
                    description = description
                )
            )
            if (response.isSuccessful) {
                refreshBalance()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }
}
