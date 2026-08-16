package com.example.firstapp.chat.model

data class RewardTask(
    val id: String,
    val title: String,
    val points: Int,
    val is_active: Boolean,
    val is_completed: Boolean
)

data class RewardDashboard(
    val total_points: Int,
    val ads_watched_today: Int,
    val max_daily_ads: Int,
    val cooldown_remaining_seconds: Int,
    val tasks: List<RewardTask>
)
