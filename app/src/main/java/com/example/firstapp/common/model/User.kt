package com.example.firstapp.common.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Int,
    val username: String = "",
    val email: String? = null,
    val display_name: String = "",
    val phone_number: String? = null,
    val created_at: String = "",
    val avatar_url: String? = null,
    val banner_url: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val is_verified: Boolean = false,
    @SerializedName("is_artist") val isArtist: Boolean = false,
    val followers_count: Int = 0,
    val following_count: Int = 0,
    
    // Vocab specific fields
    val xp: Int = 0,
    val level: Int = 1,
    val streak: Int = 0,
    @SerializedName("is_premium") val isPremium: Boolean = false,
    @SerializedName("last_daily_unlock") val lastDailyUnlock: Long = 0,
    @SerializedName("ads_watched_today") val adsWatchedToday: Int = 0,
    @SerializedName("unlocked_lesson_count") val unlockedLessonCount: Int = 1,
    @SerializedName("native_language") val nativeLanguage: String = "English",
    @SerializedName("target_language") val targetLanguage: String = "Spanish",
    val completedLessonIds: List<String> = emptyList(),
    val lessonMastery: Map<String, Int> = emptyMap(),
    @SerializedName("streak_freezes") val streakFreezes: Int = 0,
    @SerializedName("last_streak_activity") val lastStreakActivity: Long = 0,
    @SerializedName("wallet_balance") val walletBalance: Int = 0,

    // Job Profile Fields
    @SerializedName("job_full_name") val jobFullName: String? = null,
    @SerializedName("job_email") val jobEmail: String? = null,
    @SerializedName("job_phone") val jobPhone: String? = null,
    @SerializedName("job_location") val jobLocation: String? = null,
    @SerializedName("job_education") val jobEducation: String? = null,
    @SerializedName("job_experience") val jobExperience: String? = null,
    @SerializedName("job_skills") val jobSkills: String? = null,
    @SerializedName("job_about_me") val jobAboutMe: String? = null,
    @SerializedName("job_linkedin_url") val jobLinkedinUrl: String? = null,
    @SerializedName("job_portfolio_url") val jobPortfolioUrl: String? = null,
    @SerializedName("job_work_preference") val jobWorkPreference: String? = null,
    @SerializedName("job_availability") val jobAvailability: String? = null,
    @SerializedName("job_expected_salary") val jobExpectedSalary: String? = null,
    
    // E2EE
    @SerializedName("public_key") val publicKey: String? = null
)
