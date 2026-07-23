package com.example.firstapp.media.data.repository

import com.example.firstapp.common.data.api.*
import com.example.firstapp.media.data.api.*
import com.example.firstapp.common.data.local.entity.*
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.Contact
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.AppAccent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.firstapp.common.data.repository.formatUrl
import com.example.firstapp.BuildConfig

fun mapShortsFeedItems(items: List<ApiShortFeedItem>): List<ShortMedia> = items.mapNotNull { item ->
    when (item.type) {
        "ORGANIC_SHORT" -> item.short?.let { mapApiShortToShortMedia(it, ShortType.REGULAR) }
        else -> null
    }
}


private fun mapApiShortToShortMedia(apiShort: ApiShort, type: ShortType): ShortMedia = ShortMedia(
    id = apiShort.id ?: "",
    creatorId = apiShort.creator_id ?: "",
    creatorName = apiShort.creator_name ?: "User",
    creatorUsername = apiShort.creator_username ?: "",
    creatorAvatarUrl = formatUrl(apiShort.creator_avatar_url),
    description = apiShort.description ?: "",
    likes = apiShort.likes,
    comments = apiShort.comments,
    shares = apiShort.shares,
    videoUrl = formatUrl(apiShort.video_url),
    thumbnailUrl = formatUrl(apiShort.thumbnail_url),
    thumbnailColor = androidx.compose.ui.graphics.Color.Black,
    profilePictureColor = androidx.compose.ui.graphics.Color.Gray,
    isLiked = apiShort.is_liked,
    isSubscribed = apiShort.is_subscribed,
    isBookmarked = apiShort.is_bookmarked,
    type = type
)

fun mapMediaFeedItems(items: List<ApiFeedItem>): List<Post> = items.mapNotNull { item ->
    when (item.type) {
        "POST" -> item.post?.let { apiPost: ApiMediaPost ->
            // Filter out boosted posts and internal ads if we only want SDK ads
            if (apiPost.is_boosted) null 
            else mapApiMediaPost(apiPost, "POST") // Force type to POST
        }
        "ad_unit" -> mapAdUnitPlaceholder(item)
        else -> null // Ignore NATIVE_AD and INTERNAL_AD from backend
    }
}

private fun mapAdUnitPlaceholder(item: ApiFeedItem): Post = Post(
    id = "ad_unit_${item.ad_placement_id ?: System.currentTimeMillis()}",
    authorName = "Sponsored",
    authorHandle = "ad",
    authorAvatarColor = AppPrimary,
    content = "",
    timestamp = "",
    likes = 0,
    repliesCount = 0,
    shares = 0,
    views = 0,
    type = PostType.AD_UNIT
)


fun mapMediaReplyItems(items: List<ApiReplyItem>): List<PostReply> = items.mapNotNull { item: ApiReplyItem ->
    when (item.type) {
        "REPLY" -> item.reply?.let { mapApiMediaReply(it, item.type) }
        "EXTERNAL_AD_PLACEHOLDER" -> mapExternalAdReplyPlaceholder(item.placeholder)
        else -> null
    }
}

private fun mapApiMediaPost(apiPost: ApiMediaPost, itemType: String): Post = Post(
    id = apiPost.id ?: "",
    authorId = apiPost.user_id ?: "",
    authorName = apiPost.author_name ?: "User",
    authorHandle = apiPost.author_handle ?: "user",
    authorAvatarUrl = formatUrl(apiPost.author_avatar_url),
    authorAvatarColor = AppPrimary,
    isVerifiedAuthor = apiPost.is_verified_author,
    content = apiPost.content_text ?: "",
    mediaUrls = apiPost.media_urls?.map { formatUrl(it) } ?: emptyList(),
    mediaType = apiPost.media_type,
    altText = apiPost.alt_text,
    timestamp = apiPost.created_at ?: "",
    likes = apiPost.like_count,
    repliesCount = apiPost.reply_count,
    shares = apiPost.repost_count,
    views = apiPost.view_count,
    quoteCount = apiPost.quote_count,
    type = when (itemType) {
        "NATIVE_AD" -> PostType.NATIVE_AD
        "INTERNAL_AD" -> PostType.INTERNAL_AD
        else -> if (apiPost.is_boosted) PostType.INTERNAL_AD else PostType.POST
    },
    visibility = "public",
    mediaPlaceholderColor = AppPrimary.copy(0.2f),
    thumbnailUrl = formatUrl(apiPost.thumbnail_url),
    aspectRatio = apiPost.aspect_ratio,
    durationSec = apiPost.duration_sec,
    blurhash = apiPost.blurhash,
    hasThread = false,
    threadCount = 0,
    parentPostId = apiPost.parent_post_id,
    rootPostId = apiPost.root_post_id,
    repostOfId = apiPost.repost_of_id,
    isLiked = false,
    isReposted = false,
    isBookmarked = false,
    isAuthorFollowed = false,
    repliesLocked = apiPost.replies_locked,
    status = apiPost.status ?: "published",
    isPinned = apiPost.is_pinned,
    replySettings = apiPost.reply_settings ?: "everyone",
    replies = emptyList(),
    poll = null,
    location = null,
    tags = emptyList(),
    ad = null,
    originalPost = null,
    updatedAt = apiPost.updated_at
)

