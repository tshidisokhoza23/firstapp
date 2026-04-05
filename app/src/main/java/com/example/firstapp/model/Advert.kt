package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

data class Advert(
    val id: Int,
    val advertiserName: String,
    val advertiserId: String = "",
    val title: String = "",
    val time: String,
    val avatarUrl: String? = null,
    val avatarColor: Color,
    val contentUrl: String? = null,
    val contentColor: Color,
    val contentText: String = "",
    val actionText: String = "Learn More",
    val targetUrl: String = "",
    val isSponsored: Boolean = true,
    val impressions: String = "0",
    val clicks: String = "0"
)
