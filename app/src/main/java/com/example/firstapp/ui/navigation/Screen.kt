package com.example.firstapp.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main?tab={tab}&shortId={shortId}") {
        fun createRoute(tab: String = "home", shortId: String? = null) = 
            "main?tab=$tab&shortId=${shortId ?: ""}"
    }
    object OtpVerification : Screen("otp_verification/{phoneNumber}/{username}/{email}/{displayName}/{password}?countryCode={countryCode}") {
        fun createRoute(phoneNumber: String, username: String, email: String, displayName: String, password: String, countryCode: String? = null) = 
            "otp_verification/${android.net.Uri.encode(phoneNumber)}/${android.net.Uri.encode(username)}/${android.net.Uri.encode(email)}/${android.net.Uri.encode(displayName)}/${android.net.Uri.encode(password)}?countryCode=${android.net.Uri.encode(countryCode ?: "")}"
    }
    object CompleteProfile : Screen("complete_profile")

    object Chat : Screen("chat/{chatId}?type={type}&jobId={jobId}&jobTitle={jobTitle}&companyName={companyName}") {
        fun createRoute(chatId: String, type: String = "direct", jobId: String? = null, jobTitle: String? = null, companyName: String? = null) = 
            "chat/$chatId?type=$type&jobId=${jobId ?: ""}&jobTitle=${android.net.Uri.encode(jobTitle ?: "")}&companyName=${android.net.Uri.encode(companyName ?: "")}"
    }
    object Vocab : Screen("vocab")
    object Files : Screen("files")
    object Calendar : Screen("calendar")
    object Story : Screen("story?userId={userId}&showComposer={showComposer}&play={play}") {
        fun createRoute(userId: String? = null, showComposer: Boolean = false, play: Boolean = true) = 
            "story?userId=${userId ?: ""}&showComposer=$showComposer&play=$play"
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
    object StorageSettings : Screen("storage_settings")
    object HelpSettings : Screen("help_settings")
    object LanguageSettings : Screen("language_settings")
    object DevicesSettings : Screen("devices_settings")
    object EditProfile : Screen("edit_profile")
    object EditWorkExperience : Screen("edit_work_experience?experienceId={experienceId}") {
        fun createRoute(experienceId: String? = null) = "edit_work_experience?experienceId=${experienceId ?: ""}"
    }
    object UserProfile : Screen("user_profile/{userId}?isGroup={isGroup}") {
        fun createRoute(userId: String, isGroup: Boolean = false) = "user_profile/${android.net.Uri.encode(userId)}?isGroup=$isGroup"
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
    object AdvertDetail : Screen("advert_detail/{advertId}") {
        fun createRoute(advertId: String) = "advert_detail/$advertId"
    }
    object Wallet : Screen("wallet")
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
}