private fun mapApiMediaReply(apiReply: ApiMediaReply, itemType: String): PostReply = PostReply(
    id = apiReply.id ?: "",
    authorId = apiReply.user_id ?: "",
    authorName = apiReply.author_name ?: "User",
    authorHandle = apiReply.handle ?: "user",
    authorAvatarUrl = null,
    authorAvatarColor = Color(0xFF90CAF9).toArgb(),
    isVerifiedAuthor = false,
    content = apiReply.reply_text ?: "",
    timestamp = apiReply.created_at ?: "",
    likes = 0,
    isLiked = false,
    isReposted = false,
    isBookmarked = false,
    type = if (itemType == "INTERNAL_AD") PostType.INTERNAL_AD else PostType.POST,
    nestedReplies = emptyList(),
    ad = null
)

private fun mapExternalAdPlaceholder(placeholder: ApiExternalAdPlaceholder?): Post = Post(
    id = "external_ad_${placeholder?.placement ?: System.currentTimeMillis()}",
    authorName = "Sponsored",
    authorHandle = "ad",
    authorAvatarColor = AppPrimary,
    content = "",
    timestamp = "",
    likes = 0,
    repliesCount = 0,
    shares = 0,
    views = 0,
    type = PostType.EXTERNAL_AD_PLACEHOLDER
)

private fun mapExternalAdReplyPlaceholder(placeholder: ApiExternalAdPlaceholder?): PostReply = PostReply(
    id = "external_ad_reply_${placeholder?.placement ?: System.currentTimeMillis()}",
    authorId = "",
    authorName = "Sponsored",
    authorHandle = "ad",
    authorAvatarUrl = null,
    authorAvatarColor = Color(0xFF90CAF9).toArgb(),
    isVerifiedAuthor = false,
    content = "",
    timestamp = "",
    likes = 0,
    isLiked = false,
    isReposted = false,
    isBookmarked = false,
    type = PostType.EXTERNAL_AD_PLACEHOLDER,
    nestedReplies = emptyList(),
    ad = null
)

fun mapApiPost(apiPost: ApiPost): Post = Post(
    id = apiPost.id ?: "",
    authorId = apiPost.author_id ?: "",
    authorName = apiPost.author?.display_name ?: "Unknown",
    authorHandle = apiPost.author?.username ?: "",
    authorAvatarUrl = formatUrl(apiPost.author?.avatar_url),
    authorAvatarColor = AppPrimary,
    isVerifiedAuthor = apiPost.author?.is_verified ?: false,
    content = apiPost.content ?: "",
    mediaUrls = apiPost.media_urls?.map { formatUrl(it) } ?: emptyList(),
    mediaType = null,
    altText = null,
    timestamp = apiPost.created_at?.toString() ?: "",
    likes = apiPost.likes,
    repliesCount = apiPost.replies_count,
    shares = apiPost.shares,
    views = apiPost.views,
    quoteCount = 0,
    type = when (apiPost.post_type) {
        "INTERNAL_AD" -> PostType.INTERNAL_AD
        "EXTERNAL_AD_PLACEHOLDER" -> PostType.EXTERNAL_AD_PLACEHOLDER
        else -> PostType.POST
    },
    visibility = "public",
    mediaPlaceholderColor = AppPrimary.copy(0.2f),
    thumbnailUrl = null,
    aspectRatio = null,
    durationSec = null,
    blurhash = null,
    hasThread = apiPost.has_thread,
    threadCount = apiPost.thread_count,
    parentPostId = null,
    rootPostId = null,
    repostOfId = null,
    isLiked = apiPost.is_liked,
    isReposted = apiPost.is_reposted,
    isBookmarked = apiPost.is_bookmarked,
    isAuthorFollowed = apiPost.is_followed,
    repliesLocked = apiPost.replies_locked,
    status = "published",
    isPinned = false,
    replySettings = "everyone",
    replies = apiPost.replies?.map { mapApiPostReply(it) } ?: emptyList(),
    poll = apiPost.poll?.let { apiPoll ->
        PostPoll(
            expiresAt = apiPoll.expires_at?.toString() ?: "",
            options = apiPoll.options.map { PollOption(it.id, it.option_text, it.vote_count) },
            userVote = apiPoll.user_vote
        )
    },
    location = apiPost.location,
    tags = apiPost.tags ?: emptyList(),
    ad = apiPost.ad?.let { mapApiAdvert(it) },
    originalPost = null,
    updatedAt = null
)

