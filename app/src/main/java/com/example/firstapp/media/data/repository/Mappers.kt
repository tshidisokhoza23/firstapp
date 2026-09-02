package com.example.firstapp.media.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.firstapp.media.data.local.entity.PostEntity
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.media.data.api.*
import com.example.firstapp.chat.data.repository.mapApiAdvert
import com.example.firstapp.media.model.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.ui.theme.AppPrimary
import java.util.ArrayList
import kotlin.text.*

fun mapMediaFeedItems(items: List<ApiFeedItem>): List<Post> = items.mapNotNull { item ->
    val itemType = item.type.uppercase(java.util.Locale.ROOT)
    when (itemType) {
        "POST", "INTERNAL_AD", "NATIVE_AD" -> item.post?.let { mapApiPost(it, itemType) }
        "EXTERNAL_AD_PLACEHOLDER" -> mapExternalAdPlaceholder(item.placeholder)
        "AD_UNIT" -> mapAdUnitPlaceholder(item)
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
        likes = if (apiPost.is_liked) Math.max(apiPost.likes, apiPost.like_count) else apiPost.likes,
        repliesCount = Math.max(apiPost.replies_count, apiPost.reply_count),
        repostCount = if (apiPost.is_reposted) Math.max(apiPost.shares, apiPost.repost_count) else apiPost.repost_count,
        shares = apiPost.actual_shares,
        views = viewsValue,
        quoteCount = apiPost.quote_count,
        type = when {
            itemType == "INTERNAL_AD" || apiPost.post_type == "INTERNAL_AD" -> PostType.INTERNAL_AD
            itemType == "NATIVE_AD" || apiPost.post_type == "NATIVE_AD" -> PostType.NATIVE_AD
            itemType == "AD_UNIT" || apiPost.post_type == "AD_UNIT" -> PostType.AD_UNIT
            apiPost.post_type?.lowercase() == "short" -> PostType.SHORT
            apiPost.post_type?.lowercase() == "repost" -> PostType.REPOST
            apiPost.post_type?.lowercase() == "standard" -> PostType.POST
            // Heuristic: if post_type is missing but it's a video on its own, it might be a short
            // but we should be careful not to catch standard video posts.
            // For now, only trust explicit "short" type.
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
        thumbnailUrl = MediaUtils.formatUrl(apiPost.thumbnail_url),
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
