package com.example.firstapp.common.data.repository

import com.example.firstapp.common.data.api.*
import com.example.firstapp.chat.data.api.*
import com.example.firstapp.media.data.api.*
import com.example.firstapp.music.data.api.*
import com.example.firstapp.drive.data.api.*
import com.example.firstapp.jobs.data.api.*
import com.example.firstapp.common.data.local.entity.*
import com.example.firstapp.chat.model.*
import com.example.firstapp.jobs.model.*
import com.example.firstapp.media.model.*
import com.example.firstapp.music.model.*
import com.example.firstapp.drive.model.*
import com.example.firstapp.media.model.ShortMedia as AppShort
import com.example.firstapp.common.model.User
import com.example.firstapp.common.model.PrivacySettings
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.firstapp.common.ui.theme.AppPrimary
import com.example.firstapp.common.ui.theme.AppAccent
import android.util.Log
import android.util.Base64
import com.example.firstapp.common.data.TokenManager
import com.example.firstapp.common.util.CryptoUtils
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import com.example.firstapp.chat.data.GlobalCallState

fun mapApiChatSummary(chat: ChatSummary): ChatMessage {
    val timestamp = chat.time ?: System.currentTimeMillis()
    val id = if (chat.is_group == true && chat.group_id != null) chat.group_id else chat.contact_id
    val resolvedName = chat.contact_name?.takeIf { it.isNotBlank() && !it.startsWith("User ") } 
        ?: GlobalCallState.getContactName(id)
        ?: chat.phone_number?.takeIf { it.isNotBlank() }
        ?: GlobalCallState.getContactPhone(id)
        ?: chat.contact_name?.takeIf { it.isNotBlank() }
        ?: (if (chat.is_group == true) "Group" else "User $id")

    return ChatMessage(
        id = id,
        senderName = resolvedName,
        lastMessage = chat.last_message ?: "",
        timestamp = timestamp,
        unreadCount = chat.unread_count ?: 0,
        lastSeen = chat.last_seen ?: 0L,
        avatarColor = com.example.firstapp.common.ui.theme.BoyishPurple,
        avatarUrl = chat.avatar_url,
        isOnline = chat.is_online ?: false,
        isMuted = chat.is_muted ?: false,
        isPinned = chat.is_pinned ?: false,
        isTyping = false,
        isGroup = chat.is_group ?: false,
        isArchived = chat.is_archived ?: false,
        wallpaperUrl = chat.wallpaper_url,
        lastMessageIsFromMe = false,
        lastMessageRead = false,
        publicKey = chat.public_key
    )
}

fun mapApiMessage(apiMessage: ApiMessage): Message {
    var body = apiMessage.body
    val isEncrypted = apiMessage.body.startsWith("ENC:")
    if (isEncrypted) {
        val encryptedText = body.removePrefix("ENC:")
        val privateKeyStr = TokenManager.getPrivateKey()
        if (privateKeyStr != null) {
            try {
                val privateBytes = Base64.decode(privateKeyStr, Base64.DEFAULT)
                val keySpec = PKCS8EncodedKeySpec(privateBytes)
                val kf = KeyFactory.getInstance("RSA")
                val privKey = kf.generatePrivate(keySpec)
                body = CryptoUtils.decrypt(encryptedText, privKey)
            } catch (e: Exception) {
                Log.e("Mappers", "Decryption failed", e)
                body = "[Encrypted Message]"
            }
        } else body = "[Encrypted Message]"
    }

    val timestamp = apiMessage.created_at
    val senderId = apiMessage.sender_id ?: 0
    val senderName = apiMessage.sender_name?.takeIf { it.isNotBlank() && !it.startsWith("User ") && it.lowercase() != "unknown" } 
        ?: GlobalCallState.getContactName(senderId)
        ?: apiMessage.contact_phone?.takeIf { it.isNotBlank() }
        ?: GlobalCallState.getContactPhone(senderId)
        ?: "Unknown"

    return Message(
        id = apiMessage.id,
        chatId = 0,
        content = body,
        senderId = senderId,
        senderName = senderName,
        timestamp = timestamp,
        isFromMe = apiMessage.direction == "outgoing",
        type = when (apiMessage.message_type?.lowercase()) {
            "image" -> MessageType.IMAGE
            "video" -> MessageType.VIDEO
            "audio", "audio_message" -> MessageType.AUDIO_MESSAGE
            "voice_call" -> MessageType.VOICE_CALL
            "location" -> MessageType.LOCATION
            "contact" -> MessageType.CONTACT
            "document" -> MessageType.DOCUMENT
            else -> MessageType.TEXT
        },
        duration = apiMessage.duration,
        mediaUri = formatUrl(apiMessage.media_url),
        fileName = apiMessage.file_name,
        fileSize = apiMessage.file_size,
        contactName = apiMessage.contact_name,
        contactPhone = apiMessage.contact_phone,
        latitude = apiMessage.latitude,
        longitude = apiMessage.longitude,
        isRead = apiMessage.read_at != null,
        isDelivered = apiMessage.delivered_at != null || apiMessage.read_at != null,
        replyToId = apiMessage.parent_id,
        replyToText = apiMessage.parent_body,
        replyToSenderName = apiMessage.parent_sender_name,
        senderAvatarUrl = null,
        isStarred = false,
        isViewOnce = apiMessage.is_view_once ?: false,
        expiresAt = apiMessage.expires_at,
        status = if (apiMessage.read_at != null) MessageStatus.READ else if (apiMessage.delivered_at != null) MessageStatus.DELIVERED else MessageStatus.SENT,
        reactions = apiMessage.reactions ?: emptyMap(),
        isEncrypted = isEncrypted,
        clientId = apiMessage.client_id
    )
}

