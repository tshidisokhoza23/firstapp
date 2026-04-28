package com.example.firstapp.vocab.domain.repository

import com.example.firstapp.model.User
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserData(userId: Int): Flow<User?>
    fun getCurrentUser(): Flow<Resource<User>>
    suspend fun ensureUserExists(userId: Int)
    suspend fun updateUserXp(userId: Int, xpDelta: Int)
    suspend fun recordLessonResult(userId: Int, lessonId: String, score: Int, maxScore: Int)
    suspend fun incrementAdsWatched(userId: Int)
    suspend fun resetDailyAds(userId: Int)
    suspend fun setPremiumStatus(userId: Int, isPremium: Boolean)
    suspend fun setNativeLanguage(userId: Int, language: String)
    suspend fun setTargetLanguage(userId: Int, language: String)
    suspend fun useStreakFreeze(userId: Int)
    suspend fun buyStreakFreeze(userId: Int)
    fun updateUser(displayName: String?, bio: String?, location: String?, avatarUrl: String?): Flow<Resource<User>>
    fun forgotPassword(emailOrUsername: String): Flow<Resource<Boolean>>
    fun deleteAccount(): Flow<Resource<Boolean>>
}
