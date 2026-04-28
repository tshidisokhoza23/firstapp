package com.example.firstapp.vocab.data.repository

import com.example.firstapp.vocab.data.local.dao.UserDao
import com.example.firstapp.vocab.data.local.dao.LessonProgressDao
import com.example.firstapp.vocab.data.local.entity.LessonProgressEntity
import com.example.firstapp.vocab.data.local.entity.UserEntity
import com.example.firstapp.model.User
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.data.api.ApiClient
import com.example.firstapp.data.api.UpdateUserRequest
import com.example.firstapp.data.api.ForgotPasswordRequest
import com.example.firstapp.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.Calendar

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val lessonProgressDao: LessonProgressDao
) : UserRepository {

    private val apiService = ApiClient.apiService

    override fun getUserData(userId: Int): Flow<User?> {
        val userFlow = userDao.getUser(userId)
        val progressFlow = lessonProgressDao.getAllProgress(userId)

        return combine(userFlow, progressFlow) { entity, progressList ->
            entity?.let {
                User(
                    id = it.id,
                    username = it.username,
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
                    completedLessonIds = progressList.filter { p -> p.isCompleted }.map { p -> p.lessonId },
                    lessonMastery = progressList.associate { p -> p.lessonId to p.score }
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
                isPremium = cached.isPremium
            )
            emit(Resource.Success(model))
        }

        try {
            val response = apiService.getCurrentUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!.user
                syncUserLocally(user)
                emit(Resource.Success(user))
            }
        } catch (e: Exception) {
            // Already emitted cached
        }
    }

    private suspend fun syncUserLocally(user: User) {
        val existing = userDao.getUser(user.id).firstOrNull()
        val entity = UserEntity(
            id = user.id,
            username = user.username,
            display_name = user.display_name,
            phone_number = user.phone_number,
            created_at = user.created_at,
            avatar_url = user.avatar_url,
            bio = user.bio,
            location = user.location,
            is_verified = user.is_verified,
            xp = existing?.xp ?: user.xp,
            level = existing?.level ?: user.level,
            streak = existing?.streak ?: user.streak,
            isPremium = existing?.isPremium ?: user.isPremium,
            lastDailyUnlock = existing?.lastDailyUnlock ?: user.lastDailyUnlock,
            adsWatchedToday = existing?.adsWatchedToday ?: user.adsWatchedToday,
            unlockedLessonCount = existing?.unlockedLessonCount ?: user.unlockedLessonCount,
            nativeLanguage = existing?.nativeLanguage ?: user.nativeLanguage,
            targetLanguage = existing?.targetLanguage ?: user.targetLanguage,
            streakFreezes = existing?.streakFreezes ?: user.streakFreezes,
            lastStreakActivity = existing?.lastStreakActivity ?: user.lastStreakActivity
        )
        userDao.insertUser(entity)
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
                adsWatchedToday = 0
            )
            userDao.insertUser(newUser)
        }
    }

    override suspend fun setNativeLanguage(userId: Int, language: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        userDao.updateUser(userEntity.copy(nativeLanguage = language))
    }

    override suspend fun setTargetLanguage(userId: Int, language: String) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        userDao.updateUser(userEntity.copy(targetLanguage = language))
    }

    override suspend fun updateUserXp(userId: Int, xpDelta: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val newXp = userEntity.xp + xpDelta
        val newLevel = (newXp / 100) + 1
        userDao.updateUser(userEntity.copy(xp = newXp, level = newLevel))
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

            userDao.updateUser(userEntity.copy(
                streak = newStreak,
                lastDailyUnlock = timestamp,
                streakFreezes = if (usedFreeze) userEntity.streakFreezes - 1 else userEntity.streakFreezes
            ))
        }
    }

    override suspend fun incrementAdsWatched(userId: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val newAdsWatched = userEntity.adsWatchedToday + 1
        val shouldUnlock = newAdsWatched >= 2
        
        userDao.updateUser(userEntity.copy(
            adsWatchedToday = newAdsWatched,
            unlockedLessonCount = if (shouldUnlock) userEntity.unlockedLessonCount + 1 else userEntity.unlockedLessonCount
        ))
    }

    override suspend fun resetDailyAds(userId: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        userDao.updateUser(userEntity.copy(adsWatchedToday = 0))
    }

    override suspend fun setPremiumStatus(userId: Int, isPremium: Boolean) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        userDao.updateUser(userEntity.copy(isPremium = isPremium))
    }

    override suspend fun useStreakFreeze(userId: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        if (userEntity.streakFreezes > 0) {
            userDao.updateUser(userEntity.copy(
                streakFreezes = userEntity.streakFreezes - 1,
                lastStreakActivity = System.currentTimeMillis()
            ))
        }
    }

    override suspend fun buyStreakFreeze(userId: Int) {
        val userEntity = userDao.getUser(userId).firstOrNull() ?: return
        val cost = 500
        if (userEntity.xp >= cost) {
            userDao.updateUser(userEntity.copy(
                xp = userEntity.xp - cost,
                streakFreezes = userEntity.streakFreezes + 1
            ))
        }
    }

    override fun updateUser(displayName: String?, bio: String?, location: String?, avatarUrl: String?): Flow<Resource<User>> = flow {
        emit(Resource.Loading())
        try {
            val response = apiService.updateUser(UpdateUserRequest(displayName, bio, location, avatarUrl))
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
            val response = apiService.forgotPassword(ForgotPasswordRequest(emailOrUsername))
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
