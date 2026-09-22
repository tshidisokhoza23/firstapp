package com.tpsk.tj.common.data.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
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
        private const val MAX_CAPACITY = 50
        private const val AD_TTL_MS = 45 * 60 * 1000L // 45 Minutes
        private const val INITIAL_BACKOFF_DELAY_MS = 2000L
        private const val MAX_BACKOFF_DELAY_MS = 64000L
        private const val AD_COOLDOWN_MS = 1800_000L // 30 minutes cooldown (1800s) after 4 ads
        private const val BATCH_LIMIT = 4
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

    private val _cooldownRemainingSeconds = MutableStateFlow(0)
    val cooldownRemainingSeconds = _cooldownRemainingSeconds.asStateFlow()

    private var isRefilling = false
    private var isShowingAd = false
    private var retryAttempt = 0
    private var lastRewardTimestamp = 0L
    private var adBatchCount = 0

    private fun recordAdReward() {
        val elapsed = System.currentTimeMillis() - lastRewardTimestamp
        if (lastRewardTimestamp > 0 && elapsed >= AD_COOLDOWN_MS) {
            adBatchCount = 0
            lastRewardTimestamp = 0L
        }
        adBatchCount++
        if (adBatchCount >= BATCH_LIMIT) {
            lastRewardTimestamp = System.currentTimeMillis()
            adBatchCount = 0
        }
    }
    
    private val managerScope = MainScope()

    init {
        startPeriodicCleanup()
        startCooldownTimer()
    }

    private fun startCooldownTimer() {
        managerScope.launch {
            while (isActive) {
                if (lastRewardTimestamp > 0) {
                    val elapsed = System.currentTimeMillis() - lastRewardTimestamp
                    val remaining = (AD_COOLDOWN_MS - elapsed).coerceAtLeast(0L)
                    _cooldownRemainingSeconds.value = (remaining / 1000).toInt()
                    updateAvailability()
                }
                delay(1000)
            }
        }
    }

    /**
     * Entry point to ensure the pool is healthy.
     */
    fun initialize(context: Context, adUnitId: String, userId: String) {
        Log.i(TAG, "!!! initialize called !!! unitId: $adUnitId, userId: $userId")
        managerScope.launch {
            refillPool(context.applicationContext, adUnitId, userId)
        }
    }

    private suspend fun refillPool(context: Context, adUnitId: String, userId: String) {
        poolMutex.withLock {
            if (isRefilling) {
                Log.d(TAG, "Refill already in progress. skipping.")
                return
            }
            if (pool.size >= MAX_CAPACITY) {
                Log.d(TAG, "Pool at capacity ($MAX_CAPACITY). skipping refill.")
                return
            }
            isRefilling = true
        }

        Log.i(TAG, "Triggering refill. Current pool size: ${pool.size}")
        try {
            MobileAds.initialize(context)
            loadAdInternal(context, adUnitId, userId)
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error in loadAdInternal: ${e.message}")
            poolMutex.withLock { isRefilling = false }
        }
    }

    private fun loadAdInternal(context: Context, adUnitId: String, userId: String) {
        Log.i(TAG, "Requesting RewardedAd.load... unitId: $adUnitId, userId: $userId")
        val adRequest = AdRequest.Builder().build()
        
        // Setup Server-Side Verification (SSV) with professional fraud detection
        // AdMob's SSV includes cryptographic signatures that the server should verify.
        val ssvOptions = ServerSideVerificationOptions.Builder()
            .setUserId(userId)
            .setCustomData("reward_v2_10pts_${System.currentTimeMillis()}")
            .build()

        RewardedAd.load(
            context, 
            adUnitId, 
            adRequest, 
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.i(TAG, "Ad preloaded SUCCESS. unitId: $adUnitId")
                    rewardedAd.setServerSideVerificationOptions(ssvOptions)
                    
                    managerScope.launch {
                        poolMutex.withLock {
                            pool.add(CachedAd(rewardedAd))
                            isRefilling = false
                            retryAttempt = 0 // Reset backoff
                            updateAvailability()
                        }
                        Log.d(TAG, "Added ad to pool. Pool size: ${pool.size}")
                        
                        // Recursive refill if still below capacity
                        if (pool.size < MAX_CAPACITY) {
                            refillPool(context, adUnitId, userId)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Ad FAILED to load: ${error.message}, code: ${error.code}, domain: ${error.domain}. userId: $userId, adUnitId: $adUnitId")
                    managerScope.launch {
                        poolMutex.withLock { isRefilling = false }
                        
                        // Exponential Backoff Strategy
                        val base = 2.0
                        val rawDelay = INITIAL_BACKOFF_DELAY_MS * base.pow(retryAttempt.toDouble())
                        val delayMs = rawDelay.toLong().coerceAtMost(MAX_BACKOFF_DELAY_MS)
                        
                        Log.e(TAG, "Retrying refill in ${delayMs}ms (Attempt $retryAttempt)")
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
     * If no ad is available, it will wait for a short period before failing.
     */
    fun showAd(
        activity: Activity,
        adUnitId: String,
        userId: String,
        taskId: String = "generic_task",
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit = {}
    ) {
        managerScope.launch {
            if (isShowingAd) {
                Log.w(TAG, "showAd requested while an ad is already showing. Ignoring.")
                return@launch
            }

            val elapsed = System.currentTimeMillis() - lastRewardTimestamp
            if (lastRewardTimestamp > 0 && elapsed < AD_COOLDOWN_MS) {
                val remainingSec = ((AD_COOLDOWN_MS - elapsed) / 1000).coerceAtLeast(1)
                val remainingMins = (remainingSec / 60) + 1
                Log.w(TAG, "Ad requested during cooldown. Remaining: ${remainingSec}s")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(activity, "Ad limit reached (4/4). Try again in ${remainingMins} mins!", android.widget.Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            // Attempt to get an ad, wait up to 5 seconds if none available
            var cachedAd = getNextValidAd()
            if (cachedAd == null) {
                Log.i(TAG, "Pool empty, triggering immediate refill and waiting...")
                refillPool(activity.applicationContext, adUnitId, userId)
                
                // Wait loop (max 5 seconds)
                val startWait = System.currentTimeMillis()
                while (cachedAd == null && (System.currentTimeMillis() - startWait) < 5000) {
                    delay(500)
                    cachedAd = getNextValidAd()
                }
            }
            
            if (cachedAd == null) {
                Log.w(TAG, "No cached ad in pool. Attempting direct load for userId: $userId")
                val adRequest = AdRequest.Builder().build()
                val ssvOptions = ServerSideVerificationOptions.Builder()
                    .setUserId(userId)
                    .setCustomData(taskId)
                    .build()

                activity.runOnUiThread {
                    RewardedAd.load(activity, adUnitId, adRequest, object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(rewardedAd: RewardedAd) {
                            rewardedAd.setServerSideVerificationOptions(ssvOptions)
                            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    isShowingAd = false
                                    onAdClosed()
                                }

                                override fun onAdFailedToShowFullScreenContent(err: com.google.android.gms.ads.AdError) {
                                    isShowingAd = false
                                    Log.e(TAG, "Failed to show direct ad: ${err.message}, granting fallback points")
                                    lastRewardTimestamp = System.currentTimeMillis()
                                    onRewardEarned()
                                    onAdClosed()
                                }
                            }
                            isShowingAd = true
                            rewardedAd.show(activity) { _ ->
                                Log.i(TAG, "User earned reward from direct ad!")
                                lastRewardTimestamp = System.currentTimeMillis()
                                onRewardEarned()
                            }
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            Log.e(TAG, "Direct Ad failed to load: ${error.message}, granting fallback points")
                            lastRewardTimestamp = System.currentTimeMillis()
                            onRewardEarned()
                            onAdClosed()
                        }
                    })
                }
                return@launch
            }

            // Update SSV options with specific taskId before showing
            val ssvOptions = ServerSideVerificationOptions.Builder()
                .setUserId(userId)
                .setCustomData(taskId)
                .build()
            cachedAd.ad.setServerSideVerificationOptions(ssvOptions)

            isShowingAd = true
            val adToShow = cachedAd.ad
            activity.runOnUiThread {
                adToShow.fullScreenContentCallback = object : FullScreenContentCallback() {
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

                adToShow.show(activity) {
                    Log.i(TAG, "User earned reward!")
                    recordAdReward()
                    onRewardEarned()
                }
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
        val elapsed = System.currentTimeMillis() - lastRewardTimestamp
        val coolingDown = lastRewardTimestamp > 0 && elapsed < AD_COOLDOWN_MS
        _hasAvailableAds.value = pool.size > 0 && !coolingDown
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
