package com.example.firstapp.chat.data.repository

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.firstapp.chat.data.local.entity.AdvertEntity
import com.example.firstapp.chat.data.local.entity.ChannelEntity
import com.example.firstapp.chat.data.local.entity.StoryEntity
import com.example.firstapp.common.util.MediaUtils
import com.example.firstapp.media.data.api.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.common.ui.theme.AppPrimary

fun UserStory.toStoryEntity() = StoryEntity(id, userId, userName, phoneNumber, userAvatarUrl, userAvatarColor.toArgb(), stories, lastUpdated, stories.maxByOrNull { it.createdAt }?.createdAt ?: 0L, isMuted)
fun StoryEntity.toUserStory() = UserStory(id, userId, userName, phoneNumber, userAvatarUrl, Color(userAvatarColor), stories, lastUpdated, isMuted)

fun Channel.toEntity() = ChannelEntity(
    id = id,
    name = name,
    description = description,
    followers = followers.toLongOrNull() ?: 0L,
    avatarUrl = avatarUrl,
    avatarColor = avatarColor.toArgb(),
    isVerified = isVerified,
    isSubscribed = isSubscribed,
    isAdmin = isAdmin,
    isMuted = isMuted,
    adminsCanPost = adminsCanPost,
    category = category,
    createdBy = createdBy,
    conversationId = conversationId,
    lastPostText = lastPostText,
    lastPostImageUrl = lastPostImageUrl,
    lastPostImageColor = lastPostImageColor.toArgb(),
    lastPostTime = lastPostTime
)
fun ChannelEntity.toModel() = Channel(
    id = id, name = name, description = description, followers = followers.toString(), 
    avatarUrl = avatarUrl, avatarColor = Color(avatarColor), isVerified = isVerified, 
    isSubscribed = isSubscribed, isMuted = isMuted, isAdmin = isAdmin, 
    adminsCanPost = adminsCanPost, category = category, createdBy = createdBy, 
    lastPostText = lastPostText, lastPostImageUrl = lastPostImageUrl, 
    lastPostImageColor = Color(lastPostImageColor), lastPostTime = lastPostTime,
    conversationId = conversationId
)

fun Advert.toEntity() = AdvertEntity(id, advertiserName, advertiserId, title, time, avatarUrl, avatarColor, contentUrl, contentColor, contentText, actionText, targetUrl, isSponsored, impressions, clicks, isNativeAd, isViewed)
fun AdvertEntity.toModel() = Advert(id, advertiserName, advertiserId, title, time, avatarUrl, avatarColor, contentUrl, contentColor, contentText, actionText, targetUrl, isSponsored, impressions, clicks, isNativeAd, isViewed)

fun mapApiAdvert(apiAd: ApiAdvert): Advert = Advert(
    id = apiAd.id ?: "",
    advertiserName = apiAd.advertiser_name ?: "",
    advertiserId = apiAd.advertiser_id ?: "",
    title = apiAd.title ?: "",
    time = apiAd.time,
    avatarUrl = MediaUtils.formatUrl(apiAd.avatar_url),
    avatarColor = AppPrimary.toArgb(),
    contentUrl = MediaUtils.formatUrl(apiAd.content_url),
    contentText = apiAd.content_text ?: "",
    actionText = apiAd.action_text ?: "Learn More",
    targetUrl = apiAd.target_url ?: "",
    isSponsored = apiAd.is_sponsored,
    impressions = apiAd.impressions.toString(),
    clicks = apiAd.clicks.toString(),
    isNativeAd = apiAd.is_native_ad,
    isViewed = apiAd.is_viewed,
    contentColor = 0
)

fun mapApiChannel(api: ApiChannel): Channel = Channel(
    id = api.id,
    name = api.name,
    description = api.description ?: "",
    followers = api.followers.toString(),
    avatarUrl = MediaUtils.formatUrl(api.avatar_url),
    isVerified = api.is_verified,
    isSubscribed = api.is_subscribed,
    isAdmin = api.is_admin,
    adminsCanPost = api.admins_can_post,
    category = api.category ?: "Community",
    createdBy = api.created_by,
    lastPostText = api.last_post_text ?: "",
    lastPostImageUrl = MediaUtils.formatUrl(api.last_post_image_url),
    lastPostTime = api.last_post_time?.toString() ?: "",
    avatarColor = Color.Gray,
    conversationId = api.conversation_id
)

fun mapApiStory(apiStory: ApiStoryResponse): UserStory {
    val stories = apiStory.stories.filter { 
        !(it.id.startsWith("ad_"))
    }.map {
        StoryItem(
            id = it.id, 
            content = it.content ?: "", 
            contentUrl = MediaUtils.formatUrl(it.content_url), 
            type = when (it.story_type) { 
                "video" -> StoryType.VIDEO 
                "image" -> StoryType.IMAGE 
                else -> StoryType.TEXT 
            }, 
            timestamp = it.created_at?.let { ts -> 
                formatStoryTimestamp(ts)
            } ?: "", 
            createdAt = it.created_at ?: System.currentTimeMillis(),
            backgroundColorArgb = 0xBF546E7A,
            isViewed = it.is_viewed ?: false, 
            durationMs = it.duration_ms ?: 5000
        ) 
    }.sortedBy { it.createdAt }

    val lastUpdateMillis = stories.lastOrNull()?.createdAt ?: 0L
    
    return UserStory(
        id = apiStory.user_id, 
        userId = apiStory.user_id,
        userName = apiStory.user_name, 
        phoneNumber = apiStory.phone_number,
        userAvatarUrl = MediaUtils.formatUrl(apiStory.avatar_url), 
        userAvatarColor = Color(0xFF455A64), 
        stories = stories, 
        lastUpdated = if (lastUpdateMillis > 0) formatTimeAgo(lastUpdateMillis) else "recently",
        isMuted = apiStory.is_muted
    )
}

fun formatStoryTimestamp(timestamp: Long): String {
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

fun formatTimeAgo(timestamp: Long): String {
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
