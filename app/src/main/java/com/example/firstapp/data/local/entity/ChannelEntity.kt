package com.example.firstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val followers: String,
    val avatarUrl: String?,
    val avatarColor: Int,
    val isVerified: Boolean,
    val isSubscribed: Boolean,
    val category: String,
    val lastPostText: String,
    val lastPostImageUrl: String?,
    val lastPostImageColor: Int,
    val lastPostTime: String
)
