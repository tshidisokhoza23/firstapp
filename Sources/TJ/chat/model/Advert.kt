package com.tpsk.tj.chat.model

data class Advert(
    val id: String,
    val advertiserName: String,
    val advertiserId: String = "",
    val title: String = "",
    val time: String? = null,
    val avatarUrl: String? = null,
    val avatarColor: Int,
    val contentUrl: String? = null,
    val contentColor: Int,
    val contentText: String = "",
    val actionText: String = "Learn More",
    val targetUrl: String = "",
    val isSponsored: Boolean = true,
    val impressions: String = "0",
    val clicks: String = "0",
    val isNativeAd: Boolean = false,
    val isViewed: Boolean = false
)
