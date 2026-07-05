package com.example.firstapp.common.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.media.model.PostPoll
import com.example.firstapp.media.model.PostReply
import com.example.firstapp.media.model.PostType
import com.example.firstapp.media.model.Advert

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
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
    val isAuthorFollowed: Boolean,
    val repliesLocked: Boolean = false,
    val replies: List<PostReply>,
    val poll: PostPoll? = null,
    val location: String?,
    val tags: List<String>,
    val ad: Advert? = null
)
