package com.example.firstapp.chat.ad

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.collections.mutableListOf

/**
 * STEP 3: Low-Overhead Cache Controller (StoryAdManager.kt)
 * Manages background pre-loading, portrait enforcement, and memory safety.
 */
class StoryAdManager(private val context: Context) {
    // Standard AdMob Test Unit ID for Native Ads
    private val adUnitId = "ca-app-pub-3940256099942544/2247696110" 
    private val adCache = mutableListOf<NativeAd>()
    private var lastLoadTime: Long = 0
    private val isLoading = AtomicBoolean(false)

    /**
     * AdLoader configured for immersive (9:16) portrait ads.
     */
    private val adLoader: AdLoader by lazy {
        AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd: NativeAd ->
                // Zero Latency: Add to cache for immediate display
                adCache.add(nativeAd)
                lastLoadTime = System.currentTimeMillis()
                isLoading.set(false)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoading.set(false)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    // CONSTRAINT: Force portrait delivery for Stories (9:16)
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
                    .setVideoOptions(
                        VideoOptions.Builder()
                            // STEP 5: Start muted to align with Story transitions
                            .setStartMuted(true)
                            .build()
                    )
                    .build()
            )
            .build()
    }

    /**
     * PRE-WARMING: Predictive background caching to prevent spinners.
     */
    fun preWarmAds(count: Int = 1) {
        if (isLoading.get() || adCache.size >= count) {
            // Check for stale ads even if cache is full
            refreshIfStale()
            return
        }
        
        isLoading.set(true)
        adLoader.loadAds(AdRequest.Builder().build(), count)
    }

    private fun refreshIfStale() {
        // REFRESH: Ads older than 1 hour are discarded to prevent impression fraud
        if (System.currentTimeMillis() - lastLoadTime > 3600000) {
            clearCache()
        }
    }

    fun getAvailableAds(): List<NativeAd> {
        refreshIfStale()
        return adCache.toList()
    }

    fun clearCache() {
        adCache.forEach { ad: NativeAd -> ad.destroy() }
        adCache.clear()
    }

    fun destroy() {
        clearCache()
    }
}