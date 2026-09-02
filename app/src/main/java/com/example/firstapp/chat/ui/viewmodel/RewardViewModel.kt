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
import com.example.firstapp.common.data.realtime.WebSocketManager
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
    private val webSocketManager: WebSocketManager,
    private val sessionManager: com.example.firstapp.common.domain.auth.SessionManager,
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
        observeRealtimeUpdates()
    }

    private fun observeRealtimeUpdates() {
        webSocketManager.events
            .filter { it.event == "REWARD_UPDATE" }
            .onEach {
                android.util.Log.i("RewardViewModel", "Reward update received via WS, refreshing dashboard")
                fetchDashboard()
                userRepository.getCurrentUser().collectLatest { }
            }
            .launchIn(viewModelScope)
    }

    private fun initializePool() {
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId != null) {
                android.util.Log.d("RewardViewModel", "Initializing ad pool for user: $userId")
                poolManager.initialize(context, AdUnitConfig.REWARDED_POINTS_ID, userId)
            } else {
                android.util.Log.w("RewardViewModel", "Cannot initialize pool: No user ID found in session")
            }
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
        android.util.Log.d("RewardViewModel", "watchAd called for task: $taskId")
        viewModelScope.launch {
            val userId = sessionManager.getUserId()
            if (userId == null) {
                android.util.Log.e("RewardViewModel", "Cannot watch ad: No active session")
                _uiState.value = RewardUiState.Error("Please log in again to earn rewards")
                return@launch
            }
            
            // Show loading toast if pool is empty
            if (!poolManager.hasAvailableAds.value) {
                android.widget.Toast.makeText(context, "Loading ad... please wait.", android.widget.Toast.LENGTH_SHORT).show()
            }

            android.util.Log.d("RewardViewModel", "Showing ad for user: $userId...")
            poolManager.showAd(
                activity = activity,
                adUnitId = adUnitId,
                userId = userId,
                taskId = taskId,
                onRewardEarned = {
                    android.util.Log.i("RewardViewModel", "Reward earned for task: $taskId")
                    handleAdCompleted(taskId)
                },
                onAdClosed = {
                    android.util.Log.d("RewardViewModel", "Ad closed/failed for task: $taskId")
                    if (!poolManager.hasAvailableAds.value) {
                        android.widget.Toast.makeText(context, "Ad could not be loaded. Please check your connection and try again.", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    }

    private fun handleAdCompleted(taskId: String) {
        // 1. Optimistic UI Update
        val currentState = _uiState.value
        if (currentState is RewardUiState.Success) {
            val task = currentState.dashboard.tasks.find { it.id == taskId }
            val earnedPoints = task?.points ?: 10.0 // Fallback only if task not found
            
            val updatedTasks = currentState.dashboard.tasks.map {
                if (it.id == taskId) it.copy(is_completed = true) else it
            }
            val updatedDash = currentState.dashboard.copy(
                total_points = currentState.dashboard.total_points + earnedPoints,
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
