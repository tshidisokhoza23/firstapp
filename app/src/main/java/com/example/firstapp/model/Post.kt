package com.example.firstapp.model

import androidx.compose.ui.graphics.Color

enum class PostType {
    TEXT_ONLY,
    IMAGE_WITH_TEXT,
    VIDEO_WITH_TEXT,
    POLL,
    ARTICLE
}

data class Post(
    val id: Int,
    val authorId: Int = 0,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarColor: Color,
    val isVerifiedAuthor: Boolean = false,
    val content: String,
    val mediaUrls: List<String> = emptyList(),
    val timestamp: String,
    val likes: String,
    val repliesCount: String,
    val shares: String,
    val views: String,
    val type: PostType = PostType.TEXT_ONLY,
    val mediaPlaceholderColor: Color? = null,
    val hasThread: Boolean = false,
    val threadCount: Int = 0,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val replies: List<PostReply> = emptyList(),
    val location: String? = null,
    val tags: List<String> = emptyList()
)

data class PostReply(
    val id: Int,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarColor: Color,
    val isVerifiedAuthor: Boolean = false,
    val content: String,
    val timestamp: String,
    val likes: String,
    val isLiked: Boolean = false,
    val nestedReplies: List<PostReply> = emptyList()
)
