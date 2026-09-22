package com.tpsk.tj.chat.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "adverts")
data class AdvertEntity(
    @PrimaryKey val id: String,
    val advertiserName: String,
    val advertiserId: String,
    val title: String,
    val time: String?,
    val avatarUrl: String?,
    val avatarColor: Int,
    val contentUrl: String?,
    val contentColor: Int,
    val contentText: String,
    val actionText: String,
    val targetUrl: String,
    val isSponsored: Boolean,
    val impressions: String,
    val clicks: String,
    val isNativeAd: Boolean,
    val isViewed: Boolean
)
