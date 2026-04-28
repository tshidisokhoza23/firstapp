package com.example.firstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.model.StoryItem

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: Int,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val userAvatarColor: Int,
    val stories: List<StoryItem>,
    val lastUpdated: String,
    val isMuted: Boolean
)
