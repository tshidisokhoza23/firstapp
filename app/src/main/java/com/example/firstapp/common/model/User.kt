package com.example.firstapp.common.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String = "",
    @SerializedName("email") val email: String = "",
    @SerializedName("display_name") val displayName: String = "",
    @SerializedName("phone_number") val phoneNumber: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    @SerializedName("banner_url") val bannerUrl: String? = null,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("location") val location: String? = null,
    @SerializedName("is_verified") val isVerified: Boolean = false,
    @SerializedName("is_artist") val isArtist: Boolean = false,
    @SerializedName("is_driver") val isDriver: Boolean = false,
    @SerializedName("is_profile_complete") val isProfileComplete: Boolean = true,
    @SerializedName("missing_fields") val missingFields: List<String>? = emptyList(),

    @SerializedName("followers_count") val followersCount: Int = 0,
    @SerializedName("following_count") val followingCount: Int = 0,
    @SerializedName("is_following") val isFollowing: Boolean = false,
    
    // Vocab specific fields
    @SerializedName("xp") val xp: Int = 0,
    @SerializedName("level") val level: Int = 1,
    @SerializedName("streak") val streak: Int = 0,
    @SerializedName("is_premium") val isPremium: Boolean = false,
    @SerializedName("last_daily_unlock") val lastDailyUnlock: Long = 0,
    @SerializedName("ads_watched_today") val adsWatchedToday: Int = 0,
    @SerializedName("unlocked_lesson_count") val unlockedLessonCount: Int = 1,
    @SerializedName("native_language") val nativeLanguage: String = "English",
    @SerializedName("target_language") val targetLanguage: String = "Spanish",
    @SerializedName("completed_lesson_ids") val completedLessonIds: List<String> = emptyList(),
    @SerializedName("lesson_mastery") val lessonMastery: Map<String, Int> = emptyMap(),
    @SerializedName("streak_freezes") val streakFreezes: Int = 0,
    @SerializedName("last_streak_activity") val lastStreakActivity: Long = 0,
    @SerializedName("wallet_balance") val walletBalance: Double = 0.0,

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
