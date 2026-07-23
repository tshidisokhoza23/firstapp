package com.example.firstapp.common.data.ads

import android.app.Activity
import android.content.Context
import android.view.View
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdListener
import com.applovin.mediation.MaxReward
import com.applovin.mediation.MaxRewardedAdListener
import com.applovin.mediation.ads.MaxInterstitialAd
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.mediation.nativeAds.MaxNativeAdListener
import com.applovin.mediation.nativeAds.MaxNativeAdLoader
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkInitializationConfiguration
import com.applovin.sdk.AppLovinMediationProvider

class MaxManagerImpl : AdManager {

    override fun initialize(context: Context) {
        val sdkKey = "YOUR_MAX_SDK_KEY"
        val initConfig = AppLovinSdkInitializationConfiguration.builder(sdkKey, context)
            .setMediationProvider(AppLovinMediationProvider.MAX)
            .build()
        
        AppLovinSdk.getInstance(context).initialize(initConfig) { }
    }

    override fun loadNativeAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit,
        onAdRevenuePaid: ((Double) -> Unit)?
    ) {
        val nativeAdLoader = MaxNativeAdLoader(adUnitId, context)
        nativeAdLoader.setNativeAdListener(object : MaxNativeAdListener() {
            override fun onNativeAdLoaded(nativeAdView: MaxNativeAdView?, ad: MaxAd) {
                onAdLoaded(ad)
            }

            override fun onNativeAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {
                onAdFailed(error.message)
            }
        })
        nativeAdLoader.setRevenueListener { ad ->
            onAdRevenuePaid?.invoke(ad.revenue)
        }
        nativeAdLoader.loadAd()
    }

    override fun loadShortsNativeAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit,
        onAdRevenuePaid: ((Double) -> Unit)?
    ) {
        loadNativeAd(context, adUnitId, onAdLoaded, onAdFailed, onAdRevenuePaid)
    }

    override fun loadBannerAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (View) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        // Implementation for MAX Banner
    }

    override fun loadInterstitialAd(
        context: Context,
        adUnitId: String,
        onAdLoaded: (Any) -> Unit,
        onAdFailed: (String) -> Unit
    ) {
        val interstitialAd = MaxInterstitialAd(adUnitId, context as? Activity)
        interstitialAd.setListener(object : MaxAdListener {
            override fun onAdLoaded(ad: MaxAd) { onAdLoaded(ad) }
            override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) { onAdFailed(error.message) }
            override fun onAdDisplayed(ad: MaxAd) {}
            override fun onAdHidden(ad: MaxAd) {}
            override fun onAdClicked(ad: MaxAd) {}
            override fun onAdDisplayFailed(ad: MaxAd, error: com.applovin.mediation.MaxError) {}
        })
        interstitialAd.loadAd()
    }

    override fun showInterstitialAd(activity: Activity, adObject: Any, onAdClosed: () -> Unit) {
        if (adObject is MaxInterstitialAd && adObject.isReady) {
            adObject.setListener(object : MaxAdListener {
                override fun onAdLoaded(ad: MaxAd) {}
                override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {}
                override fun onAdDisplayed(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) { onAdClosed() }
                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdDisplayFailed(ad: MaxAd, error: com.applovin.mediation.MaxError) { onAdClosed() }
            })
            adObject.showAd()
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
        val rewardedAd = MaxRewardedAd.getInstance(adUnitId, context as? Activity)
        rewardedAd.setListener(object : MaxRewardedAdListener {
            override fun onAdLoaded(ad: MaxAd) { onAdLoaded(ad) }
            override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) { onAdFailed(error.message) }
            override fun onAdDisplayed(ad: MaxAd) {}
            override fun onAdHidden(ad: MaxAd) {}
            override fun onAdClicked(ad: MaxAd) {}
            override fun onAdDisplayFailed(ad: MaxAd, error: com.applovin.mediation.MaxError) {}
            override fun onUserRewarded(ad: MaxAd, reward: MaxReward) {}
        })
        rewardedAd.loadAd()
    }

    override fun showRewardedAd(
        activity: Activity,
        adObject: Any,
        onRewardEarned: () -> Unit,
        onAdClosed: () -> Unit
    ) {
        if (adObject is MaxRewardedAd && adObject.isReady) {
            adObject.setListener(object : MaxRewardedAdListener {
                override fun onAdLoaded(ad: MaxAd) {}
                override fun onAdLoadFailed(adUnitId: String, error: com.applovin.mediation.MaxError) {}
                override fun onAdDisplayed(ad: MaxAd) {}
                override fun onAdHidden(ad: MaxAd) { onAdClosed() }
                override fun onAdClicked(ad: MaxAd) {}
                override fun onAdDisplayFailed(ad: MaxAd, error: com.applovin.mediation.MaxError) { onAdClosed() }
                override fun onUserRewarded(ad: MaxAd, reward: MaxReward) { onRewardEarned() }
            })
            adObject.showAd()
        } else {
            onAdClosed()
        }
    }

    override fun destroyAd(adObject: Any?) {
        // Destroy logic
    }
}
