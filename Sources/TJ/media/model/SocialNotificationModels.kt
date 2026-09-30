package com.tpsk.tj.media.model

enum class SocialActionType {
    LIKE,
    REPLY,
    REPOST,
    FOLLOW
}

data class SocialNotificationItem(
    val id: String,
    val actorId: String,
    val actorName: String,
    val actionText: String, // e.g., "liked your post", "reposted your thought"
    val actionType: SocialActionType,
    val postSnippet: String? = null,
    val targetPostId: String? = null,
    val timeAgo: String,
    val isRead: Boolean
)