fun mapApiContact(apiUser: ApiUser) = Contact(apiUser.id, apiUser.username, apiUser.display_name.takeIf { it.isNotBlank() && !it.startsWith("User ") } ?: apiUser.phone_number ?: apiUser.username, formatUrl(apiUser.avatar_url), apiUser.phone_number ?: "", apiUser.phone_number != null, true, apiUser.is_verified, apiUser.bio, apiUser.public_key)

fun mapApiPost(apiPost: ApiPost) = Post(id = apiPost.id, authorId = apiPost.author_id, authorName = apiPost.author?.display_name ?: "Unknown", authorHandle = apiPost.author?.username ?: "", authorAvatarUrl = formatUrl(apiPost.author?.avatar_url), authorAvatarColor = AppPrimary, isVerifiedAuthor = apiPost.author?.is_verified ?: false, content = apiPost.content, mediaUrls = apiPost.media_urls?.map { formatUrl(it) } ?: emptyList(), timestamp = apiPost.created_at ?: "", likes = apiPost.likes.toString(), repliesCount = apiPost.replies_count.toString(), shares = apiPost.shares.toString(), views = apiPost.views.toString(), type = when (apiPost.post_type) { "image_with_text" -> PostType.IMAGE_WITH_TEXT; "video_with_text" -> PostType.VIDEO_WITH_TEXT; "article" -> PostType.ARTICLE; "poll" -> PostType.POLL; "song" -> PostType.SONG; else -> PostType.TEXT_ONLY }, mediaPlaceholderColor = AppPrimary.copy(0.2f), hasThread = apiPost.has_thread, threadCount = apiPost.thread_count, isLiked = apiPost.is_liked, isReposted = apiPost.is_reposted, isBookmarked = apiPost.is_bookmarked, isAuthorFollowed = apiPost.is_followed, replies = apiPost.replies?.map { mapApiPostReply(it) } ?: emptyList(), poll = apiPost.poll?.let { apiPoll -> PostPoll(expiresAt = apiPoll.expires_at, options = apiPoll.options.map { PollOption(it.id, it.option_text, it.vote_count) }, userVote = apiPoll.user_vote) }, location = apiPost.location, tags = apiPost.tags ?: emptyList())

