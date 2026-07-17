package com.example.firstapp.media.model

import androidx.compose.ui.graphics.Color

enum class PostType {
    POST,
    INTERNAL_AD,
    EXTERNAL_AD_PLACEHOLDER,
    NATIVE_AD
}

data class Post(
    val id: String,
    val authorId: String = "",
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
    val type: PostType = PostType.POST,
    val mediaPlaceholderColor: Color? = null,
    val hasThread: Boolean = false,
    val threadCount: Int = 0,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val isAuthorFollowed: Boolean = false,
    val repliesLocked: Boolean = false,
    val replies: List<PostReply> = emptyList(),
    val poll: PostPoll? = null,
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val ad: Advert? = null
)

data class PostPoll(
    val expiresAt: String,
    val options: List<PollOption>,
    val userVote: String? = null
)

data class PollOption(
    val id: String,
    val text: String,
    val voteCount: Long
)

data class PostReply(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorHandle: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarColor: Color,
    val isVerifiedAuthor: Boolean = false,
    val content: String,
    val timestamp: String,
    val likes: String,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val type: PostType = PostType.POST,
    val nestedReplies: List<PostReply> = emptyList(),
    val ad: Advert? = null
)
