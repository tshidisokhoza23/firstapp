package com.tpsk.tj.vocab.domain.repository

import com.tpsk.tj.common.model.User
import com.tpsk.tj.common.util.Resource
import com.tpsk.tj.profile.model.WorkExperience
import kotlinx.coroutines.flow.Flow
import android.content.Context
import android.net.Uri

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
    fun updateAvatar(file: java.io.File): Flow<Resource<String>>
    fun updateAvatar(context: Context, uri: Uri): Flow<Resource<String>>
    fun forgotPassword(emailOrUsername: String): Flow<Resource<Boolean>>
    fun googleSignIn(idToken: String): Flow<Resource<com.tpsk.tj.common.data.api.AuthResponse>>
    fun completeProfile(displayName: String?, username: String?, phoneNumber: String?, categories: List<String>? = null): Flow<Resource<User>>
    fun deleteAccount(): Flow<Resource<Boolean>>
    fun login(username: String, password: String): Flow<Resource<com.tpsk.tj.common.data.api.AuthResponse>>
    fun register(username: String, email: String, displayName: String, phoneNumber: String, password: String, countryCode: String? = null, location: String? = null): Flow<Resource<com.tpsk.tj.common.data.api.AuthResponse>>
    fun requestOTP(phoneNumber: String, countryCode: String? = null): Flow<Resource<String>>

    fun verifyOTP(phoneNumber: String, code: String): Flow<Resource<Boolean>>

    fun logout(): Flow<Resource<Boolean>>
    suspend fun updateUserLocally(user: User)

    // Work Experience
    fun getWorkExperiences(): Flow<Resource<List<WorkExperience>>>
    fun addWorkExperience(experience: WorkExperience): Flow<Resource<String>>
    fun updateWorkExperience(experience: WorkExperience): Flow<Resource<Boolean>>
    fun deleteWorkExperience(experienceId: String): Flow<Resource<Boolean>>
}
