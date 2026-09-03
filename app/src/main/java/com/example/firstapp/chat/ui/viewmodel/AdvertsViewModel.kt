package com.example.firstapp.chat.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firstapp.common.data.ads.AdManager
import com.example.firstapp.common.data.ads.RewardedAdPoolManager
import com.example.firstapp.common.util.Resource
import com.example.firstapp.chat.domain.repository.AdRepository
import com.example.firstapp.chat.model.Advert
import com.example.firstapp.chat.rewards.data.RewardsManager
import com.example.firstapp.vocab.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
    val cooldownRemainingSeconds: StateFlow<Int> = poolManager.cooldownRemainingSeconds

    val adverts: StateFlow<Resource<List<Advert>>> = combine(
        refreshTrigger.onStart { emit(Unit) }.flatMapLatest { repository.getAdverts() },
        refreshTrigger.onStart { emit(Unit) }.flatMapLatest { repository.getViewedAdverts() }
    ) { availableRes, viewedRes ->
        val available = availableRes.data ?: emptyList()
        val viewed = viewedRes.data ?: emptyList()
        val totalExisting = available.size + viewed.size
        
        val resultList = available.toMutableList()
        
        // If total (available + viewed) < 30, add virtual slots
        if (totalExisting < 30) {
            val needed = 30 - totalExisting
            for (i in 1..needed) {
                resultList.add(
                    Advert(
                        id = "virtual_ad_$i", // Stable ID to prevent list flickering
                        advertiserName = "Rewards",
                        title = "Extra Reward Slot #${totalExisting + i}",
                        actionText = "Watch",
                        avatarColor = 0xFFCCCCCC.toInt(),
                        contentColor = 0xFFEEEEEE.toInt(),
                        isViewed = false
                    )
                )
            }
        }

        when (availableRes) {
            is Resource.Success -> Resource.Success(resultList)
            is Resource.Error -> Resource.Error(availableRes.message ?: "Error", resultList)
            is Resource.Loading -> Resource.Loading(resultList)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Resource.Loading())

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
                        viewModelScope.launch {
                            // AdMob SSV is asynchronous; refresh the profile after the
                            // callback has had time to update the authoritative balance.
                            delay(5000)
                            userRepository.getCurrentUser().collectLatest { }
                        }
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
            // Optimistically award points for better UX
            RewardsManager.addOptimisticReward(10.0)

            val isVirtual = advertId.startsWith("virtual_ad_")
            
            if (isVirtual) {
                // Virtual slots are local placeholders and cannot produce a valid AdMob SSV reward.
                return@launch
            } else {
                repository.markAdvertViewed(advertId).collect { result ->
                    if (result is Resource.Success && result.data == true) fetchAdverts()
                }
            }
        }
    }
}
