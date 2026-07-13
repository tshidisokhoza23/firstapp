package com.example.firstapp.vocab.domain.repository

import com.example.firstapp.common.model.User
import com.example.firstapp.common.util.Resource
import com.example.firstapp.profile.model.WorkExperience
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserData(userId: String): Flow<User?>
    fun observeCurrentUser(): Flow<User?>
    fun getCurrentUser(): Flow<Resource<User>>
    suspend fun ensureUserExists(userId: String)
    suspend fun updateUserXp(userId: String, xpDelta: Int)
    suspend fun recordLessonResult(userId: String, lessonId: String, score: Int, maxScore: Int)
    suspend fun incrementAdsWatched(userId: String)
    suspend fun resetDailyAds(userId: String)
    suspend fun setPremiumStatus(userId: String, isPremium: Boolean)
    suspend fun setNativeLanguage(userId: String, language: String)
    suspend fun setTargetLanguage(userId: String, language: String)
    suspend fun useStreakFreeze(userId: String)
    suspend fun buyStreakFreeze(userId: String)
    fun updateUser(displayName: String?, bio: String?, location: String?, avatarUrl: String?, bannerUrl: String? = null, isArtist: Boolean? = null): Flow<Resource<User>>
    fun forgotPassword(emailOrUsername: String): Flow<Resource<Boolean>>
    fun deleteAccount(): Flow<Resource<Boolean>>
    fun login(username: String, password: String): Flow<Resource<com.example.firstapp.common.data.api.AuthResponse>>
    fun register(username: String, email: String, displayName: String, phoneNumber: String, password: String): Flow<Resource<com.example.firstapp.common.data.api.AuthResponse>>
    fun requestOTP(phoneNumber: String): Flow<Resource<String>>
    fun verifyOTP(phoneNumber: String, code: String): Flow<Resource<Boolean>>

    // Work Experience
    fun getWorkExperiences(): Flow<Resource<List<WorkExperience>>>
    fun addWorkExperience(experience: WorkExperience): Flow<Resource<String>>
    fun updateWorkExperience(experience: WorkExperience): Flow<Resource<Boolean>>
    fun deleteWorkExperience(experienceId: String): Flow<Resource<Boolean>>
}
