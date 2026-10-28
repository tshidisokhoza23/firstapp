package com.tpsk.tj.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main?tab={tab}&subTab={subTab}&shortId={shortId}") {
        fun createRoute(tab: String = "home", subTab: String? = null, shortId: String? = null) = 
            "main?tab=$tab&subTab=${subTab ?: ""}&shortId=${shortId ?: ""}"
    }
    object OtpVerification : Screen("otp_verification/{phoneNumber}/{username}/{email}/{displayName}/{password}?countryCode={countryCode}") {
        fun createRoute(phoneNumber: String, username: String, email: String, displayName: String, password: String, countryCode: String? = null) = 
            "otp_verification/${android.net.Uri.encode(phoneNumber)}/${android.net.Uri.encode(username)}/${android.net.Uri.encode(email)}/${android.net.Uri.encode(displayName)}/${android.net.Uri.encode(password)}?countryCode=${android.net.Uri.encode(countryCode ?: "")}"
    }
    object CompleteProfile : Screen("complete_profile")

    object Chat : Screen("chat/{chatId}?type={type}&jobId={jobId}&jobTitle={jobTitle}&companyName={companyName}&displayName={displayName}") {
        fun createRoute(chatId: String, type: String = "direct", jobId: String? = null, jobTitle: String? = null, companyName: String? = null, displayName: String? = null) = 
            "chat/$chatId?type=$type&jobId=${jobId ?: ""}&jobTitle=${android.net.Uri.encode(jobTitle ?: "")}&companyName=${android.net.Uri.encode(companyName ?: "")}&displayName=${android.net.Uri.encode(displayName ?: "")}"
    }
    object Vocab : Screen("vocab")
    object SavedPosts : Screen("saved_posts")
    object Files : Screen("files")
    object Calendar : Screen("calendar")
    object Story : Screen("story?userId={userId}&showComposer={showComposer}&play={play}&storyId={storyId}") {
        fun createRoute(userId: String? = null, showComposer: Boolean = false, play: Boolean = true, storyId: String? = null) = 
            "story?userId=${android.net.Uri.encode(userId ?: "")}&showComposer=$showComposer&play=$play&storyId=${android.net.Uri.encode(storyId ?: "")}"
    }
    object NewStory : Screen("new_story")
    object ShareStory : Screen("share_story?content={content}&url={url}&type={type}") {
        fun createRoute(content: String = "", url: String = "", type: String = "text") = 
            "share_story?content=${android.net.Uri.encode(content)}&url=${android.net.Uri.encode(url)}&type=${android.net.Uri.encode(type)}"
    }
    object ArchivedChats : Screen("archived_chats")
    object StarredMessages : Screen("starred_messages")
    object Settings : Screen("settings")
    object PrivacySettings : Screen("privacy_settings")
    object BlockedUsers : Screen("blocked_users")
    object StoryPrivacy : Screen("story_privacy")
    object AccountSettings : Screen("account_settings")
    object ChatSettings : Screen("chat_settings")
    object NotificationSettings : Screen("notification_settings")
    object SocialNotification : Screen("social_notifications")
    object MingleNotification : Screen("mingle_notification")
    object MingleChats : Screen("mingle_chats")
    object MingleMessageRequests : Screen("mingle_message_requests")
    object StorageSettings : Screen("storage_settings")
    object HelpSettings : Screen("help_settings")
    object LanguageSettings : Screen("language_settings")
    object DevicesSettings : Screen("devices_settings")
    object EditProfile : Screen("edit_profile")
    object EditWorkExperience : Screen("edit_work_experience?experienceId={experienceId}") {
        fun createRoute(experienceId: String? = null) = "edit_work_experience?experienceId=${experienceId ?: ""}"
    }
    object UserProfile : Screen("user_profile/{userId}?isGroup={isGroup}&tab={tab}") {
        fun createRoute(userId: String, isGroup: Boolean = false, tab: String? = null) = "user_profile/${android.net.Uri.encode(userId)}?isGroup=$isGroup${if (!tab.isNullOrBlank()) "&tab=$tab" else ""}"
    }
    object ChatProfile : Screen("chat_profile/{userId}?isGroup={isGroup}") {
        fun createRoute(userId: String, isGroup: Boolean = false) = "chat_profile/${android.net.Uri.encode(userId)}?isGroup=$isGroup"
    }
    object FollowList : Screen("follow_list/{userId}/{type}") {
        fun createRoute(userId: String, type: String) = "follow_list/${android.net.Uri.encode(userId)}/$type"
    }
    object ContactPicker : Screen("contact_picker?isGroupMode={isGroupMode}") {
        fun createRoute(isGroupMode: Boolean = false) = "contact_picker?isGroupMode=$isGroupMode"
    }
    object CreateGroup : Screen("create_group")
    object JobProfile : Screen("job_profile")
    object Applications : Screen("applications")
    object MyJobs : Screen("my_jobs")
    object DirectMessages : Screen("direct_messages")
    object DirectChat : Screen("direct_chat/{chatId}") {
        fun createRoute(chatId: String) = "direct_chat/$chatId"
    }
    object SharedMedia : Screen("shared_media/{chatId}") {
        fun createRoute(chatId: String) = "shared_media/$chatId"
    }
    object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
    }
    object ChannelDetail : Screen("channel_detail/{channelId}") {
        fun createRoute(channelId: String) = "channel_detail/$channelId"
    }
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/${android.net.Uri.encode(groupId)}"
    }
    object GroupChat : Screen("group_chat/{groupId}") {
        fun createRoute(groupId: String) = "group_chat/${android.net.Uri.encode(groupId)}"
    }
    object JoinGroup : Screen("join_group/{inviteCode}") {
        fun createRoute(inviteCode: String) = "join_group/${android.net.Uri.encode(inviteCode)}"
    }
    object AdvertDetail : Screen("advert_detail/{advertId}") {
        fun createRoute(advertId: String) = "advert_detail/$advertId"
    }
    object Wallet : Screen("wallet")
    object Stream : Screen("stream")
    object Ride : Screen("ride")
    object Drive : Screen("drive")
    object PostJob : Screen("post_job")
    object CreateChannel : Screen("create_channel")
    object MediaMessages : Screen("media_messages")
    object MediaChat : Screen("media_chat/{userId}") {
        fun createRoute(userId: String) = "media_chat/$userId"
    }
    object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }
    object MediaViewer : Screen("media_viewer?url={url}&type={type}&position={position}") {
        fun createRoute(url: String, type: String = "image", position: Long = 0L) = 
            "media_viewer?url=${android.net.Uri.encode(url)}&type=$type&position=$position"
    }
    object MessageInfo : Screen("message_info/{messageId}") {
        fun createRoute(messageId: String) = "message_info/$messageId"
    }
    object CreateShort : Screen("create_short?videoUri={videoUri}") {
        fun createRoute(videoUri: String? = null) = "create_short?videoUri=${android.net.Uri.encode(videoUri ?: "")}"
    }
    object CreatePost : Screen("create_post?replyId={replyId}&quoteId={quoteId}") {
        fun createRoute(replyId: String? = null, quoteId: String? = null) =
            "create_post?replyId=${android.net.Uri.encode(replyId ?: "")}&quoteId=${android.net.Uri.encode(quoteId ?: "")}"
    }
    object Circles : Screen("circles")
    object CreatorDashboard : Screen("creator_dashboard")
    object TermsAndPrivacy : Screen("terms_and_privacy?url={url}&title={title}") {
        fun createRoute(url: String = "https://firstappserver.fly.dev/privacy", title: String = "Terms of Service & Privacy Policy") =
            "terms_and_privacy?url=${android.net.Uri.encode(url)}&title=${android.net.Uri.encode(title)}"
    }
    object ContestDiscovery : Screen("contest_discovery")
    object ContestFeed : Screen("contest_feed/{hashtag}") {
        fun createRoute(hashtag: String) = "contest_feed/${android.net.Uri.encode(hashtag)}"
    }
    object HashtagFeed : Screen("hashtag_feed/{hashtag}") {
        fun createRoute(hashtag: String) = "hashtag_feed/${android.net.Uri.encode(hashtag.removePrefix("#"))}"
    }
    object CreateContest : Screen("create_contest")
    object TrendingFeed : Screen("trending_feed")
    object News : Screen("news")
    object NewsWebView : Screen("news_webview?url={url}&title={title}&source={source}") {
        fun createRoute(url: String, title: String = "", source: String = "") =
            "news_webview?url=${android.net.Uri.encode(url)}&title=${android.net.Uri.encode(title)}&source=${android.net.Uri.encode(source)}"
    }
    object Events : Screen("events")
    object DiscussionThread : Screen("discussion_thread?postId={postId}&postTitle={postTitle}&postSubtitle={postSubtitle}&authorName={authorName}&authorDistance={authorDistance}&authorTime={authorTime}&postContent={postContent}&authorId={authorId}") {
        fun createRoute(
            postId: String,
            postTitle: String = "",
            postSubtitle: String = "",
            authorName: String = "",
            authorDistance: String = "",
            authorTime: String = "",
            postContent: String = "",
            authorId: String = ""
        ) = "discussion_thread?postId=${android.net.Uri.encode(postId)}&postTitle=${android.net.Uri.encode(postTitle)}&postSubtitle=${android.net.Uri.encode(postSubtitle)}&authorName=${android.net.Uri.encode(authorName)}&authorDistance=${android.net.Uri.encode(authorDistance)}&authorTime=${android.net.Uri.encode(authorTime)}&postContent=${android.net.Uri.encode(postContent)}&authorId=${android.net.Uri.encode(authorId)}"
    }
    object CreateNearbyPost : Screen("create_nearby_post?area={area}") {
        fun createRoute(area: String = "") = "create_nearby_post?area=${android.net.Uri.encode(area)}"
    }
    object MyNearbyPosts : Screen("my_nearby_posts")
    object ChatRequests : Screen("chat_requests")
    object Rent : Screen("rent")
    object Services : Screen("services")
    object Market : Screen("market")
}