fun mapApiPostReply(apiReply: ApiPostReply): PostReply = PostReply(id = apiReply.id, authorName = apiReply.author?.display_name?.takeIf { it.isNotBlank() && !it.startsWith("User ") } ?: GlobalCallState.getContactName(apiReply.author_id) ?: apiReply.author_id.toString(), authorHandle = apiReply.author?.username ?: "", authorAvatarUrl = formatUrl(apiReply.author?.avatar_url), authorAvatarColor = Color(0xFF90CAF9), isVerifiedAuthor = apiReply.author?.is_verified ?: false, content = apiReply.content, timestamp = apiReply.created_at ?: "", likes = apiReply.likes.toString(), isLiked = apiReply.is_liked, isReposted = apiReply.is_reposted, isBookmarked = false, nestedReplies = apiReply.nested_replies?.map { mapApiPostReply(it) } ?: emptyList())

fun mapApiStory(apiStory: ApiStoryResponse) = UserStory(id = apiStory.user_id, userName = apiStory.user_name, userAvatarUrl = formatUrl(apiStory.avatar_url), userAvatarColor = Color(0xFF455A64), stories = apiStory.stories.map { StoryItem(id = it.id, content = it.content ?: "", contentUrl = formatUrl(it.content_url), type = when (it.story_type) { "video" -> StoryType.VIDEO; "image" -> StoryType.IMAGE; else -> StoryType.TEXT }, timestamp = it.created_at?.let { ts -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(ts)) } ?: "", createdAt = it.created_at ?: System.currentTimeMillis(), backgroundColor = Color(0xFF546E7A).copy(0.75f), isViewed = it.is_viewed ?: false, durationMs = it.duration_ms ?: 5000) }, lastUpdated = apiStory.stories.firstOrNull()?.created_at?.toString() ?: "recently", isMuted = false)

fun mapApiAdvert(apiAd: ApiAdvert) = Advert(id = apiAd.id, advertiserName = apiAd.advertiser_name, advertiserId = apiAd.advertiser_id ?: "", title = apiAd.title ?: "", time = apiAd.created_at ?: apiAd.time ?: "", avatarUrl = formatUrl(apiAd.avatar_url), avatarColor = Color(0xFF283593), contentUrl = formatUrl(apiAd.content_url), contentColor = Color(0xFFE8EAF6), contentText = apiAd.content_text, actionText = apiAd.action_text ?: "Learn More", targetUrl = apiAd.target_url ?: "", isSponsored = apiAd.is_sponsored, impressions = apiAd.impressions.toString(), clicks = apiAd.clicks.toString(), isNativeAd = apiAd.is_native_ad, isViewed = apiAd.is_viewed)

fun mapApiChannel(apiChannel: ApiChannel) = Channel(id = apiChannel.id, name = apiChannel.name, description = apiChannel.description ?: "", followers = (apiChannel.followers).toString(), avatarUrl = formatUrl(apiChannel.avatar_url), avatarColor = Color(0xFF1A237E), isVerified = apiChannel.is_verified, isSubscribed = apiChannel.is_subscribed, category = apiChannel.category ?: "Community", lastPostText = apiChannel.last_post_text ?: "", lastPostImageUrl = formatUrl(apiChannel.last_post_image_url), lastPostImageColor = Color(0xFFE8EAF6), lastPostTime = apiChannel.last_post_time ?: "")

fun mapApiShort(apiShort: ApiShort) = AppShort(id = apiShort.id, creatorName = apiShort.creator_name, description = apiShort.description, likes = apiShort.likes.toString(), comments = apiShort.comments.toString(), shares = apiShort.shares.toString(), videoUrl = formatUrl(apiShort.video_url), thumbnailUrl = formatUrl(apiShort.thumbnail_url), thumbnailColor = Color.Black, profilePictureColor = Color.Gray, isLiked = apiShort.is_liked, isSubscribed = apiShort.is_subscribed)

