package com.tpsk.tj.common.data.ads

import android.app.Activity
import android.content.Context
import android.view.View
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobManagerImpl : AdManager {

    override fun initialize(context: Context) {
        if (!isGooglePlayServicesReady(context)) {
            android.util.Log.w("AdMobManager", "Skipping MobileAds init: Google Play services unavailable")
            return
        }
        MobileAds.initialize(context) {}
    }

    override fun loadNativeAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit,
        onAdRevenuePaid: ((Double) -> Unit)?
    ) {
        if (!isGooglePlayServicesReady(context)) {
            onAdFailed("Google Play services unavailable")
            return
        }
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                nativeAd.setOnPaidEventListener { value ->
                    onAdRevenuePaid?.invoke(value.valueMicros / 1000000.0)
                }
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w("AdMobManager", "Native ad load failed ($adUnitId): ${error.message}")
                    onAdFailed(error.message)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    override fun loadShortsNativeAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit,
        onAdRevenuePaid: ((Double) -> Unit)?
    ) {
        if (!isGooglePlayServicesReady(context)) {
            onAdFailed("Google Play services unavailable")
            return
        }
        val videoOptions = VideoOptions.Builder()
            .setStartMuted(true)
            .setCustomControlsRequested(false)
            .build()

        val adOptions = NativeAdOptions.Builder()
            .setVideoOptions(videoOptions)
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .build()

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                nativeAd.setOnPaidEventListener { value ->
                    onAdRevenuePaid?.invoke(value.valueMicros / 1000000.0)
                }
                onAdLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w("AdMobManager", "Shorts native ad load failed ($adUnitId): ${error.message}")
                    onAdFailed(error.message)
                }
            })
            .withNativeAdOptions(adOptions)
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    override fun loadBannerAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (View) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        if (!isGooglePlayServicesReady(context)) {
            onAdFailed("Google Play services unavailable")
            return
        }
        val adView = AdView(context)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = adUnitId
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                onAdLoaded(adView)
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                android.util.Log.w("AdMobManager", "Banner ad load failed ($adUnitId): ${error.message}")
                onAdFailed(error.message)
            }
        }
        adView.loadAd(AdRequest.Builder().build())
    }

    override fun loadInterstitialAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        if (!isGooglePlayServicesReady(context)) {
            onAdFailed("Google Play services unavailable")
            return
        }
        InterstitialAd.load(context, adUnitId, AdRequest.Builder().build(), 
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onAdLoaded(ad)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w("AdMobManager", "Interstitial ad load failed ($adUnitId): ${error.message}")
                    onAdFailed(error.message)
                }
            })
    }

    override fun showInterstitialAd(
        activity: Activity,
        adObject: Any,
        onAdClosed: () -> Unit
    ) {
        if (adObject is InterstitialAd) {
            adObject.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdClosed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    onAdClosed()
                }
            }
            adObject.show(activity)
        } else {
            onAdClosed()
        }
    }

    override fun loadRewardedAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        if (!isGooglePlayServicesReady(context)) {
            onAdFailed("Google Play services unavailable")
            return
        }
        RewardedAd.load(context, adUnitId, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    onAdLoaded(ad)
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w("AdMobManager", "Rewarded ad load failed ($adUnitId): ${error.message}")
                    onAdFailed(error.message)
                }
            })
    }

    override fun showRewardedAd(
        activity: Activity,
        adObject: Any,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        if (adObject is RewardedAd) {
            adObject.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdClosed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    onAdClosed()
                }
            }
            adObject.show(activity) {
                onRewardEarned()
            }
        } else {
            onAdClosed()
        }
    }

    override fun loadNativeAds(
        context: Context,
        adUnitId: String,
        count: Int,
        onAdsLoaded: (List<Any>) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        if (!isGooglePlayServicesReady(context)) {
            onAdFailed("Google Play services unavailable")
            return
        }
        val loadedAds = mutableListOf<NativeAd>()
        var failedCount = 0

        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                loadedAds.add(nativeAd)
                if (loadedAds.size + failedCount == count) {
                    onAdsLoaded(loadedAds)
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w("AdMobManager", "Batch native ad load failed ($adUnitId): ${error.message}")
                    failedCount++
                    if (loadedAds.size + failedCount == count) {
                        onAdsLoaded(loadedAds)
                    }
                }
            })
            .build()

        repeat(count) {
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    override fun destroyAd(adObject: Any?) {
        when (adObject) {
            is NativeAd -> adObject.destroy()
            is AdView -> adObject.destroy()
        }
    }

    private fun isGooglePlayServicesReady(context: Context): Boolean {
        return GoogleApiAvailability.getInstance()
            .isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
    }
}
