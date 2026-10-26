package com.tpsk.tj.chat.nearby.model

import androidx.compose.ui.graphics.Color

data class NearbyPost(
    val id: String,
    val userId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarColor: Color = Color(0xFF00B4D8),
    val content: String,
    val coarseAreaName: String = "Nearby",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceKm: Double = 1.5,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val isLiked: Boolean = false,
    val isResolved: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val replies: List<NearbyReply> = emptyList(),
    val recentCommenterAvatars: List<String> = emptyList()
) {
    val statusBadge: String
        get() = if (isResolved) "Resolved" else "Active"
}

data class NearbyReply(
    val id: String,
    val postId: String,
    val userId: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val authorAvatarColor: Color = Color(0xFF9C27B0),
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class AreaPlace(
    val displayName: String,
    val latitude: Double,
    val longitude: Double,
    val postCount: Int = 0,
    val viewCount: Int = 0
)

data class NearbyChatRequest(
    val id: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val recipientId: String,
    val recipientName: String,
    val postReferenceSnippet: String,
    val postId: String,
    val inquiryMessage: String,
    val createdAt: Long = System.currentTimeMillis()
)