fun ChatMessage.toEntity() = ChatEntity(id = id, senderName = senderName, lastMessage = lastMessage, timestamp = timestamp, unreadCount = unreadCount, lastSeen = lastSeen, avatarUrl = avatarUrl, isOnline = isOnline, isMuted = isMuted, isPinned = isPinned, isTyping = isTyping, isGroup = isGroup, isArchived = isArchived, wallpaperUrl = wallpaperUrl, customNotificationTone = customNotificationTone, mediaVisibility = mediaVisibility, disappearingMessagesDuration = disappearingMessagesDuration, lastMessageIsFromMe = lastMessageIsFromMe, lastMessageRead = lastMessageRead, publicKey = publicKey)
fun ChatEntity.toModel() = ChatMessage(id = id, senderName = senderName, lastMessage = lastMessage, timestamp = timestamp, unreadCount = unreadCount, lastSeen = lastSeen, avatarColor = AppPrimary, avatarUrl = avatarUrl, isOnline = isOnline, isMuted = isMuted, isPinned = isPinned, isTyping = isTyping, isGroup = isGroup, isArchived = isArchived, wallpaperUrl = wallpaperUrl, customNotificationTone = customNotificationTone, mediaVisibility = mediaVisibility, disappearingMessagesDuration = disappearingMessagesDuration, lastMessageIsFromMe = lastMessageIsFromMe, lastMessageRead = lastMessageRead, publicKey = publicKey)
fun Message.toEntity(chatId: Int) = MessageEntity(id, chatId, content, senderId, senderName, timestamp, isFromMe, type, duration, mediaUri, fileName, fileSize, contactName, contactPhone, latitude, longitude, isRead, isDelivered, replyToId, replyToText, replyToSenderName, senderAvatarUrl, isStarred, isViewOnce, expiresAt, status, reactions, isEncrypted, clientId)
fun MessageEntity.toModel() = Message(id = id, chatId = chatId, content = content, senderId = senderId, senderName = senderName, timestamp = timestamp, isFromMe = isFromMe, type = type, duration = duration, mediaUri = mediaUri, fileName = fileName, fileSize = fileSize, contactName = contactName, contactPhone = contactPhone, latitude = latitude, longitude = longitude, isRead = isRead, isDelivered = isDelivered, replyToId = replyToId, replyToText = replyToText, replyToSenderName = replyToSenderName, senderAvatarUrl = senderAvatarUrl, isStarred = isStarred, isViewOnce = isViewOnce, expiresAt = expiresAt, status = status, reactions = reactions, isEncrypted = isEncrypted, clientId = clientId)
fun ChatSummary.toEntity() = mapApiChatSummary(this).toEntity()
fun ApiMessage.toEntity(chatId: Int) = mapApiMessage(this).toEntity(chatId)

fun CallLog.toEntity() = CallLogEntity(id = id, sessionId = sessionId, callerId = callerId, calleeId = calleeId, contactId = contactId, contactName = contactName, contactAvatar = contactAvatar, callType = callType, status = status, durationSeconds = durationSeconds, timestamp = timestamp, endedAt = endedAt)
fun CallLogEntity.toModel() = CallLog(id, sessionId, callerId, calleeId, contactId, contactName, contactAvatar, callType, status, durationSeconds, timestamp, endedAt)

fun Job.toEntity() = JobEntity(id, title, companyName, companyLogoUrl, location, salaryRange, jobType, postedTime, description, requirements, isVerified, isApplied, isSaved)
fun JobEntity.toModel() = Job(id = id, title = title, companyName = companyName, companyLogoUrl = companyLogoUrl, location = location, logoColor = AppPrimary, salaryRange = salaryRange, jobType = jobType, postedTime = postedTime, description = description, requirements = requirements, isVerified = isVerified, isApplied = isApplied, isSaved = isSaved)

fun Advert.toEntity() = AdvertEntity(id, advertiserName, advertiserId, title, time, avatarUrl, avatarColor.toArgb(), contentUrl, contentColor.toArgb(), contentText, actionText, targetUrl, isSponsored, impressions, clicks, isNativeAd, isViewed)
fun AdvertEntity.toModel() = Advert(
    id = id,
    advertiserName = advertiserName,
    advertiserId = advertiserId,
    title = title,
    time = time,
    avatarUrl = avatarUrl,
    avatarColor = Color(avatarColor),
    contentUrl = contentUrl,
    contentColor = Color(contentColor),
    contentText = contentText,
    actionText = actionText,
    targetUrl = targetUrl,
    isSponsored = isSponsored,
    impressions = impressions,
    clicks = clicks,
    isNativeAd = isNativeAd,
    isViewed = isViewed
)

