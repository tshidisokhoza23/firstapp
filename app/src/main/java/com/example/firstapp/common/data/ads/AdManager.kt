package com.example.firstapp.common.data.ads

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
        onAdFailed: (String) -> Unit
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

    fun destroyAd(adObject: Any?)
}
