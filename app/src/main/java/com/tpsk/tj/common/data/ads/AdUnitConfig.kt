package com.tpsk.tj.common.data.ads

import com.tpsk.tj.BuildConfig

/**
 * Centralized configuration for AdMob Ad Unit IDs across environments.
 * Uses Real Ad Units for BETA and PROD environments.
 * Uses Google Test Ad Units for SIT and UAT environments.
 */
object AdUnitConfig {

    private val isRealAdEnvironment: Boolean
        get() = BuildConfig.FLAVOR.equals("beta", ignoreCase = true) || 
                BuildConfig.FLAVOR.equals("prod", ignoreCase = true)

    val ADMOB_APP_ID: String
        get() = if (isRealAdEnvironment) {
            "ca-app-pub-4322207563956922~1108788212"
        } else {
            "ca-app-pub-3940256099942544~3347511713"
        }

    /**
     * Banner Ad Unit IDs
     */
    val BANNER_MAIN_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/5798752977" else "ca-app-pub-3940256099942544/6300978111"
    val BANNER_REWARDS_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/5798752977" else "ca-app-pub-3940256099942544/6300978111"
    val BANNER_JOBS_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/5798752977" else "ca-app-pub-3940256099942544/6300978111"
    val BANNER_CHANNELS_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/5798752977" else "ca-app-pub-3940256099942544/6300978111"
    val BANNER_DRIVE_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/5798752977" else "ca-app-pub-3940256099942544/6300978111"

    /**
     * Native Ad Unit ID for Social Feed Posts
     */
    val NATIVE_POSTS_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/1859507960" else "ca-app-pub-3940256099942544/2247696110"

    /**
     * Native Ad Unit ID for Vertical Shorts
     */
    val NATIVE_SHORTS_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/1859507960" else "ca-app-pub-3940256099942544/2247696110"

    /**
     * Rewarded Ad Unit ID for Earning Points
     */
    val REWARDED_POINTS_ID: String get() = if (isRealAdEnvironment) "ca-app-pub-4322207563956922/5594828136" else "ca-app-pub-3940256099942544/5224354917"
}
