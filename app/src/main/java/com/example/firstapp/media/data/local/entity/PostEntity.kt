package com.example.firstapp.media.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.firstapp.media.model.PostPoll
import com.example.firstapp.media.model.PostType
import com.example.firstapp.chat.model.Advert
import com.example.firstapp.media.model.Post

@Entity(tableName = "posts", primaryKeys = ["id", "feedType"])
data class PostEntity(
    val id: String,
    val feedType: String = "standard",
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String?,
    val authorAvatarColor: Int,
    val isVerifiedAuthor: Boolean,
    val content: String,
    val mediaUrls: List<String>,
    val mediaType: String?,
    val altText: String?,
    val timestamp: String,
    val likes: Long,
    val repliesCount: Long,
    val shares: Long,
    val views: Long,
    val quoteCount: Long = 0,
    val estimatedAdRevenue: Double = 0.0,
    val type: PostType,
    val visibility: String = "public",
    val mediaPlaceholderColor: Int?,
    val thumbnailUrl: String?,
    val aspectRatio: Double?,
    val durationSec: Int?,
    val blurhash: String?,
    val hasThread: Boolean,
    val threadCount: Int,
    val parentPostId: String?,
    val rootPostId: String?,
    val repostOfId: String?,
    val isLiked: Boolean,
    val isReposted: Boolean,
    val isBookmarked: Boolean,
    val isAuthorFollowed: Boolean,
    val repliesLocked: Boolean = false,
    val status: String = "published",
    val isPinned: Boolean = false,
    val replySettings: String = "everyone",
    val replies: List<Post>,
    val poll: PostPoll? = null,
    val location: String?,
    val tags: List<String>,
    val ad: Advert? = null,
    val updatedAt: String? = null
)
