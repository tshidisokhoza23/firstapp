package com.example.firstapp.data

import androidx.compose.ui.graphics.Color
import com.example.firstapp.model.Advert

object AdvertService {
    val dummyAdverts = (1..25).map { index ->
        Advert(
            id = index,
            advertiserName = "Google Ad #$index",
            time = "Sponsored",
            avatarColor = Color(0xFF4285F4),
            contentColor = Color(0xFFF1F3F4),
            contentText = "Experience the power of Google services with ad #$index. Discover more today!",
            isNativeAd = true,
            isSponsored = true
        )
    }
}
