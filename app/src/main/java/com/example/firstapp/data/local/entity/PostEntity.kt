package com.example.firstapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.model.PostReply
import com.example.firstapp.model.PostType

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: Int,
    val authorId: Int,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String?,
    val authorAvatarColor: Int,
    val isVerifiedAuthor: Boolean,
    val content: String,
    val mediaUrls: List<String>,
    val timestamp: String,
    val likes: String,
    val repliesCount: String,
    val shares: String,
    val views: String,
    val type: PostType,
    val mediaPlaceholderColor: Int?,
    val hasThread: Boolean,
    val threadCount: Int,
    val isLiked: Boolean,
    val isReposted: Boolean,
    val isBookmarked: Boolean,
    val replies: List<PostReply>,
    val location: String?,
    val tags: List<String>
)
