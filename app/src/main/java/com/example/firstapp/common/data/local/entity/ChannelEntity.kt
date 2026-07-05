package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val followers: String,
    val avatarUrl: String?,
    val avatarColor: Int,
    val isVerified: Boolean,
    val isSubscribed: Boolean,
    val isAdmin: Boolean = false,
    val isMuted: Boolean = false,
    val category: String,
    val createdBy: String? = null,
    val lastPostText: String,
    val lastPostImageUrl: String?,
    val lastPostImageColor: Int,
    val lastPostTime: String
)
