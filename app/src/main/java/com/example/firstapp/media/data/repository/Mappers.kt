package com.example.firstapp.media.data.repository

import com.example.firstapp.common.data.api.*
import com.example.firstapp.media.data.api.*
import com.example.firstapp.common.data.local.entity.PostEntity
import com.example.firstapp.common.data.local.entity.StoryEntity
import com.example.firstapp.common.data.local.entity.ShortEntity
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.AppAccent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.firstapp.common.data.repository.formatUrl
import com.example.firstapp.BuildConfig

fun mapApiPost(apiPost: ApiPost): Post = Post(
    id = apiPost.id,
    authorId = apiPost.author_id,
    authorName = apiPost.author?.display_name ?: "Unknown",
    authorHandle = apiPost.author?.username ?: "",
    authorAvatarUrl = formatUrl(apiPost.author?.avatar_url),
    authorAvatarColor = AppPrimary,
    isVerifiedAuthor = apiPost.author?.is_verified ?: false,
    content = apiPost.content,
    mediaUrls = apiPost.media_urls?.map { formatUrl(it) } ?: emptyList(),
    timestamp = apiPost.created_at ?: "",
    likes = apiPost.likes.toString(),
    repliesCount = apiPost.replies_count.toString(),
    shares = apiPost.shares.toString(),
    views = apiPost.views.toString(),
    type = when (apiPost.post_type) {
        "image_with_text" -> PostType.IMAGE_WITH_TEXT
        "video_with_text" -> PostType.VIDEO_WITH_TEXT
        "article" -> PostType.ARTICLE
        "poll" -> PostType.POLL
        "song" -> PostType.SONG
        else -> PostType.TEXT_ONLY
    },
    mediaPlaceholderColor = AppPrimary.copy(0.2f),
    hasThread = apiPost.has_thread,
    threadCount = apiPost.thread_count,
    isLiked = apiPost.is_liked,
    isReposted = apiPost.is_reposted,
    isBookmarked = apiPost.is_bookmarked,
    isAuthorFollowed = apiPost.is_followed,
    replies = apiPost.replies?.map { mapApiPostReply(it) } ?: emptyList(),
    poll = apiPost.poll?.let { apiPoll ->
        PostPoll(
            expiresAt = apiPoll.expires_at,
            options = apiPoll.options.map { PollOption(it.id, it.option_text, it.vote_count) },
            userVote = apiPoll.user_vote
        )
    },
    location = apiPost.location,
    tags = apiPost.tags ?: emptyList()
)

fun mapApiPostReply(apiReply: ApiPostReply): PostReply = PostReply(
    id = apiReply.id, 
    authorName = apiReply.author?.display_name ?: apiReply.author_id.toString(), 
    authorHandle = apiReply.author?.username ?: "", 
    authorAvatarUrl = formatUrl(apiReply.author?.avatar_url),
    authorAvatarColor = Color(0xFF90CAF9), 
    isVerifiedAuthor = apiReply.author?.is_verified ?: false, 
    content = apiReply.content, 
    timestamp = apiReply.created_at ?: "", 
    likes = apiReply.likes.toString(), 
    isLiked = apiReply.is_liked, 
    isReposted = apiReply.is_reposted, 
    isBookmarked = false, 
    nestedReplies = apiReply.nested_replies?.map { mapApiPostReply(it) } ?: emptyList()
)

fun mapApiStory(apiStory: ApiStoryResponse): UserStory {
    val stories = apiStory.stories.map { 
        StoryItem(
            id = it.id, 
            content = it.content ?: "", 
            contentUrl = formatUrl(it.content_url), 
            type = when (it.story_type) { 
                "video" -> StoryType.VIDEO 
                "image" -> StoryType.IMAGE 
                else -> StoryType.TEXT 
            }, 
            timestamp = it.created_at?.let { ts -> 
                formatStoryTimestamp(ts)
            } ?: "", 
            createdAt = it.created_at ?: System.currentTimeMillis(),
            backgroundColor = Color(0xFF546E7A).copy(0.75f), 
            isViewed = it.is_viewed ?: false, 
            durationMs = it.duration_ms ?: 5000
        ) 
    }.sortedBy { it.createdAt } // Individual story items: Oldest to Newest (left to right)

    val lastUpdateMillis = stories.lastOrNull()?.createdAt ?: 0L
    
    return UserStory(
        id = apiStory.user_id, 
        userName = apiStory.user_name, 
        userAvatarUrl = formatUrl(apiStory.avatar_url), 
        userAvatarColor = Color(0xFF455A64), 
        stories = stories, 
        lastUpdated = if (lastUpdateMillis > 0) formatTimeAgo(lastUpdateMillis) else "recently",
        isMuted = false
    )
}

private fun formatStoryTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return if (diff < 24 * 60 * 60 * 1000L) {
        val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(timestamp))
    } else {
        val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
        sdf.format(java.util.Date(timestamp))
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    
    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> {
            val days = hours / 24
            "${days}d ago"
        }
    }
}

fun Post.toEntity() = PostEntity(id, authorId, authorName, authorHandle, authorAvatarUrl, authorAvatarColor.toArgb(), isVerifiedAuthor, content, mediaUrls, timestamp, likes, repliesCount, shares, views, type, mediaPlaceholderColor?.toArgb(), hasThread, threadCount, isLiked, isReposted, isBookmarked, isAuthorFollowed, replies, poll, location, tags)
fun PostEntity.toModel() = Post(id, authorId, authorName, authorHandle, authorAvatarUrl, Color(authorAvatarColor), isVerifiedAuthor, content, mediaUrls, timestamp, likes, repliesCount, shares, views, type, mediaPlaceholderColor?.let { Color(it) }, hasThread, threadCount, isLiked, isReposted, isBookmarked, isAuthorFollowed, replies, poll, location, tags)

fun UserStory.toEntity() = StoryEntity(id, userId, userName, userAvatarUrl, userAvatarColor.toArgb(), stories, lastUpdated, isMuted)
fun StoryEntity.toModel() = UserStory(id, userId, userName, userAvatarUrl, Color(userAvatarColor), stories, lastUpdated, isMuted)

fun ShortMedia.toEntity() = ShortEntity(id, creatorName, description, likes, comments, shares, videoUrl, thumbnailUrl, thumbnailColor.toArgb(), profilePictureColor.toArgb(), isLiked, isSubscribed)
fun ShortEntity.toModel() = ShortMedia(id, creatorName, description, likes, comments, shares, videoUrl, thumbnailUrl, Color(thumbnailColor), Color(profilePictureColor), isLiked, isSubscribed)
