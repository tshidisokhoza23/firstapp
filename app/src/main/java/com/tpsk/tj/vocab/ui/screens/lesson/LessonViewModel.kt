package com.tpsk.tj.vocab.ui.screens.lesson

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tpsk.tj.vocab.model.Lesson
import com.tpsk.tj.vocab.model.LessonContent
import com.tpsk.tj.vocab.model.ContentType
import com.tpsk.tj.vocab.domain.repository.LessonRepository
import com.tpsk.tj.vocab.domain.repository.UserRepository
import com.tpsk.tj.common.model.User
import com.tpsk.tj.vocab.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    fun loadLesson(lessonId: String) {
        if (lessonId.isBlank()) {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        viewModelScope.launch {
            try {
                val lesson = lessonRepository.getLessonById(lessonId)
                val user = userRepository.getUserData(Constants.DEFAULT_USER_ID).firstOrNull()
                _uiState.value = _uiState.value.copy(
                    lesson = lesson, 
                    user = user,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun onAnswerSelected(index: Int) {
        val currentState = _uiState.value
        if (currentState.showFeedback) return 
        
        val lesson = currentState.lesson ?: return
        val currentContent = lesson.content.getOrNull(currentState.currentStep) ?: return
        
        val isCorrect = currentContent.correctAnswerIndex == index
        
        _uiState.value = currentState.copy(
            selectedAnswerIndex = index,
            showFeedback = true,
            isCorrect = isCorrect,
            score = if (isCorrect) currentState.score + 10 else currentState.score,
            missedPrompts = if (!isCorrect) {
                val prompt = currentContent.translations["English"] ?: ""
                (currentState.missedPrompts + prompt).distinct()
            } else {
                currentState.missedPrompts
            },
            reviewItems = if (!isCorrect) {
                (currentState.reviewItems + currentContent).distinctBy { it.translations["English"] }
            } else {
                currentState.reviewItems
            }
        )
    }

    fun onNextClicked() {
        if (!canGoNext()) return
        _uiState.value = _uiState.value.copy(
            speechFeedback = null,
            speechError = null
        )
        nextStep()
    }

    fun onBackClicked() {
        val currentState = _uiState.value
        if (currentState.currentStep > 0) {
            _uiState.value = currentState.copy(
                currentStep = currentState.currentStep - 1,
                selectedAnswerIndex = null,
                showFeedback = false,
                isCorrect = false,
                speechFeedback = null,
                speechError = null
            )
        }
    }

    fun canGoBack(): Boolean = _uiState.value.currentStep > 0

    fun canGoNext(): Boolean {
        val currentState = _uiState.value
        val currentContent = currentContent(currentState) ?: return false
        return currentContent.type == ContentType.TEACH || 
               currentContent.type == ContentType.EXAMPLE || 
               currentState.showFeedback
    }

    fun retryLesson() {
        _uiState.value = _uiState.value.copy(
            currentStep = 0,
            selectedAnswerIndex = null,
            showFeedback = false,
            isCorrect = false,
            score = 0,
            isFinished = false,
            missedPrompts = emptyList(),
            speechFeedback = null,
            speechError = null
        )
    }

    fun nextStep() {
        val currentState = _uiState.value
        val nextStep = currentState.currentStep + 1
        val lesson = currentState.lesson ?: return
        
        if (nextStep < lesson.content.size) {
            _uiState.value = currentState.copy(
                currentStep = nextStep,
                selectedAnswerIndex = null,
                showFeedback = false,
                isCorrect = false
            )
        } else {
            if (currentState.reviewItems.isNotEmpty()) {
                val updatedLesson = lesson.copy(
                    content = lesson.content + currentState.reviewItems.map { it.copy(explanation = "Let's try this one again: " + it.explanation) }
                )
                _uiState.value = currentState.copy(
                    lesson = updatedLesson,
                    currentStep = nextStep,
                    reviewItems = emptyList(),
                    selectedAnswerIndex = null,
                    showFeedback = false,
                    isCorrect = false
                )
            } else {
                _uiState.value = currentState.copy(isFinished = true)
                saveProgress()
            }
        }
    }

    private fun saveProgress() {
        viewModelScope.launch {
            val lessonId = _uiState.value.lesson?.id ?: return@launch
            val score = _uiState.value.score
            val maxScore = _uiState.value.lesson
                ?.content
                ?.count { it.type == ContentType.QUESTION || it.type == ContentType.SPEAKING }
                ?.times(10)
                ?: 0
            
            lessonRepository.completeLesson(Constants.DEFAULT_USER_ID, lessonId, score)
            userRepository.recordLessonResult(Constants.DEFAULT_USER_ID, lessonId, score, maxScore)
            
            if (score == maxScore && maxScore > 0) {
                userRepository.updateUserXp(Constants.DEFAULT_USER_ID, score)
                userRepository.resetDailyAds(Constants.DEFAULT_USER_ID)
            }
        }
    }

    private fun currentContent(state: LessonUiState): LessonContent? {
        val lesson = state.lesson ?: return null
        return lesson.content.getOrNull(state.currentStep)
    }
}

data class LessonUiState(
    val lesson: Lesson? = null,
    val user: User? = null,
    val isLoading: Boolean = true,
    val currentStep: Int = 0,
    val selectedAnswerIndex: Int? = null,
    val showFeedback: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Int = 0,
    val isFinished: Boolean = false,
    val missedPrompts: List<String> = emptyList(),
    val speechFeedback: String? = null,
    val speechError: String? = null,
    val reviewItems: List<LessonContent> = emptyList()
)
