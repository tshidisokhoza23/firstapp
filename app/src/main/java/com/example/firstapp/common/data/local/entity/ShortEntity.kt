package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shorts")
data class ShortEntity(
    @PrimaryKey val id: String,
    val creatorId: String,
    val creatorName: String,
    val description: String,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val thumbnailColor: Int,
    val profilePictureColor: Int,
    val isLiked: Boolean,
    val isSubscribed: Boolean,
    val isBookmarked: Boolean = false
)
