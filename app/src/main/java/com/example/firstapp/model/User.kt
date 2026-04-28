package com.example.firstapp.model

data class User(
    val id: Int,
    val username: String = "",
    val display_name: String = "",
    val phone_number: String? = null,
    val created_at: String = "",
    val avatar_url: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val is_verified: Boolean = false,
    
    // Vocab specific fields
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    val isPremium: Boolean = false,
    val lastDailyUnlock: Long = 0,
    val adsWatchedToday: Int = 0,
    val unlockedLessonCount: Int = 1,
    val nativeLanguage: String = "English",
    val targetLanguage: String = "Spanish",
    val completedLessonIds: List<String> = emptyList(),
    val lessonMastery: Map<String, Int> = emptyMap(),
    val streakFreezes: Int = 0,
    val lastStreakActivity: Long = 0
)
