package com.tpsk.tj.media.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.tpsk.tj.media.data.local.entity.PostEntity
import com.tpsk.tj.common.util.MediaUtils
import com.tpsk.tj.media.data.api.*
import com.tpsk.tj.chat.data.repository.mapApiAdvert
import com.tpsk.tj.media.model.*
import com.tpsk.tj.chat.model.*
import com.tpsk.tj.common.ui.theme.AppPrimary
import java.util.ArrayList
import kotlin.text.*

fun mapMediaFeedItems(items: List<ApiFeedItem>): List<Post> = items.mapNotNull { item ->
    val itemType = item.type.uppercase(java.util.Locale.ROOT)
    when (itemType) {
        // Ads are placed by the feed screen. Mapping server ad rows here
        // showed a second ad and shifted the list.
        "POST", "INTERNAL_AD", "NATIVE_AD" -> item.post?.let { mapApiPost(it, itemType) }
        else -> null
    }
}

fun mapApiPost(apiPost: ApiPost, itemType: String? = null): Post {
    val author = apiPost.author
    
    // Server uses author_id for the UUID in media_posts table
    val authorId: String = apiPost.author_id ?: apiPost.user_id ?: author?.id ?: ""

    val timestampRaw = apiPost.created_at
    val timestampFormatted = if (timestampRaw != null) {
        val ts = if (timestampRaw < 10000000000L) timestampRaw * 1000 else timestampRaw
        ts.toString()
    } else {
        System.currentTimeMillis().toString()
    }

    val mediaUrlsRaw = apiPost.media_urls
    val mediaUrlsList = ArrayList<String>()
    if (mediaUrlsRaw != null) {
        for (u in mediaUrlsRaw) {
            val formatted = MediaUtils.formatUrl(u)
            if (formatted != null) {
                mediaUrlsList.add(formatted)
            }
        }
    }
    if (mediaUrlsList.isEmpty()) {
        apiPost.media?.forEach { item ->
            val formatted = MediaUtils.formatUrl(item.media_url)
            if (formatted != null) {
                mediaUrlsList.add(formatted)
            }
        }
    }
    android.util.Log.d("Mapper", "Post ${apiPost.id} mapped with ${mediaUrlsList.size} media URLs. Type: ${apiPost.media_type}")

    val viewsValue = Math.max(apiPost.views, apiPost.view_count)
    android.util.Log.d("Mapper", "Post ${apiPost.id} counts: Likes=${apiPost.likes}/${apiPost.like_count}, Reposts=${apiPost.shares}/${apiPost.repost_count}, Views=${apiPost.views}/${apiPost.view_count} -> Final Views=$viewsValue")

    return Post(
        id = apiPost.id ?: "",
        authorId = authorId,
        authorName = author?.display_name ?: apiPost.author_name ?: "User",
        authorHandle = author?.username ?: apiPost.author_handle ?: "user",
        authorAvatarUrl = MediaUtils.formatUrl(author?.avatar_url ?: apiPost.author_avatar_url),
        authorAvatarColor = AppPrimary,
        isVerifiedAuthor = author?.is_verified ?: false,
        content = apiPost.content ?: apiPost.content_text ?: "",
        mediaUrls = mediaUrlsList,
        mediaType = if (apiPost.media_type != null && apiPost.media_type.length > 0) {
            apiPost.media_type
        } else if (mediaUrlsList.size > 0) {
            if (MediaUtils.isVideo(mediaUrlsList[0])) "VIDEO" else "IMAGE"
        } else {
            "TEXT"
        },
        altText = apiPost.alt_text,
        timestamp = timestampFormatted,
        likes = Math.max(apiPost.likes, apiPost.like_count),
        repliesCount = Math.max(apiPost.replies_count, apiPost.reply_count),
        repostCount = Math.max(apiPost.shares, apiPost.repost_count),
        shares = apiPost.actual_shares,
        views = viewsValue,
        quoteCount = apiPost.quote_count,
        type = when {
            itemType == "INTERNAL_AD" || apiPost.post_type == "INTERNAL_AD" -> PostType.INTERNAL_AD
            itemType == "NATIVE_AD" || apiPost.post_type == "NATIVE_AD" -> PostType.NATIVE_AD
            itemType == "AD_UNIT" || apiPost.post_type == "AD_UNIT" -> PostType.AD_UNIT
            itemType == "short" || apiPost.post_type?.lowercase() == "short" -> PostType.SHORT
            apiPost.post_type?.lowercase() == "repost" -> PostType.REPOST
            apiPost.post_type?.lowercase() == "standard" -> PostType.POST
            else -> PostType.POST
        },
        repliesLocked = apiPost.replies_locked,
        isLiked = apiPost.is_liked,
        isReposted = apiPost.is_reposted,
        isBookmarked = apiPost.is_bookmarked,
        isAuthorFollowed = apiPost.is_followed,
        replies = apiPost.replies?.map { mapApiPost(it) } ?: emptyList(),
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
        originalPost = apiPost.original_post?.let { mapApiPost(it) },
        thumbnailUrl = MediaUtils.formatUrl(apiPost.thumbnail_url)
            ?: MediaUtils.formatUrl(apiPost.media?.firstOrNull()?.thumbnail_url),
        aspectRatio = apiPost.aspect_ratio,
        durationSec = apiPost.duration_sec,
        blurhash = apiPost.blurhash,
        parentPostId = apiPost.parent_post_id,
        rootPostId = apiPost.root_post_id,
        repostOfId = apiPost.repost_of_id,
        status = apiPost.status ?: "active",
        isPinned = apiPost.is_pinned,
        replySettings = apiPost.reply_settings ?: "everyone",
        updatedAt = apiPost.updated_at?.toString(),
        hasThread = apiPost.has_thread,
        threadCount = apiPost.thread_count
    )
}

