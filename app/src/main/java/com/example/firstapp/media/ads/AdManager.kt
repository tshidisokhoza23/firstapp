package com.example.firstapp.media.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdManager(private val context: Context) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private var onLoadedAction: (() -> Unit)? = null

    private val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    init {
        MobileAds.initialize(context) { loadRewardedAd() }
    }

    fun loadRewardedAd() {
        if (isLoading) return
        isLoading = true

        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, AD_UNIT_ID, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                isLoading = false
                onLoadedAction = null
            }

            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
                isLoading = false
                onLoadedAction?.invoke()
                onLoadedAction = null
            }
        })
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (rewardedAd != null) {
            val adToShow = rewardedAd
            rewardedAd = null
            
            adToShow?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    onAdClosed()
                    loadRewardedAd()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    onAdClosed()
                    loadRewardedAd()
                }
            }
            
            adToShow?.show(activity) {
                onRewardEarned()
                loadRewardedAd()
            }
        } else {
            // MOCK BEHAVIOR FOR DEBUGGING/INSTANT SUCCESS if ad not loaded
            onRewardEarned()
            onAdClosed()
            loadRewardedAd()
        }
    }
}
