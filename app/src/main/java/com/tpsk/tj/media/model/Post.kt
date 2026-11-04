package com.tpsk.tj.media.model

import androidx.compose.ui.graphics.Color
import com.tpsk.tj.chat.model.Advert

enum class PostType {
    POST,
    SHORT,
    REPOST,
    INTERNAL_AD,
    EXTERNAL_AD_PLACEHOLDER,
    NATIVE_AD,
    AD_UNIT,
    JOB
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
    val mediaType: String? = null,   // "image", "video", "gif"
    val altText: String? = null,
    val timestamp: String,
    val likes: Long = 0,
    val repliesCount: Long = 0,
    val repostCount: Long = 0,
    val shares: Long = 0,
    val views: Long = 0,
    val quoteCount: Long = 0,
    val estimatedAdRevenue: Double = 0.0,
    val type: PostType = PostType.POST, // "standard", "short", "repost"
    val visibility: String = "public", // "public", "followers"
    val mediaPlaceholderColor: Color? = null,
    val thumbnailUrl: String? = null,
    val aspectRatio: Double? = null,
    val durationSec: Int? = null,
    val blurhash: String? = null,
    val hasThread: Boolean = false,
    val threadCount: Int = 0,
    val parentPostId: String? = null,
    val rootPostId: String? = null,
    val repostOfId: String? = null,
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val isAuthorFollowed: Boolean = false,
    val repliesLocked: Boolean = false,
    val status: String = "published", // "active", "deleted", "flagged"
    val isPinned: Boolean = false,
    val replySettings: String = "everyone", // "everyone", "following"
    val replies: List<Post> = emptyList(),
    val poll: PostPoll? = null,
    val location: String? = null,
    val category: String = "general",
    val tags: List<String> = emptyList(),
    val ad: Advert? = null,
    val originalPost: Post? = null,
    val updatedAt: String? = null,
    val mood: String = "",
    val aiAssisted: Boolean = false,
    val isContest: Boolean = false,
    val contestVotes: Int = 0,
    val collaborators: List<String> = emptyList()
) {
    val formattedReplies: String by lazy(LazyThreadSafetyMode.NONE) { if (repliesCount > 0) "Reply ${formatCount(repliesCount)}" else "Reply 0" }
    val formattedReposts: String by lazy(LazyThreadSafetyMode.NONE) { if (repostCount > 0) "Repost ${formatCount(repostCount)}" else "Repost 0" }
    val formattedLikes: String by lazy(LazyThreadSafetyMode.NONE) { if (likes > 0) "Like ${formatCount(likes)}" else "Like" }
    val formattedViews: String by lazy(LazyThreadSafetyMode.NONE) { "Views ${formatCount(views)}" }
    val formattedShares: String by lazy(LazyThreadSafetyMode.NONE) { "Share ${formatCount(shares)}" }
    val formattedShortViews: String by lazy(LazyThreadSafetyMode.NONE) { formatCount(views) }
    val formattedQuotes: String by lazy(LazyThreadSafetyMode.NONE) { if (isBookmarked) "Saved ${formatCount(quoteCount)}" else "Save ${formatCount(quoteCount)}" }
    val formattedTimestamp: String by lazy(LazyThreadSafetyMode.NONE) { com.tpsk.tj.common.util.formatTwitterTime(timestamp) }
    val formattedVideoUrl: String? by lazy(LazyThreadSafetyMode.NONE) {
        if (mediaUrls.isNotEmpty() && com.tpsk.tj.common.util.MediaUtils.isVideo(mediaUrls[0])) {
            com.tpsk.tj.common.util.MediaUtils.formatUrl(mediaUrls[0])
        } else null
    }
    val formattedThumbnailUrl: String? by lazy(LazyThreadSafetyMode.NONE) {
        if (!thumbnailUrl.isNullOrBlank()) {
            com.tpsk.tj.common.util.MediaUtils.formatUrl(thumbnailUrl)
        } else formattedVideoUrl
    }
    val formattedAvatarUrl: String? by lazy(LazyThreadSafetyMode.NONE) {
        com.tpsk.tj.common.util.MediaUtils.formatUrl(authorAvatarUrl)
    }

    private fun formatCount(count: Long): String {
        val nonNegative = if (count < 0) 0L else count
        return when {
            nonNegative >= 1_000_000 -> String.format(java.util.Locale.US, "%.1fM", nonNegative / 1_000_000f)
            nonNegative >= 1_000 -> String.format(java.util.Locale.US, "%.1fK", nonNegative / 1_000f)
            else -> nonNegative.toString()
        }
    }
}

fun Post.isOwnedBy(userId: String?, userHandle: String? = null): Boolean {
    val cleanUserId = userId?.trim().orEmpty()
    val cleanUserHandle = userHandle?.trim().orEmpty()
    val authorId = this.authorId.trim()
    val authorHandle = this.authorHandle.trim()

    val matchId = cleanUserId.isNotEmpty() && authorId.isNotEmpty() && cleanUserId == authorId
    val matchHandle = cleanUserHandle.isNotEmpty() && authorHandle.isNotEmpty() && cleanUserHandle.equals(authorHandle, ignoreCase = true)

    return matchId || matchHandle
}

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
