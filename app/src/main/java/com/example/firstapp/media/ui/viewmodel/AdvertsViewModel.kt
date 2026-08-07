package com.example.firstapp.media.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.data.ads.RewardedAdPoolManager
import com.example.firstapp.common.util.Resource
import com.example.firstapp.media.domain.repository.AdRepository
import com.example.firstapp.media.model.Advert
import com.example.firstapp.vocab.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class AdvertsViewModel @Inject constructor(
    private val repository: AdRepository,
    private val userRepository: UserRepository,
    val adManager: AdManager,
    val poolManager: RewardedAdPoolManager,
) : ViewModel() {

    private val refreshTrigger = MutableSharedFlow<Unit>(replay = 1)
    
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage = _toastMessage.asStateFlow()

    val hasRewardedAdsAvailable: StateFlow<Boolean> = poolManager.hasAvailableAds

    val adverts: StateFlow<Resource<List<Advert>>> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { 
            repository.getAdverts() 
        }
        .onEach { res -> 
            if (res is Resource.Error) {
                _toastMessage.value = res.message ?: "Failed to fetch ads"
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    val viewedAdverts: StateFlow<Resource<List<Advert>>> = refreshTrigger
        .onStart { emit(Unit) }
        .flatMapLatest { repository.getViewedAdverts() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

    val points: StateFlow<Double> = userRepository.observeCurrentUser()
        .map { it?.walletBalance ?: 0.0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun initializePool(context: Context, adUnitId: String) {
        viewModelScope.launch {
            userRepository.observeCurrentUser()
                .filterNotNull()
                .first()
                .let { user ->
                    poolManager.initialize(context, adUnitId, user.id)
                }
        }
    }

    fun showRewardedAd(activity: Activity, adUnitId: String, advertId: String) {
        viewModelScope.launch {
            val user = userRepository.observeCurrentUser().firstOrNull()
            if (user != null) {
                poolManager.showAd(
                    activity = activity,
                    adUnitId = adUnitId,
                    userId = user.id,
                    onRewardEarned = {
                        markAdvertAsViewed(advertId)
                    }
                )
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }

    fun fetchAdverts() {
        viewModelScope.launch { 
            refreshTrigger.emit(Unit) 
            userRepository.getCurrentUser().collectLatest { } // Force user refresh
        }
    }

    fun resetAds() {
        viewModelScope.launch {
            repository.resetAds().collect { result ->
                if (result is Resource.Success) {
                    fetchAdverts()
                }
            }
        }
    }

    fun markAdvertAsViewed(advertId: String) {
        viewModelScope.launch {
            repository.markAdvertViewed(advertId).collect { result ->
                if (result is Resource.Success && (result.data == true)) {
                    // Give the backend SSV callback a moment to process (usually < 2s)
                    kotlinx.coroutines.delay(2000)
                    
                    // Authoritatively refresh balance from server
                    com.example.firstapp.chat.rewards.data.RewardsManager.refreshBalance()
                    
                    // Also refresh user profile
                    userRepository.getCurrentUser().collectLatest { }
                }
            }
        }
    }
}
