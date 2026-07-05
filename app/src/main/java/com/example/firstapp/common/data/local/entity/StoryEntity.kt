package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.media.model.StoryItem

@Entity(tableName = "stories")
data class StoryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String?,
    val userAvatarColor: Int,
    val stories: List<StoryItem>,
    val lastUpdated: String,
    val isMuted: Boolean
)
