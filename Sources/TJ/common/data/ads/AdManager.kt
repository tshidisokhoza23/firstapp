package com.tpsk.tj.common.data.ads

import android.content.Context
import android.view.View

/**
 * Clean Interface for swappable Ad Providers (AdMob / MAX)
 */
interface AdManager {
    fun initialize(context: Context)
    
    fun loadNativeAd(
        context: Context, 
        adUnitId: String, 
        onAdLoaded: (Any) -> Unit, 
        onAdFailed: (String) -> Unit,
        onAdRevenuePaid: ((Double) -> Unit)? = null
    )

    fun loadShortsNativeAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit,
        onAdRevenuePaid: ((Double) -> Unit)? = null
    )

    fun loadBannerAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (View) -> Unit,
        onAdFailed: (String) -> Unit
    )

    fun loadInterstitialAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit
    )

    fun showInterstitialAd(
        activity: android.app.Activity,
        adObject: Any,
        onAdClosed: () -> Unit
    )

    fun loadRewardedAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit
    )

    fun showRewardedAd(
        activity: android.app.Activity,
        adObject: Any,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    )

    fun loadNativeAds(
        context: Context,
        adUnitId: String,
        count: Int,
        onAdsLoaded: (List<Any>) -> Unit,
        onAdFailed: (String) -> Unit,
        portrait: Boolean = false
    )

    fun destroyAd(adObject: Any?)
}
