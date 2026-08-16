package com.example.firstapp.chat.ui.viewmodel

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.chat.model.RewardDashboard
import com.example.firstapp.chat.model.RewardTask
import com.example.firstapp.chat.rewards.data.api.RewardsApi
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.data.ads.RewardedAdPoolManager
import com.example.firstapp.common.util.Resource
import com.example.firstapp.vocab.domain.repository.UserRepository
import com.example.firstapp.common.data.ads.AdUnitConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class RewardUiState {
    object Loading : RewardUiState()
    data class Success(val dashboard: RewardDashboard) : RewardUiState()
    data class Error(val message: String) : RewardUiState()
}

@HiltViewModel
class RewardViewModel @Inject constructor(
    private val rewardsApi: RewardsApi,
    private val userRepository: UserRepository,
    private val poolManager: RewardedAdPoolManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<RewardUiState>(RewardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _cooldownSeconds = MutableStateFlow(0)
    val cooldownSeconds = _cooldownSeconds.asStateFlow()

    private val prefs: SharedPreferences = context.getSharedPreferences("reward_prefs", Context.MODE_PRIVATE)
    private var timerJob: Job? = null

    init {
        fetchDashboard()
        checkStoredCooldown()
        initializePool()
    }

    private fun initializePool() {
        viewModelScope.launch {
            val user = userRepository.observeCurrentUser().filterNotNull().first()
            poolManager.initialize(context, AdUnitConfig.REWARDED_POINTS_ID, user.id)
        }
    }

    fun fetchDashboard() {
        viewModelScope.launch {
            _uiState.value = RewardUiState.Loading
            try {
                val response = rewardsApi.getDashboard()
                if (response.isSuccessful && response.body() != null) {
                    val dash = response.body()!!
                    _uiState.value = RewardUiState.Success(dash)
                    
                    if (dash.cooldown_remaining_seconds > 0) {
                        startCooldownTimer(dash.cooldown_remaining_seconds)
                    }
                } else {
                    _uiState.value = RewardUiState.Error("Failed to load rewards")
                }
            } catch (e: Exception) {
                _uiState.value = RewardUiState.Error(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    private fun checkStoredCooldown() {
        val targetEpoch = prefs.getLong("target_cooldown_epoch", 0L)
        val now = System.currentTimeMillis()
        if (targetEpoch > now) {
            val remaining = ((targetEpoch - now) / 1000).toInt()
            startCooldownTimer(remaining)
        }
    }

    private fun startCooldownTimer(seconds: Int) {
        timerJob?.cancel()
        _cooldownSeconds.value = seconds
        
        // Persist target epoch
        val targetEpoch = System.currentTimeMillis() + (seconds * 1000L)
        prefs.edit().putLong("target_cooldown_epoch", targetEpoch).apply()

        timerJob = viewModelScope.launch {
            var current = seconds
            while (current > 0) {
                delay(1000)
                current--
                _cooldownSeconds.value = current
            }
            // Cleanup persistence when done
            prefs.edit().remove("target_cooldown_epoch").apply()
        }
    }

    fun watchAd(activity: Activity, adUnitId: String, taskId: String) {
        viewModelScope.launch {
            val user = userRepository.observeCurrentUser().firstOrNull() ?: return@launch
            
            poolManager.showAd(
                activity = activity,
                adUnitId = adUnitId,
                userId = user.id,
                taskId = taskId,
                onRewardEarned = {
                    handleAdCompleted(taskId)
                }
            )
        }
    }

    private fun handleAdCompleted(taskId: String) {
        // 1. Optimistic UI Update
        val currentState = _uiState.value
        if (currentState is RewardUiState.Success) {
            val updatedTasks = currentState.dashboard.tasks.map {
                if (it.id == taskId) it.copy(is_completed = true) else it
            }
            val updatedDash = currentState.dashboard.copy(
                total_points = currentState.dashboard.total_points + 10,
                ads_watched_today = currentState.dashboard.ads_watched_today + 1,
                tasks = updatedTasks
            )
            _uiState.value = RewardUiState.Success(updatedDash)
        }

        // 2. Start 120s cooldown
        startCooldownTimer(120)

        // 3. Background Sync
        viewModelScope.launch {
            delay(2000) // Small delay to let SSV process
            fetchDashboard()
            // Also refresh user profile for SSOT points
            userRepository.getCurrentUser().collectLatest { }
        }
    }
}