fun mapApiAdvert(apiAd: ApiAdvert): Advert = Advert(
    id = apiAd.id ?: "",
    advertiserName = apiAd.advertiser_name ?: "Sponsored",
    advertiserId = apiAd.advertiser_id ?: "",
    title = apiAd.title ?: "",
    time = apiAd.time ?: (apiAd.created_at?.let { formatTimeAgo(it) } ?: "now"),
    avatarUrl = formatUrl(apiAd.avatar_url),
    avatarColor = Color(0xFF607D8B).toArgb(),
    contentUrl = formatUrl(apiAd.content_url),
    contentColor = Color(0xFFECEFF1).toArgb(),
    contentText = apiAd.content_text ?: "",
    actionText = apiAd.action_text ?: "Learn More",
    targetUrl = apiAd.target_url ?: "",
    isSponsored = apiAd.is_sponsored,
    impressions = apiAd.impressions.toString(),
    clicks = apiAd.clicks.toString(),
    isNativeAd = apiAd.is_native_ad,
    isViewed = apiAd.is_viewed
)

fun mapApiPostReply(apiReply: ApiPostReply): PostReply = PostReply(
    id = apiReply.id ?: "", 
    authorId = apiReply.author_id ?: "",
    authorName = apiReply.author?.display_name ?: apiReply.author_id ?: "Unknown",
    authorHandle = apiReply.author?.username ?: "", 
    authorAvatarUrl = formatUrl(apiReply.author?.avatar_url),
    authorAvatarColor = Color(0xFF90CAF9).toArgb(),
    isVerifiedAuthor = apiReply.author?.is_verified ?: false, 
    content = apiReply.content ?: "",
    timestamp = apiReply.created_at?.toString() ?: "",
    likes = apiReply.likes,
    isLiked = apiReply.is_liked, 
    isReposted = apiReply.is_reposted, 
    isBookmarked = false, 
    type = when (apiReply.post_type) {
        "INTERNAL_AD" -> PostType.INTERNAL_AD
        "EXTERNAL_AD_PLACEHOLDER" -> PostType.EXTERNAL_AD_PLACEHOLDER
        else -> PostType.POST
    },
    nestedReplies = apiReply.nested_replies?.map { mapApiPostReply(it) } ?: emptyList(),
    ad = apiReply.ad?.let { mapApiAdvert(it) }
)

fun mapApiPostReplyToShortReply(apiReply: ApiPostReply): ShortReply = ShortReply(
    id = apiReply.id ?: "",
    authorName = apiReply.author?.display_name ?: "Unknown",
    authorHandle = apiReply.author?.username ?: "",
    authorAvatarUrl = formatUrl(apiReply.author?.avatar_url),
    authorAvatarColor = Color(0xFF90CAF9),
    isVerifiedAuthor = apiReply.author?.is_verified ?: false,
    content = apiReply.content ?: "",
    timestamp = apiReply.created_at?.toString() ?: "",
    likes = apiReply.likes,
    isLiked = apiReply.is_liked
)

fun mapApiShortReplyToShortReply(apiReply: ApiShortReply): ShortReply = ShortReply(
    id = apiReply.id ?: "",
    authorName = apiReply.author?.display_name ?: "Unknown",
    authorHandle = apiReply.author?.username ?: "",
    authorAvatarUrl = formatUrl(apiReply.author?.avatar_url),
    authorAvatarColor = Color(0xFF90CAF9),
    isVerifiedAuthor = apiReply.author?.is_verified ?: false,
    content = apiReply.content ?: "",
    timestamp = apiReply.created_at?.toString() ?: "",
    likes = apiReply.likes,
    isLiked = apiReply.is_liked
)

