package com.example.firstapp.chat.model

data class RewardTask(
    val id: String,
    val title: String,
    val points: Double,
    val is_active: Boolean,
    val is_completed: Boolean
)

data class RewardDashboard(
    val total_points: Double,
    val ads_watched_today: Int,
    val max_daily_ads: Int,
    val cooldown_remaining_seconds: Int,
    val tasks: List<RewardTask>
)
