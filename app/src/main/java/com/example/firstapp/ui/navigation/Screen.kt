package com.example.firstapp.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Main : Screen("main")
    object OtpVerification : Screen("otp_verification/{phoneNumber}/{username}/{email}/{displayName}/{password}") {
        fun createRoute(phoneNumber: String, username: String, email: String, displayName: String, password: String) = 
            "otp_verification/${android.net.Uri.encode(phoneNumber)}/${android.net.Uri.encode(username)}/${android.net.Uri.encode(email)}/${android.net.Uri.encode(displayName)}/${android.net.Uri.encode(password)}"
    }
    object Chat : Screen("chat/{chatId}?isGroup={isGroup}") {
        fun createRoute(chatId: Int, isGroup: Boolean = false) = "chat/$chatId?isGroup=$isGroup"
    }
    object Vocab : Screen("vocab")
    object Files : Screen("files")
    object Calendar : Screen("calendar")
    object Story : Screen("story?userId={userId}&showComposer={showComposer}") {
        fun createRoute(userId: Int? = null, showComposer: Boolean = false) = 
            "story?userId=${userId ?: -1}&showComposer=$showComposer"
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
    object AccountSettings : Screen("account_settings")
    object ChatSettings : Screen("chat_settings")
    object NotificationSettings : Screen("notification_settings")
    object StorageSettings : Screen("storage_settings")
    object HelpSettings : Screen("help_settings")
    object LanguageSettings : Screen("language_settings")
    object DevicesSettings : Screen("devices_settings")
    object EditProfile : Screen("edit_profile")
    object UserProfile : Screen("user_profile/{userId}?isGroup={isGroup}") {
        fun createRoute(userId: Int, isGroup: Boolean = false) = "user_profile/$userId?isGroup=$isGroup"
    }
    object ContactPicker : Screen("contact_picker?isGroupMode={isGroupMode}") {
        fun createRoute(isGroupMode: Boolean = false) = "contact_picker?isGroupMode=$isGroupMode"
    }
    object CreateGroup : Screen("create_group")
    object JobProfile : Screen("job_profile")
    object Applications : Screen("applications")
    object DirectMessages : Screen("direct_messages")
    object SharedMedia : Screen("shared_media/{chatId}") {
        fun createRoute(chatId: Int) = "shared_media/$chatId"
    }
    object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: Int) = "job_detail/$jobId"
    }
    object ChannelDetail : Screen("channel_detail/{channelId}") {
        fun createRoute(channelId: Int) = "channel_detail/$channelId"
    }
    object AdvertDetail : Screen("advert_detail/{advertId}") {
        fun createRoute(advertId: Int) = "advert_detail/$advertId"
    }
    object Wallet : Screen("wallet")
}