fun mapApiChannel(api: ApiChannel): Channel = Channel(
    id = api.id,
    name = api.name,
    description = api.description ?: "",
    followers = api.followers.toString(),
    avatarUrl = formatUrl(api.avatar_url),
    isVerified = api.is_verified,
    isSubscribed = api.is_subscribed,
    isAdmin = api.is_admin,
    category = api.category ?: "Community",
    lastPostText = api.last_post_text ?: "",
    lastPostImageUrl = formatUrl(api.last_post_image_url),
    lastPostTime = api.last_post_time?.toString() ?: "",
    avatarColor = Color.Gray
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
        isMuted = apiStory.is_muted
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

fun Post.toPostEntity() = PostEntity(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorHandle = authorHandle,
    authorAvatarUrl = authorAvatarUrl,
    authorAvatarColor = authorAvatarColor.toArgb(),
    isVerifiedAuthor = isVerifiedAuthor,
    content = content,
    mediaUrls = mediaUrls,
    mediaType = mediaType,
    altText = altText,
    timestamp = timestamp,
    likes = likes,
    repliesCount = repliesCount,
    shares = shares,
    views = views,
    quoteCount = quoteCount,
    type = type,
    visibility = visibility,
    mediaPlaceholderColor = mediaPlaceholderColor?.toArgb(),
    thumbnailUrl = thumbnailUrl,
    aspectRatio = aspectRatio,
    durationSec = durationSec,
    blurhash = blurhash,
    hasThread = hasThread,
    threadCount = threadCount,
    parentPostId = parentPostId,
    rootPostId = rootPostId,
    repostOfId = repostOfId,
    isLiked = isLiked,
    isReposted = isReposted,
    isBookmarked = isBookmarked,
    isAuthorFollowed = isAuthorFollowed,
    repliesLocked = repliesLocked,
    status = status,
    isPinned = isPinned,
    replySettings = replySettings,
    replies = replies,
    poll = poll,
    location = location,
    tags = tags,
    ad = ad,
    originalPost = originalPost,
    updatedAt = updatedAt
)

fun PostEntity.toPost() = Post(
    id = id,
    authorId = authorId,
    authorName = authorName,
    authorHandle = authorHandle,
    authorAvatarUrl = authorAvatarUrl,
    authorAvatarColor = Color(authorAvatarColor),
    isVerifiedAuthor = isVerifiedAuthor,
    content = content,
    mediaUrls = mediaUrls,
    mediaType = mediaType,
    altText = altText,
    timestamp = timestamp,
    likes = likes,
    repliesCount = repliesCount,
    shares = shares,
    views = views,
    quoteCount = quoteCount,
    type = type,
    visibility = visibility,
    mediaPlaceholderColor = mediaPlaceholderColor?.let { Color(it) },
    thumbnailUrl = thumbnailUrl,
    aspectRatio = aspectRatio,
    durationSec = durationSec,
    blurhash = blurhash,
    hasThread = hasThread,
    threadCount = threadCount,
    parentPostId = parentPostId,
    rootPostId = rootPostId,
    repostOfId = repostOfId,
    isLiked = isLiked,
    isReposted = isReposted,
    isBookmarked = isBookmarked,
    isAuthorFollowed = isAuthorFollowed,
    repliesLocked = repliesLocked,
    status = status,
    isPinned = isPinned,
    replySettings = replySettings,
    replies = replies,
    poll = poll,
    location = location,
    tags = tags,
    ad = ad,
    originalPost = originalPost,
    updatedAt = updatedAt
)

fun UserStory.toStoryEntity() = StoryEntity(id, userId, userName, userAvatarUrl, userAvatarColor.toArgb(), stories, lastUpdated, isMuted)
fun StoryEntity.toUserStory() = UserStory(id, userId, userName, userAvatarUrl, Color(userAvatarColor), stories, lastUpdated, isMuted)

fun ShortMedia.toShortEntity() = ShortEntity(id, creatorId, creatorName, description, likes, comments, shares, videoUrl, thumbnailUrl, thumbnailColor.toArgb(), profilePictureColor.toArgb(), isLiked, isSubscribed)
fun ShortEntity.toShortMedia() = ShortMedia(
    id = id,
    creatorId = creatorId,
    creatorName = creatorName,
    description = description,
    likes = likes,
    comments = comments,
    shares = shares,
    videoUrl = videoUrl,
    thumbnailUrl = thumbnailUrl,
    thumbnailColor = Color(thumbnailColor),
    profilePictureColor = Color(profilePictureColor),
    isLiked = isLiked,
    isSubscribed = isSubscribed
)

fun Channel.toEntity() = ChannelEntity(id, name, description, followers, avatarUrl, avatarColor.toArgb(), isVerified, isSubscribed, isAdmin, isMuted, category, createdBy, lastPostText, lastPostImageUrl, lastPostImageColor.toArgb(), lastPostTime)
fun ChannelEntity.toModel() = Channel(id, name, description, followers, avatarUrl, Color(avatarColor), isVerified, isSubscribed, isMuted, isAdmin, category, createdBy, lastPostText, lastPostImageUrl, Color(lastPostImageColor), lastPostTime)

fun Advert.toEntity() = AdvertEntity(id, advertiserName, advertiserId, title, time, avatarUrl, avatarColor, contentUrl, contentColor, contentText, actionText, targetUrl, isSponsored, impressions, clicks, isNativeAd, isViewed)
fun AdvertEntity.toModel() = Advert(id, advertiserName, advertiserId, title, time, avatarUrl, avatarColor, contentUrl, contentColor, contentText, actionText, targetUrl, isSponsored, impressions, clicks, isNativeAd, isViewed)
