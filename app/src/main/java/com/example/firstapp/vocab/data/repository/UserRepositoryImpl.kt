package com.example.firstapp.vocab.data.repository

import com.example.firstapp.vocab.data.local.dao.UserDao
import com.example.firstapp.vocab.data.local.dao.LessonProgressDao
import com.example.firstapp.vocab.data.local.entity.LessonProgressEntity
import com.example.firstapp.vocab.data.local.entity.UserEntity
import com.example.firstapp.common.model.User
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.data.api.ApiClient
import com.example.firstapp.common.data.api.UpdateUserRequest
import com.example.firstapp.common.data.api.ForgotPasswordRequest
import com.example.firstapp.common.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Calendar

import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val lessonProgressDao: LessonProgressDao
) : UserRepository {

    private val apiService = ApiClient.userApi

    override fun getUserData(userId: Int): Flow<User?> {
        val userFlow = userDao.getUser(userId)
        val progressFlow = lessonProgressDao.getAllProgress(userId)

        return combine(userFlow, progressFlow) { entity, progressList ->
            entity?.let {
                User(
                    id = it.id,
                    username = it.username,
                    email = it.email,
                    display_name = it.display_name,
                    phone_number = it.phone_number,
                    created_at = it.created_at,
                    avatar_url = it.avatar_url,
                    bio = it.bio,
                    location = it.location,
                    is_verified = it.is_verified,
                    xp = it.xp,
                    level = it.level,
                    streak = it.streak,
                    isPremium = it.isPremium,
                    lastDailyUnlock = it.lastDailyUnlock,
                    adsWatchedToday = it.adsWatchedToday,
                    unlockedLessonCount = it.unlockedLessonCount,
                    nativeLanguage = it.nativeLanguage,
                    targetLanguage = it.targetLanguage,
                    streakFreezes = it.streakFreezes,
                    lastStreakActivity = it.lastStreakActivity,
                    walletBalance = it.walletBalance,
                    banner_url = it.bannerUrl,
                    isArtist = it.isArtist,
                    jobFullName = it.jobFullName,
                    jobEmail = it.jobEmail,
                    jobPhone = it.jobPhone,
                    jobLocation = it.jobLocation,
                    jobEducation = it.jobEducation,
                    jobExperience = it.jobExperience,
                    jobSkills = it.jobSkills,
                    jobAboutMe = it.jobAboutMe,
                    jobLinkedinUrl = it.jobLinkedinUrl,
                    jobPortfolioUrl = it.jobPortfolioUrl,
                    jobWorkPreference = it.jobWorkPreference,
                    jobAvailability = it.jobAvailability,
                    jobExpectedSalary = it.jobExpectedSalary,
                    completedLessonIds = progressList.filter { p -> p.isCompleted }.map { p -> p.lessonId },
                    lessonMastery = progressList.associate { p -> p.lessonId to p.score }
                )
            }
        }
    }

    override fun observeCurrentUser(): Flow<User?> {
        return userDao.getCurrentUser().map { entity ->
            entity?.let {
                User(
                    id = it.id,
                    username = it.username,
                    email = it.email,
                    display_name = it.display_name,
                    phone_number = it.phone_number,
                    created_at = it.created_at,
                    avatar_url = it.avatar_url,
                    bio = it.bio,
                    location = it.location,
                    is_verified = it.is_verified,
                    xp = it.xp,
                    level = it.level,
                    streak = it.streak,
                    isPremium = it.isPremium,
                    adsWatchedToday = it.adsWatchedToday,
                    walletBalance = it.walletBalance,
                    banner_url = it.bannerUrl,
                    isArtist = it.isArtist,
                    jobFullName = it.jobFullName,
                    jobEmail = it.jobEmail,
                    jobPhone = it.jobPhone,
                    jobLocation = it.jobLocation,
                    jobEducation = it.jobEducation,
                    jobExperience = it.jobExperience,
                    jobSkills = it.jobSkills,
                    jobAboutMe = it.jobAboutMe,
                    jobLinkedinUrl = it.jobLinkedinUrl,
                    jobPortfolioUrl = it.jobPortfolioUrl,
                    jobWorkPreference = it.jobWorkPreference,
                    jobAvailability = it.jobAvailability,
                    jobExpectedSalary = it.jobExpectedSalary
                )
            }
        }
    }

    override fun getCurrentUser(): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        
        // Try to get cached user first
        userDao.getCurrentUser().firstOrNull()?.let { cached ->
            val model = User(
                id = cached.id,
                username = cached.username,
                email = cached.email,
                display_name = cached.display_name,
                phone_number = cached.phone_number,
                created_at = cached.created_at,
                avatar_url = cached.avatar_url,
                bio = cached.bio,
                location = cached.location,
                is_verified = cached.is_verified,
                xp = cached.xp,
                level = cached.level,
                streak = cached.streak,
                isPremium = cached.isPremium,
                adsWatchedToday = cached.adsWatchedToday,
                walletBalance = cached.walletBalance,
                banner_url = cached.bannerUrl,
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
            display_name = user.display_name,
            phone_number = user.phone_number,
            created_at = user.created_at,
            avatar_url = user.avatar_url,
            bio = user.bio,
            location = user.location,
            is_verified = user.is_verified,
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
            bannerUrl = user.banner_url,
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

    override suspend fun ensureUserExists(userId: Int) {
        val existingUser = userDao.getUser(userId).firstOrNull()
        if (existingUser == null) {
            val newUser = UserEntity(
                id = userId,
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

    override suspend fun setNativeLanguage(userId: Int, language: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(nativeLanguage = language)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun setTargetLanguage(userId: Int, language: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(targetLanguage = language)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun updateUserXp(userId: Int, xpDelta: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val newXp = userEntity.xp + xpDelta
        val newLevel = (newXp / 100) + 1
        val updated = userEntity.copy(xp = newXp, level = newLevel)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun recordLessonResult(userId: Int, lessonId: String, score: Int, maxScore: Int) {
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

    private suspend fun updateStreak(userId: Int, timestamp: Long) {
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

    override suspend fun incrementAdsWatched(userId: Int) {
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

    override suspend fun resetDailyAds(userId: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(adsWatchedToday = 0)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun setPremiumStatus(userId: Int, isPremium: Boolean) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val updated = userEntity.copy(isPremium = isPremium)
        userDao.updateUser(updated)
        syncUserToServer(updated)
    }

    override suspend fun useStreakFreeze(userId: Int) {
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

    override suspend fun buyStreakFreeze(userId: Int) {
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
            val response = ApiClient.authApi.login(com.example.firstapp.common.data.api.LoginRequest(username, password))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
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
            val response = ApiClient.authApi.register(com.example.firstapp.common.data.api.RegisterRequest(username, email, displayName, phoneNumber, password))
            if (response.isSuccessful && response.body() != null) {
                emit(Resource.Success(response.body()!!))
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                emit(Resource.Error(errorMsg))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.toUserMessage()))
        }
    }

    private fun Throwable.toUserMessage(): String {
        return when (this) {
            is ConnectException,
            is UnknownHostException,
            is SocketTimeoutException -> "Can't reach the server. Check your connection."
            is HttpException -> "Server error (${code()}). Please try again."
            else -> localizedMessage?.takeIf { it.isNotBlank() }
                ?: "Something went wrong. Please try again."
        }
    }
}
