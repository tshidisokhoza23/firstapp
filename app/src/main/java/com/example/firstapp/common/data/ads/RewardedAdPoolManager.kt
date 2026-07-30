package com.example.firstapp.common.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewarded.ServerSideVerificationOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.ArrayDeque
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Production-ready Ad Pool Manager.
 * Handles thread-safe queuing, TTL expiration, SSV, and automatic background refilling.
 */
@Singleton
class RewardedAdPoolManager @Inject constructor() {

    companion object {
        private const val TAG = "RewardedAdPool"
        private const val MAX_CAPACITY = 3
        private const val AD_TTL_MS = 45 * 60 * 1000L // 45 Minutes
        private const val INITIAL_BACKOFF_DELAY_MS = 2000L
        private const val MAX_BACKOFF_DELAY_MS = 64000L
    }

    private data class CachedAd(
        val ad: RewardedAd,
        val loadedTimestamp: Long = System.currentTimeMillis(),
    ) {
        fun isExpired(): Boolean = (System.currentTimeMillis() - loadedTimestamp) > AD_TTL_MS
    }

    private val pool = ArrayDeque<CachedAd>()
    private val poolMutex = Mutex()
    
    private val _hasAvailableAds = MutableStateFlow(false)
    val hasAvailableAds = _hasAvailableAds.asStateFlow()

    private var isRefilling = false
    private var isShowingAd = false
    private var retryAttempt = 0
    
    private val managerScope = MainScope()

    init {
        startPeriodicCleanup()
    }

    /**
     * Entry point to ensure the pool is healthy.
     */
    fun initialize(context: Context, adUnitId: String, userId: String) {
        managerScope.launch {
            refillPool(context.applicationContext, adUnitId, userId)
        }
    }

    private suspend fun refillPool(context: Context, adUnitId: String, userId: String) {
        poolMutex.withLock {
            if (isRefilling || pool.size >= MAX_CAPACITY) return
            isRefilling = true
        }

        Log.d(TAG, "Triggering refill. Current pool size: ${pool.size}")
        loadAdInternal(context, adUnitId, userId)
    }

    private fun loadAdInternal(context: Context, adUnitId: String, userId: String) {
        val adRequest = AdRequest.Builder().build()
        
        // Setup Server-Side Verification (SSV)
        val ssvOptions = ServerSideVerificationOptions.Builder()
            .setUserId(userId)
            .setCustomData("reward_v1_${System.currentTimeMillis()}")
            .build()

        RewardedAd.load(
            context, 
            adUnitId, 
            adRequest, 
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    rewardedAd.setServerSideVerificationOptions(ssvOptions)
                    
                    managerScope.launch {
                        poolMutex.withLock {
                            pool.add(CachedAd(rewardedAd))
                            isRefilling = false
                            retryAttempt = 0 // Reset backoff
                            updateAvailability()
                        }
                        Log.d(TAG, "Ad preloaded successfully. Pool size: ${pool.size}")
                        
                        // Recursive refill if still below capacity
                        if (pool.size < MAX_CAPACITY) {
                            refillPool(context, adUnitId, userId)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    managerScope.launch {
                        poolMutex.withLock { isRefilling = false }
                        
                        // Exponential Backoff Strategy
                        val base = 2.0
                        val rawDelay = INITIAL_BACKOFF_DELAY_MS * base.pow(retryAttempt.toDouble())
                        val delayMs = rawDelay.toLong().coerceAtMost(MAX_BACKOFF_DELAY_MS)
                        
                        Log.e(TAG, "Ad failed to load: ${error.message}. Retrying in ${delayMs}ms (Attempt $retryAttempt)")
                        retryAttempt++
                        
                        delay(delayMs)
                        refillPool(context, adUnitId, userId)
                    }
                }
            }
        )
    }

    /**
     * Consumes an unexpired ad from the pool and displays it.
     */
    fun showAd(
        activity: Activity,
        adUnitId: String,
        userId: String,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        managerScope.launch {
            if (isShowingAd) {
                Log.w(TAG, "showAd requested while an ad is already showing. Ignoring.")
                return@launch
            }

            val cachedAd = getNextValidAd()
            
            if (cachedAd == null) {
                Log.w(TAG, "No ads available in pool to show.")
                refillPool(activity.applicationContext, adUnitId, userId)
                onAdClosed()
                return@launch
            }

            isShowingAd = true
            cachedAd.ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isShowingAd = false
                    Log.d(TAG, "Ad dismissed. Triggering background refill.")
                    managerScope.launch { refillPool(activity.applicationContext, adUnitId, userId) }
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(err: com.google.android.gms.ads.AdError) {
                    isShowingAd = false
                    Log.e(TAG, "Failed to show ad: ${err.message}")
                    managerScope.launch { refillPool(activity.applicationContext, adUnitId, userId) }
                    onAdClosed()
                }
            }

            cachedAd.ad.show(activity) {
                Log.i(TAG, "User earned reward!")
                onRewardEarned()
            }
        }
    }

    private suspend fun getNextValidAd(): CachedAd? = poolMutex.withLock {
        while (pool.size > 0) {
            val candidate = pool.poll()
            if (candidate != null && !candidate.isExpired()) {
                updateAvailability()
                return@withLock candidate
            } else {
                Log.i(TAG, "Evicting expired or null ad from pool.")
            }
        }
        updateAvailability()
        return@withLock null
    }

    private fun updateAvailability() {
        _hasAvailableAds.value = pool.size > 0
    }

    private fun startPeriodicCleanup() {
        managerScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // Every 5 minutes
                poolMutex.withLock {
                    val initialSize = pool.size
                    // java.util.ArrayDeque.removeIf is available since API 24
                    pool.removeIf { it.isExpired() }
                    if (pool.size != initialSize) {
                        Log.d(TAG, "Periodic cleanup evicted ${initialSize - pool.size} expired ads.")
                        updateAvailability()
                    }
                }
            }
        }
    }
}