fun Channel.toEntity() = ChannelEntity(id = id, name = name, description = description, followers = followers, avatarUrl = avatarUrl, avatarColor = avatarColor.toArgb(), isVerified = isVerified, isSubscribed = isSubscribed, isMuted = isMuted, category = category, lastPostText = lastPostText, lastPostImageUrl = lastPostImageUrl, lastPostImageColor = lastPostImageColor.toArgb(), lastPostTime = lastPostTime)
fun ChannelEntity.toModel() = Channel(id = id, name = name, description = description, followers = followers, avatarUrl = avatarUrl, avatarColor = Color(avatarColor), isVerified = isVerified, isSubscribed = isSubscribed, isMuted = isMuted, category = category, lastPostText = lastPostText, lastPostImageUrl = lastPostImageUrl, lastPostImageColor = Color(lastPostImageColor), lastPostTime = lastPostTime)

fun Post.toEntity() = PostEntity(id, authorId, authorName, authorHandle, authorAvatarUrl, authorAvatarColor.toArgb(), isVerifiedAuthor, content, mediaUrls, timestamp, likes, repliesCount, shares, views, type, mediaPlaceholderColor?.toArgb(), hasThread, threadCount, isLiked, isReposted, isBookmarked, isAuthorFollowed, replies, poll, location, tags)
fun PostEntity.toModel() = Post(id = id, authorId = authorId, authorName = authorName, authorHandle = authorHandle, authorAvatarUrl = authorAvatarUrl, Color(authorAvatarColor), isVerifiedAuthor = isVerifiedAuthor, content = content, mediaUrls = mediaUrls, timestamp = timestamp, likes = likes, repliesCount = repliesCount, shares = shares, views = views, type = type, mediaPlaceholderColor = mediaPlaceholderColor?.let { Color(it) }, hasThread = hasThread, threadCount = threadCount, isLiked = isLiked, isReposted = isReposted, isBookmarked = isBookmarked, isAuthorFollowed = isAuthorFollowed, replies = replies, poll = poll, location = location, tags = tags)

fun UserStory.toEntity() = StoryEntity(id, userId, userName, userAvatarUrl, userAvatarColor.toArgb(), stories, lastUpdated, isMuted)
fun StoryEntity.toModel() = UserStory(id, userId, userName, userAvatarUrl, Color(userAvatarColor), stories, lastUpdated, isMuted)

fun AppShort.toEntity() = ShortEntity(id, creatorName, description, likes, comments, shares, videoUrl, thumbnailUrl, thumbnailColor.toArgb(), profilePictureColor.toArgb(), isLiked, isSubscribed)
fun ShortEntity.toModel() = AppShort(id, creatorName, description, likes, comments, shares, videoUrl, thumbnailUrl, Color(thumbnailColor), Color(profilePictureColor), isLiked, isSubscribed)

fun Contact.toEntity() = ContactEntity(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified, bio, publicKey)
fun ContactEntity.toModel() = Contact(id, username, displayName, avatarUrl, phoneNumber, isRegistered, isAuthenticated, isVerified, bio, publicKey)

fun JobApplication.toEntity() = JobApplicationEntity(id, jobId, jobTitle, companyName, location, appliedDate, status)
fun JobApplicationEntity.toModel() = JobApplication(id, jobId, jobTitle, companyName, location, appliedDate, status, AppAccent)

fun RideOption.toEntity() = RideOptionEntity(id, name, price, currency, arrivalTime, estimatedArrival, description, iconUrl, iconColor.toArgb(), capacity)
fun RideOptionEntity.toModel() = RideOption(id, name, price, currency, arrivalTime, estimatedArrival, description, iconUrl, Color(iconColor), capacity)

fun Destination.toEntity() = DestinationEntity(id, name, address, latitude, longitude, iconColor.toArgb(), isFavorite, type, placeId)
fun DestinationEntity.toModel() = Destination(id, name, address, latitude, longitude, Color(iconColor), isFavorite, type, placeId)

fun formatUrl(url: String?): String {
    if (url == null || url.isBlank()) return ""
    if (url.startsWith("http") || url.startsWith("https") || url.startsWith("data:") || url.startsWith("local://") || url.startsWith("file://")) return url
    val baseUrl = com.example.firstapp.BuildConfig.API_BASE_URL.removeSuffix("/api/v1").removeSuffix("/api/v1/")
    return if (url.startsWith("/")) "$baseUrl$url" else "$baseUrl/$url"
}