private fun mapExternalAdPlaceholder(placeholder: ApiExternalAdPlaceholder?): Post = Post(
    id = "external_ad_${placeholder?.placement ?: System.currentTimeMillis()}",
    authorName = "",
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

private fun mapAdUnitPlaceholder(item: ApiFeedItem): Post = Post(
    id = "ad_unit_${item.ad_placement_id ?: System.currentTimeMillis()}",
    authorName = "",
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

fun mapMediaReplyItems(items: List<ApiReplyItem>): List<Post> {
    android.util.Log.d("Mapper", "Mapping ${items.size} reply items")
    return items.mapNotNull { item ->
        item.reply?.let { 
            val mapped = mapApiPost(it)
            android.util.Log.d("Mapper", "Mapped reply: ${mapped.id}, content=${mapped.content.take(20)}")
            mapped
        }
    }
}

fun Post.toPostEntity(feedType: String = "standard") = PostEntity(
    id = id,
    feedType = feedType,
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
    repostCount = repostCount,
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
    repostCount = repostCount,
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

fun mapApiSocialNotification(api: ApiSocialNotification): SocialNotificationItem {
    val actionEnum = when (api.action_type?.uppercase(java.util.Locale.ROOT)) {
        "LIKE" -> SocialActionType.LIKE
        "REPLY" -> SocialActionType.REPLY
        "REPOST" -> SocialActionType.REPOST
        "FOLLOW" -> SocialActionType.FOLLOW
        else -> SocialActionType.LIKE
    }

    val timeFormatted = if (api.time_ago.isNullOrBlank()) {
        val rawTs = api.created_at
        if (rawTs != null && rawTs > 0) {
            val tsMs = if (rawTs < 10000000000L) rawTs * 1000 else rawTs
            com.tpsk.tj.common.util.formatTwitterTime(tsMs.toString())
        } else {
            "Just now"
        }
    } else {
        api.time_ago
    }

    return SocialNotificationItem(
        id = api.id ?: java.util.UUID.randomUUID().toString(),
        actorId = api.actor_id.orEmpty(),
        actorName = api.actor_name.orEmpty().ifBlank { "Someone" },
        actionText = api.action_text.orEmpty().ifBlank { "interacted with your profile" },
        actionType = actionEnum,
        postSnippet = api.post_snippet,
        targetPostId = api.target_post_id,
        timeAgo = timeFormatted,
        isRead = api.is_read
    )
}
