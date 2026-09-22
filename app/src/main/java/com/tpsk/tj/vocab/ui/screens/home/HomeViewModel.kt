package com.tpsk.tj.vocab.ui.screens.home

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.vocab.model.Lesson
import com.tpsk.tj.common.model.User
import com.tpsk.tj.vocab.domain.repository.LessonRepository
import com.tpsk.tj.vocab.domain.repository.UserRepository
import com.tpsk.tj.vocab.utils.Constants
import com.tpsk.tj.common.data.ads.AdManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val lessonRepository: LessonRepository,
    private val adManager: AdManager
) : ViewModel() {

    private var isLoadingAd = false

    init {
        viewModelScope.launch {
            userRepository.ensureUserExists(Constants.DEFAULT_USER_ID)
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        userRepository.getUserData(Constants.DEFAULT_USER_ID),
        lessonRepository.getLessons()
    ) { user, lessons ->
        val safeUser = user ?: User(id = Constants.DEFAULT_USER_ID)
        val filteredLessons = lessons.filter { it.language == safeUser.targetLanguage }
        
        val completedCount = filteredLessons.count { lesson ->
            val mastery = safeUser.lessonMastery?.get(lesson.id) ?: 0
            mastery >= 100
        }
        
        val units = filteredLessons.chunked(5).mapIndexed { index, unitLessons ->
            val unitCompletedCount = unitLessons.count { (safeUser.lessonMastery?.get(it.id) ?: 0) >= 100 }
            val firstLesson = unitLessons.firstOrNull()
            val unitTitle = firstLesson?.categoryTranslations?.get(safeUser.nativeLanguage)
                ?: firstLesson?.categoryTranslations?.get("English")
                ?: "Unit ${index + 1}"

            LessonUnit(
                title = unitTitle,
                lessons = unitLessons,
                isCompleted = unitCompletedCount == unitLessons.size,
                progress = if (unitLessons.isEmpty()) 0f else unitCompletedCount.toFloat() / unitLessons.size
            )
        }

        HomeUiState(
            user = safeUser,
            lessons = filteredLessons,
            unlockedCount = safeUser.unlockedLessonCount,
            completedLessonsCount = completedCount,
            totalLessonsCount = filteredLessons.size,
            dailyXpGoal = 50,
            currentDailyXp = (safeUser.xp % 150).coerceAtMost(50),
            units = units,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState(isLoading = true)
    )

    fun onNativeLanguageSelected(language: String) {
        viewModelScope.launch {
            userRepository.setNativeLanguage(Constants.DEFAULT_USER_ID, language)
        }
    }

    fun onTargetLanguageSelected(language: String) {
        viewModelScope.launch {
            userRepository.setTargetLanguage(Constants.DEFAULT_USER_ID, language)
        }
    }

    fun buyStreakFreeze() {
        viewModelScope.launch {
            userRepository.buyStreakFreeze(Constants.DEFAULT_USER_ID)
        }
    }

    fun onAdWatched() {
        viewModelScope.launch {
            userRepository.incrementAdsWatched(Constants.DEFAULT_USER_ID)
        }
    }

    fun showAd(activity: Activity) {
        if (isLoadingAd) return
        isLoadingAd = true
        
        adManager.loadRewardedAd(
            context = activity,
            adUnitId = "ca-app-pub-3940256099942544/5224354917", // AdMob Rewarded Test ID
            onAdLoaded = { adObject ->
                isLoadingAd = false
                adManager.showRewardedAd(
                    activity = activity,
                    adObject = adObject,
                    onRewardEarned = {
                        onAdWatched()
                    },
                    onAdClosed = {
                        // Ad closed
                    }
                )
            },
            onAdFailed = {
                isLoadingAd = false
                // Handle failure
            }
        )
    }
}

data class HomeUiState(
    val user: User = User(id = Constants.DEFAULT_USER_ID),
    val lessons: List<Lesson> = emptyList(),
    val unlockedCount: Int = 1,
    val completedLessonsCount: Int = 0,
    val totalLessonsCount: Int = 0,
    val dailyXpGoal: Int = 50,
    val currentDailyXp: Int = 0,
    val units: List<LessonUnit> = emptyList(),
    val isLoading: Boolean = false
)

data class LessonUnit(
    val title: String,
    val lessons: List<Lesson>,
    val isCompleted: Boolean,
    val progress: Float
)
