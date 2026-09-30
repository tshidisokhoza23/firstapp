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
            .withNativeAdOptions(feedNativeAdOptions())
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
        val displayMetrics = context.resources.displayMetrics
        val widthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        val adWidth = (widthPixels / density).toInt()
        val adaptiveAdSize = if (adWidth > 0) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
        } else {
            AdSize.BANNER
        }
        adView.setAdSize(adaptiveAdSize)
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
        onAdFailed: (String) -> Unit,
        portrait: Boolean
    ) {
        if (!isGooglePlayServicesReady(context) || count <= 0) {
            onAdsLoaded(emptyList())
            return
        }
        val loadedAds = mutableListOf<NativeAd>()
        lateinit var adLoader: AdLoader
        adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                loadedAds.add(nativeAd)
                if (!adLoader.isLoading) onAdsLoaded(loadedAds.toList())
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    android.util.Log.w("AdMobManager", "Batch native ad load failed ($adUnitId): ${error.message}")
                    if (!adLoader.isLoading) {
                        if (loadedAds.isEmpty()) onAdFailed(error.message) else onAdsLoaded(loadedAds.toList())
                    }
                }
            })
            .withNativeAdOptions(if (portrait) shortsNativeAdOptions() else feedNativeAdOptions())
            .build()

        adLoader.loadAds(AdRequest.Builder().build(), count)
    }

    private fun feedNativeAdOptions(): NativeAdOptions {
        return NativeAdOptions.Builder()
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_ANY)
            .setVideoOptions(VideoOptions.Builder().setStartMuted(true).setCustomControlsRequested(false).build())
            .setRequestMultipleImages(true)
            .build()
    }

    private fun shortsNativeAdOptions(): NativeAdOptions {
        return NativeAdOptions.Builder()
            .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_PORTRAIT)
            .setVideoOptions(VideoOptions.Builder().setStartMuted(false).setCustomControlsRequested(false).build())
            .setRequestMultipleImages(false)
            .build()
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
