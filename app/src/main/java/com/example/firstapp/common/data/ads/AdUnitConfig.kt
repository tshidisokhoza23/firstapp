package com.example.firstapp.common.data.ads

import com.example.firstapp.BuildConfig

/**
 * Centralized configuration for all AdMob Ad Unit IDs across different environments.
 */
object AdUnitConfig {

    // Define environments
    enum class Environment {
        SIT, UAT, BETA, PROD
    }

    // Set current environment (In a real project, this might come from Build Variants / Flavors)
    private val currentEnvironment: Environment = when {
        BuildConfig.DEBUG -> Environment.SIT
        BuildConfig.BUILD_TYPE == "beta" -> Environment.BETA
        else -> Environment.PROD
    }

    /**
     * Common Banner Ad Unit ID (Used in Calling)
     */
    val BANNER_MAIN_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/6300978111" // TODO: Replace with real PROD ID
        else -> "ca-app-pub-3940256099942544/6300978111" // Test ID
    }

    val BANNER_REWARDS_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/6300978111"
        else -> "ca-app-pub-3940256099942544/6300978111"
    }

    val BANNER_JOBS_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/6300978111"
        else -> "ca-app-pub-3940256099942544/6300978111"
    }

    val BANNER_CHANNELS_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/6300978111"
        else -> "ca-app-pub-3940256099942544/6300978111"
    }

    val BANNER_DRIVE_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/6300978111"
        else -> "ca-app-pub-3940256099942544/6300978111"
    }

    /**
     * Native Ad Unit ID for Social Feed Posts
     */
    val NATIVE_POSTS_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/2247696110" // TODO: Replace with real PROD ID
        else -> "ca-app-pub-3940256099942544/2247696110" // Test ID
    }

    /**
     * Native Ad Unit ID for Vertical Shorts
     */
    val NATIVE_SHORTS_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/2247696110" // TODO: Replace with real PROD ID
        else -> "ca-app-pub-3940256099942544/2247696110" // Test ID
    }

    /**
     * Rewarded Ad Unit ID for earning points
     */
    val REWARDED_POINTS_ID: String = when (currentEnvironment) {
        Environment.PROD -> "ca-app-pub-3940256099942544/5224354917" // TODO: Replace with real PROD ID
        else -> "ca-app-pub-3940256099942544/5224354917" // Test ID
    }
}
