package com.example.firstapp.vocab.data.repository

import com.example.firstapp.vocab.data.local.dao.UserDao
import com.example.firstapp.vocab.data.local.dao.LessonProgressDao
import com.example.firstapp.vocab.data.local.entity.LessonProgressEntity
import com.example.firstapp.vocab.data.local.entity.UserEntity
import com.example.firstapp.common.model.User
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.chat.rewards.data.RewardsManager
import com.example.firstapp.common.data.api.UpdateUserRequest
import com.example.firstapp.common.data.api.ForgotPasswordRequest
import com.example.firstapp.common.util.Resource
import com.example.firstapp.profile.model.WorkExperience
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map as flowMap
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.util.*
import kotlin.collections.*
import kotlin.text.*
import kotlin.*
import java.net.UnknownHostException
import java.util.Calendar

import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val lessonProgressDao: LessonProgressDao
) : UserRepository {

    private val apiService = ApiClient.userApi

    override fun getUserData(userId: String): Flow<User?> {
        val userFlow = userDao.getUser(userId)
        val progressFlow = lessonProgressDao.getAllProgress(userId)

        return combine(userFlow, progressFlow) { entity, progressList ->
            if (entity != null) {
                User(
                    id = entity.id,
                    username = entity.username,
                    email = entity.email,
                    displayName = entity.display_name,
                    phoneNumber = entity.phone_number,
                    createdAt = entity.created_at,
                    avatarUrl = entity.avatar_url,
                    bio = entity.bio,
                    location = entity.location,
                    isVerified = entity.is_verified,
                    xp = entity.xp,
                    level = entity.level,
                    streak = entity.streak,
                    isPremium = entity.isPremium,
                    lastDailyUnlock = entity.lastDailyUnlock,
                    adsWatchedToday = entity.adsWatchedToday,
                    unlockedLessonCount = entity.unlockedLessonCount,
                    nativeLanguage = entity.nativeLanguage,
                    targetLanguage = entity.targetLanguage,
                    streakFreezes = entity.streakFreezes,
                    lastStreakActivity = entity.lastStreakActivity,
                    walletBalance = entity.walletBalance,
                    bannerUrl = entity.bannerUrl,
                    isArtist = entity.isArtist,
                    jobFullName = entity.jobFullName,
                    jobEmail = entity.jobEmail,
                    jobPhone = entity.jobPhone,
                    jobLocation = entity.jobLocation,
                    jobEducation = entity.jobEducation,
                    jobExperience = entity.jobExperience,
                    jobSkills = entity.jobSkills,
                    jobAboutMe = entity.jobAboutMe,
                    jobLinkedinUrl = entity.jobLinkedinUrl,
                    jobPortfolioUrl = entity.jobPortfolioUrl,
                    jobWorkPreference = entity.jobWorkPreference,
                    jobAvailability = entity.jobAvailability,
                    jobExpectedSalary = entity.jobExpectedSalary,
                    completedLessonIds = progressList.filter { p -> p.isCompleted }.map { p -> p.lessonId },
                    lessonMastery = progressList.associate { p -> p.lessonId to p.score }
                )
            } else null
        }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().flowMap { entity ->
            if (entity != null) {
                User(
                    id = entity.id,
                    username = entity.username,
                    email = entity.email,
                    displayName = entity.display_name,
                    phoneNumber = entity.phone_number,
                    createdAt = entity.created_at,
                    avatarUrl = entity.avatar_url,
                    bio = entity.bio,
                    location = entity.location,
                    isVerified = entity.is_verified,
                    xp = entity.xp,
                    level = entity.level,
                    streak = entity.streak,
                    isPremium = entity.isPremium,
                    adsWatchedToday = entity.adsWatchedToday,
                    walletBalance = entity.walletBalance,
                    bannerUrl = entity.bannerUrl,
                    isArtist = entity.isArtist,
                    jobFullName = entity.jobFullName,
                    jobEmail = entity.jobEmail,
                    jobPhone = entity.jobPhone,
                    jobLocation = entity.jobLocation,
                    jobEducation = entity.jobEducation,
                    jobExperience = entity.jobExperience,
                    jobSkills = entity.jobSkills,
                    jobAboutMe = entity.jobAboutMe,
                    jobLinkedinUrl = entity.jobLinkedinUrl,
                    jobPortfolioUrl = entity.jobPortfolioUrl,
                    jobWorkPreference = entity.jobWorkPreference,
                    jobAvailability = entity.jobAvailability,
                    jobExpectedSalary = entity.jobExpectedSalary
                )
            } else null
        }
    }

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        
        // Try to get cached user first
        val cached = userDao.getCurrentUser().firstOrNull()
        if (cached != null) {
            val model = User(
                id = cached.id,
                username = cached.username,
                email = cached.email,
                displayName = cached.display_name,
                phoneNumber = cached.phone_number,
                createdAt = cached.created_at,
                avatarUrl = cached.avatar_url,
                bio = cached.bio,
                location = cached.location,
                isVerified = cached.is_verified,
                xp = cached.xp,
                level = cached.level,
                streak = cached.streak,
                isPremium = cached.isPremium,
                adsWatchedToday = cached.adsWatchedToday,
                walletBalance = cached.walletBalance,
                bannerUrl = cached.bannerUrl,
                isArtist = cached.isArtist,
                jobFullName = cached.jobFullName,
                jobEmail = cached.jobEmail,
                jobPhone = cached.jobPhone,
                jobLocation = cached.jobLocation,
                jobEducation = cached.jobEducation,
                jobExperience = cached.jobExperience,
                jobSkills = cached.jobSkills,
                jobAboutMe = cached.jobAboutMe,
                jobLinkedinUrl = cached.jobLinkedinUrl,
                jobPortfolioUrl = cached.jobPortfolioUrl,
                jobWorkPreference = cached.jobWorkPreference,
                jobAvailability = cached.jobAvailability,
                jobExpectedSalary = cached.jobExpectedSalary
            )
            emit(Resource.Success(model))
        }

        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                syncUserLocally(user)
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Failed to fetch user data"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Connection error"))
        }
    }

    private suspend fun syncUserLocally(user: User) {
        val entity = UserEntity(
            id = user.id,
            username = user.username,
            email = user.email,
            display_name = user.displayName,
            phone_number = user.phoneNumber,
            created_at = user.createdAt,
            avatar_url = user.avatarUrl,
            bio = user.bio,
            location = user.location,
            is_verified = user.isVerified,
            xp = user.xp,
            level = user.level,
            streak = user.streak,
            isPremium = user.isPremium,
            lastDailyUnlock = user.lastDailyUnlock,
            adsWatchedToday = user.adsWatchedToday,
            unlockedLessonCount = user.unlockedLessonCount,
            nativeLanguage = user.nativeLanguage,
            targetLanguage = user.targetLanguage,
            streakFreezes = user.streakFreezes,
            lastStreakActivity = user.lastStreakActivity,
            walletBalance = user.walletBalance,
            bannerUrl = user.bannerUrl,
            isArtist = user.isArtist,
            jobFullName = user.jobFullName,
            jobEmail = user.jobEmail,
            jobPhone = user.jobPhone,
            jobLocation = user.jobLocation,
            jobEducation = user.jobEducation,
            jobExperience = user.jobExperience,
            jobSkills = user.jobSkills,
            jobAboutMe = user.jobAboutMe,
            jobLinkedinUrl = user.jobLinkedinUrl,
            jobPortfolioUrl = user.jobPortfolioUrl,
            jobWorkPreference = user.jobWorkPreference,
            jobAvailability = user.jobAvailability,
            jobExpectedSalary = user.jobExpectedSalary
        )
        userDao.insertUser(entity)
    }

    private suspend fun syncUserToServer(entity: UserEntity) {
        try {
            apiService.updateUser(
                UpdateUserRequest(
                    display_name = entity.display_name,
                    bio = entity.bio,
                    location = entity.location,
                    avatar_url = entity.avatar_url,
                    xp = entity.xp,
                    level = entity.level,
                    streak = entity.streak,
                    is_premium = entity.isPremium,
                    last_daily_unlock = entity.lastDailyUnlock,
                    ads_watched_today = entity.adsWatchedToday,
                    unlocked_lesson_count = entity.unlockedLessonCount,
                    native_language = entity.nativeLanguage,
                    target_language = entity.targetLanguage,
                    streak_freezes = entity.streakFreezes,
                    last_streak_activity = entity.lastStreakActivity
                )
            )
        } catch (e: Exception) {
            // Ignore server sync failures for now, will sync later on next get
        }
    }

    override suspend fun ensureUserExists(userId: String) {
        val existingUser = userDao.getUser(userId).firstOrNull()
        if (existingUser == null) {
            val newUser = UserEntity(
                id = userId,
                username = "",
                email = "",
                display_name = "",
                phone_number = "",
                xp = 0,
                level = 1,
                streak = 0,
                isPremium = false,
                lastDailyUnlock = 0,
                adsWatchedToday = 0,
                walletBalance = 0.0
            )
            userDao.insertUser(newUser)
        }
    }

    override suspend fun setNativeLanguage(userId: String, language: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(nativeLanguage = language)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun setTargetLanguage(userId: String, language: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(targetLanguage = language)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun updateUserXp(userId: String, xpDelta: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val newXp = userEntity.xp + xpDelta
        val newLevel = (newXp / 100) + 1
        val updated = userEntity.copy(xp = newXp, level = newLevel)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun recordLessonResult(userId: String, lessonId: String, score: Int, maxScore: Int) {
        val mastery = if (maxScore > 0) ((score * 100) / maxScore) else 0
        val isCompleted = mastery >= 100
        val timestamp = System.currentTimeMillis()
        
        lessonProgressDao.saveProgress(
            LessonProgressEntity(
                lessonId = lessonId,
                userId = userId,
                isCompleted = isCompleted,
                score = mastery,
                lastAttemptTimestamp = timestamp
            )
        )

        if (isCompleted) {
            val userEntity = userDao.getUser(userId).firstOrNull()
            if (userEntity != null) {
                val lessonIndex = lessonId.substringAfter("_").substringBefore("_").toIntOrNull() ?: 0
                val nextUnlockCount = if (lessonIndex >= userEntity.unlockedLessonCount) {
                    userEntity.unlockedLessonCount + 1
                } else {
                    userEntity.unlockedLessonCount
                }
                userDao.updateUser(userEntity.copy(unlockedLessonCount = nextUnlockCount))
            }
            updateStreak(userId, timestamp)
            updateUserXp(userId, 20)
        }
    }

    private suspend fun updateStreak(userId: String, timestamp: Long) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        
        val lastDate = Calendar.getInstance().apply { timeInMillis = userEntity.lastDailyUnlock }
        val currentDate = Calendar.getInstance().apply { timeInMillis = timestamp }
        
        val isSameDay = lastDate.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                lastDate.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR)
                
        if (!isSameDay) {
            val yesterday = Calendar.getInstance().apply { 
                timeInMillis = timestamp
                add(Calendar.DAY_OF_YEAR, -1)
            }
            
            val wasConsecutive = lastDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    lastDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
            
            var newStreak = if (wasConsecutive) userEntity.streak + 1 else 1
            var usedFreeze = false
            
            if (!wasConsecutive && userEntity.streak > 0) {
                val lastActivity = Calendar.getInstance().apply { timeInMillis = userEntity.lastStreakActivity }
                val isFreezeActive = lastActivity.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        lastActivity.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)

                if (isFreezeActive || userEntity.streakFreezes > 0) {
                    newStreak = userEntity.streak + 1
                    usedFreeze = !isFreezeActive
                }
            }

            val updated = userEntity.copy(
                streak = newStreak,
                lastDailyUnlock = timestamp,
                streakFreezes = if (usedFreeze) userEntity.streakFreezes - 1 else userEntity.streakFreezes
            )
            userDao.updateUser(updated)
            syncUserToServer(updated)
        }
    }

    override suspend fun incrementAdsWatched(userId: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val newAdsWatched = userEntity.adsWatchedToday + 1
        val shouldUnlock = newAdsWatched >= 2
        
        val updated = userEntity.copy(
            adsWatchedToday = newAdsWatched,
            unlockedLessonCount = if (shouldUnlock) userEntity.unlockedLessonCount + 1 else userEntity.unlockedLessonCount
        )
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun resetDailyAds(userId: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(adsWatchedToday = 0)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun setPremiumStatus(userId: String, isPremium: Boolean) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(isPremium = isPremium)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun useStreakFreeze(userId: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        if (userEntity.streakFreezes > 0) {
            val updated = userEntity.copy(
                streakFreezes = userEntity.streakFreezes - 1,
                lastStreakActivity = System.currentTimeMillis()
            )
            userDao.updateUser(updated)
            syncUserToServer(updated)
        }
    }

    override suspend fun buyStreakFreeze(userId: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val cost = 500
        if (userEntity.xp >= cost) {
            val updated = userEntity.copy(
                xp = userEntity.xp - cost,
                streakFreezes = userEntity.streakFreezes + 1
            )
            userDao.updateUser(updated)
            syncUserToServer(updated)
        }
    }

    override fun updateUser(displayName: String?, bio: String?, location: String?, avatarUrl: String?, bannerUrl: String?, isArtist: Boolean?): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.updateUser(UpdateUserRequest(displayName, bio, location, avatarUrl, bannerUrl, isArtist))
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                syncUserLocally(user)
                emit(Resource.Success(user))
            } else {
                emit(Resource.Error("Failed to update profile"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun forgotPassword(emailOrUsername: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = ApiClient.authApi.forgotPassword(ForgotPasswordRequest(emailOrUsername))
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("User not found or request failed"))
        } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) }
    }

    override fun deleteAccount(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.deleteAccount()
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Failed to delete account"))
        } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) }
    }

    override fun login(username: String, password: String): Flow<Resource<com.example.firstapp.common.data.api.AuthResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = withTimeout(15000) {
                ApiClient.authApi.login(com.example.firstapp.common.data.api.LoginRequest(username, password))
            }
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                auth.user?.let { syncUserLocally(it) }
                // Award R0.10 for logging in
                try {
                    com.example.firstapp.chat.rewards.data.RewardsManager.awardPoints(0.10, "LOGIN", "Daily login reward")
                } catch (_: Exception) {}

                emit(Resource.Success(auth))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed"
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun register(username: String, email: String, displayName: String, phoneNumber: String, password: String): Flow<Resource<com.example.firstapp.common.data.api.AuthResponse>> = flow {
        emit(Resource.Loading())
        try {
            val response = withTimeout(15000) {
                ApiClient.authApi.register(com.example.firstapp.common.data.api.RegisterRequest(username, email, displayName, phoneNumber, password))
            }
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                auth.user?.let { syncUserLocally(it) }
                emit(Resource.Success(auth))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun requestOTP(phoneNumber: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        
        // TEST BYPASS: Skip SMS for test numbers or numbers ending in 0000
        if (phoneNumber.endsWith("0000") || phoneNumber == "+27710000000") {
            emit(Resource.Success("TEST MODE: Use 123456"))
            return@flow
        }

        try {
            val response = withTimeout(15000) {
                ApiClient.authApi.requestOTP(mapOf("phone_number" to phoneNumber))
            }
            if (response.isSuccessful) {
                val msg = response.body()?.code?.let { "DEBUG OTP: $it" } ?: "Verification code requested"
                emit(Resource.Success(msg))
            } else {
                emit(Resource.Error("Failed to request OTP"))
            }
        } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) }
    }

    override fun verifyOTP(phoneNumber: String, code: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        
        // TEST BYPASS: If code is "123456", succeed immediately without network call
        if (code == "123456") {
            emit(Resource.Success(true))
            return@flow
        }

        try {
            val response = withTimeout(15000) {
                ApiClient.authApi.verifyOTP(mapOf("phone_number" to phoneNumber, "code" to code))
            }
            if (response.isSuccessful) emit(Resource.Success(true))
            else emit(Resource.Error("Invalid code"))
        } catch (e: Exception) { emit(Resource.Error(e.toUserMessage())) }
    }

    override fun getWorkExperiences(): Flow<Resource<List<WorkExperience>>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.getWorkExperiences()
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                emit(Resource.Error("Failed to fetch experiences"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun addWorkExperience(experience: WorkExperience): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.addWorkExperience(experience)
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!.id))
            } else {
                emit(Resource.Error("Failed to add experience"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun updateWorkExperience(experience: WorkExperience): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.updateWorkExperience(experience.id!!, experience)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to update experience"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    override fun deleteWorkExperience(experienceId: String): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.deleteWorkExperience(experienceId)
            if (response.isSuccessful) {
                emit(Resource.Success(true))
            } else {
                emit(Resource.Error("Failed to delete experience"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is ConnectException,
            is UnknownHostException,
            is SocketTimeoutException,
            is TimeoutCancellationException -> "Can't reach the server. Check your connection."
            is HttpException -> "Server error (${code()}). Please try again."
            else -> localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Please try again."
        }
    }
}
