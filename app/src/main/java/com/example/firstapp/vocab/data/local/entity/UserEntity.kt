package com.example.firstapp.vocab.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val email: String,
    val display_name: String,
    val phone_number: String = "",
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
    val streakFreezes: Int = 0,
    val lastStreakActivity: Long = 0,
    val walletBalance: Double = 0.0,
    val bannerUrl: String? = null,
    val isArtist: Boolean = false,
    val jobFullName: String? = null,
    val jobEmail: String? = null,
    val jobPhone: String? = null,
    val jobLocation: String? = null,
    val jobEducation: String? = null,
    val jobExperience: String? = null,
    val jobSkills: String? = null,
    val jobAboutMe: String? = null,
    val jobLinkedinUrl: String? = null,
    val jobPortfolioUrl: String? = null,
    val jobWorkPreference: String? = null,
    val jobAvailability: String? = null,
    val jobExpectedSalary: String? = null
)
