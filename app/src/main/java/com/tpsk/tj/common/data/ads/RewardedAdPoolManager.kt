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
        private const val MAX_CAPACITY = 2
        private const val AD_TTL_MS = 45 * 60 * 1000L // 45 Minutes
        private const val INITIAL_BACKOFF_DELAY_MS = 2000L
        private const val MAX_BACKOFF_DELAY_MS = 64000L
        private const val SHORT_COOLDOWN_MS = 10_000L
        private const val LONG_COOLDOWN_MS = 1_800_000L // 30 minutes after every 4 completed ads
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

    private val _batchCount = MutableStateFlow(0)
    val batchCount = _batchCount.asStateFlow()

    private var isRefilling = false
    private var isShowingAd = false
    private var retryAttempt = 0
    private var shortCooldownUntil = 0L
    private var longCooldownUntil = 0L
    private var adBatchCount = 0
    private var prefs: android.content.SharedPreferences? = null

    private fun recordAdReward() {
        val now = System.currentTimeMillis()
        clearExpiredCooldowns(now)
        adBatchCount++
        if (adBatchCount >= BATCH_LIMIT) {
            adBatchCount = 0
            shortCooldownUntil = 0L
            longCooldownUntil = now + LONG_COOLDOWN_MS
            Log.i(TAG, "4 ads completed. Starting 30-minute cooldown.")
        } else {
            shortCooldownUntil = now + SHORT_COOLDOWN_MS
            Log.i(TAG, "Ad reward recorded: $adBatchCount / $BATCH_LIMIT. 10s cooldown.")
        }
        _batchCount.value = adBatchCount
        persistCooldown()
        publishCooldown(now)
    }

    private fun persistCooldown() {
        prefs?.edit()
            ?.putLong("short_cooldown_until", shortCooldownUntil)
            ?.putLong("long_cooldown_until", longCooldownUntil)
            ?.putInt("ad_batch_count", adBatchCount)
            ?.apply()
    }

    private fun clearExpiredCooldowns(now: Long) {
        if (shortCooldownUntil in 1..now) shortCooldownUntil = 0L
        if (longCooldownUntil in 1..now) longCooldownUntil = 0L
    }

    private fun remainingCooldownMs(now: Long = System.currentTimeMillis()): Long {
        clearExpiredCooldowns(now)
        val shortLeft = (shortCooldownUntil - now).coerceAtLeast(0L)
        val longLeft = (longCooldownUntil - now).coerceAtLeast(0L)
        return maxOf(shortLeft, longLeft)
    }

    private fun publishCooldown(now: Long = System.currentTimeMillis()) {
        _cooldownRemainingSeconds.value = (remainingCooldownMs(now) / 1000L).toInt()
        _batchCount.value = adBatchCount
        updateAvailability()
    }

    private fun cooldownMessage(remainingSec: Int): String {
        return if (remainingSec >= 60) {
            val mins = (remainingSec + 59) / 60
            "Ad limit reached (4/4). Try again in $mins min."
        } else {
            "Please wait ${remainingSec.coerceAtLeast(1)}s before the next ad."
        }
    }

    private fun loadFailureMessage(error: LoadAdError): String {
        return when (error.code) {
            AdRequest.ERROR_CODE_NO_FILL -> "No ad is available right now. No points were added."
            AdRequest.ERROR_CODE_NETWORK_ERROR -> "Couldn't load the ad. Check your connection. No points were added."
            AdRequest.ERROR_CODE_INVALID_REQUEST -> "This ad request was rejected. No points were added."
            else -> "Ad couldn't load. No points were added."
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
                val now = System.currentTimeMillis()
                val beforeShort = shortCooldownUntil
                val beforeLong = longCooldownUntil
                publishCooldown(now)
                if ((beforeShort in 1..now) || (beforeLong in 1..now)) {
                    persistCooldown()
                }
                delay(1000)
            }
        }
    }

    /**
     * Entry point to ensure the pool is healthy.
     */
    fun initialize(context: Context, adUnitId: String, userId: String) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences("rewarded_ad_pool_prefs", Context.MODE_PRIVATE)
            val now = System.currentTimeMillis()
            shortCooldownUntil = prefs?.getLong("short_cooldown_until", 0L) ?: 0L
            longCooldownUntil = prefs?.getLong("long_cooldown_until", 0L) ?: 0L
            adBatchCount = prefs?.getInt("ad_batch_count", 0) ?: 0
            // Older builds stored the start of a 30-minute lock in last_reward_timestamp.
            val legacyStart = prefs?.getLong("last_reward_timestamp", 0L) ?: 0L
            if (longCooldownUntil == 0L && legacyStart > 0L) {
                val legacyUntil = legacyStart + LONG_COOLDOWN_MS
                if (legacyUntil > now) longCooldownUntil = legacyUntil
            }
            clearExpiredCooldowns(now)
            if (adBatchCount !in 0 until BATCH_LIMIT) adBatchCount = 0
            persistCooldown()
            publishCooldown(now)
        }
        Log.i(TAG, "initialize unitId=$adUnitId userId=$userId batch=$adBatchCount")
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
                        val noFillFloor = if (error.code == AdRequest.ERROR_CODE_NO_FILL) 15_000L else 0L
                        val delayMs = rawDelay.toLong().coerceAtLeast(noFillFloor).coerceAtMost(MAX_BACKOFF_DELAY_MS)
                        
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

            val remainingMs = remainingCooldownMs()
            if (remainingMs > 0L) {
                val remainingSec = (remainingMs / 1000L).toInt().coerceAtLeast(1)
                Log.w(TAG, "Ad requested during cooldown. Remaining: ${remainingSec}s")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(activity, cooldownMessage(remainingSec), android.widget.Toast.LENGTH_LONG).show()
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
                            presentRewardedAd(activity, rewardedAd, adUnitId, userId, onRewardEarned, onAdClosed)
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isShowingAd = false
                            Log.e(TAG, "Direct ad failed to load: code=${error.code} ${error.message}. No points granted.")
                            android.widget.Toast.makeText(activity, loadFailureMessage(error), android.widget.Toast.LENGTH_LONG).show()
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

            presentRewardedAd(activity, cachedAd.ad, adUnitId, userId, onRewardEarned, onAdClosed)
        }
    }

    private fun presentRewardedAd(
        activity: Activity,
        rewardedAd: RewardedAd,
        adUnitId: String,
        userId: String,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        var earned = false
        isShowingAd = true
        activity.runOnUiThread {
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    isShowingAd = false
                    Log.d(TAG, "Ad dismissed. earned=$earned")
                    managerScope.launch { refillPool(activity.applicationContext, adUnitId, userId) }
                    if (!earned) {
                        android.widget.Toast.makeText(activity, "Watch the full ad to earn points.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    onAdClosed()
                }

                override fun onAdFailedToShowFullScreenContent(err: com.google.android.gms.ads.AdError) {
                    isShowingAd = false
                    Log.e(TAG, "Failed to show ad: ${err.message}. No points granted.")
                    managerScope.launch { refillPool(activity.applicationContext, adUnitId, userId) }
                    android.widget.Toast.makeText(activity, "The ad couldn't be played. No points were added.", android.widget.Toast.LENGTH_LONG).show()
                    onAdClosed()
                }
            }

            rewardedAd.show(activity) {
                if (earned) return@show
                earned = true
                Log.i(TAG, "User earned reward from a completed ad.")
                recordAdReward()
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
        _hasAvailableAds.value = pool.size > 0 && remainingCooldownMs() <= 0L
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
