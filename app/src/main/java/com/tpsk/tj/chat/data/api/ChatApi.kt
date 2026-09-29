package com.tpsk.tj.chat.data.api

import com.tpsk.tj.common.data.api.StatusResponse
import com.tpsk.tj.common.data.api.ApiUser
import retrofit2.Response
import retrofit2.http.*
import kotlin.collections.emptyList

interface ChatApi {
    @GET("conversations")
    suspend fun getChats(): Response<ChatSummaryResponse>

    @GET("sync/delta")
    suspend fun syncDelta(@Query("since") since: Long): Response<SyncDeltaResponse>

    @GET("conversations/{id}")
    suspend fun getChatDetails(@Path("id") id: String): Response<ChatSummary>

    @GET("conversations/{conversationID}/messages")
    suspend fun getMessages(
        @Path("conversationID") conversationID: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = 50
    ): Response<MessagesResponse>

    @POST("conversations/{conversationID}/messages")
    suspend fun sendMessage(
        @Path("conversationID") conversationID: String,
        @Body request: SendMessageRequest
    ): Response<MessageResponse>

    @POST("conversations/{conversationID}/read")
    suspend fun markAsRead(@Path("conversationID") conversationID: String, @Body request: ReadReceiptRequest): Response<StatusResponse>

    @GET("conversations/{conversationID}/members")
    suspend fun getMembers(@Path("conversationID") conversationID: String): Response<List<ApiUser>>

    @PATCH("conversations/{conversationID}/application-status")
    suspend fun updateApplicationStatus(@Path("conversationID") conversationID: String, @Body request: StatusUpdateRequest): Response<StatusResponse>

    @POST("conversations/{groupId}/members")
    suspend fun addGroupMember(@Path("groupId") groupId: String, @Body request: AddMemberRequest): Response<StatusResponse>

    @DELETE("conversations/{groupId}/members/{userId}")
    suspend fun removeGroupMember(@Path("groupId") groupId: String, @Path("userId") userId: String): Response<StatusResponse>

    @POST("conversations/{groupId}/admin/{userId}")
    suspend fun promoteToAdmin(@Path("groupId") groupId: String, @Path("userId") userId: String): Response<StatusResponse>

    @POST("conversations/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: String): Response<StatusResponse>

    @POST("chats/dm")
    suspend fun initiateDM(@Body request: InitiateDMRequest): Response<ConversationResponse>

    @POST("chats/group")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<ConversationResponse>

    @GET("chats/groups/{groupId}")
    suspend fun getGroupInfo(@Path("groupId") groupId: String): Response<GroupInfoResponse>

    @PATCH("chats/groups/{groupId}")
    suspend fun renameGroup(@Path("groupId") groupId: String, @Body request: RenameGroupRequest): Response<StatusResponse>

    @POST("chats/groups/{groupId}/members")
    suspend fun addChatGroupMember(@Path("groupId") groupId: String, @Body request: AddMemberRequest): Response<StatusResponse>

    @HTTP(method = "DELETE", path = "chats/groups/{groupId}/members", hasBody = true)
    suspend fun removeChatGroupMember(@Path("groupId") groupId: String, @Body request: AddMemberRequest): Response<StatusResponse>

    @POST("chats/groups/{groupId}/admins")
    suspend fun promoteChatGroupAdmin(@Path("groupId") groupId: String, @Body request: AddMemberRequest): Response<StatusResponse>

    @HTTP(method = "DELETE", path = "chats/groups/{groupId}/admins", hasBody = true)
    suspend fun demoteChatGroupAdmin(@Path("groupId") groupId: String, @Body request: AddMemberRequest): Response<StatusResponse>

    @PATCH("chats/groups/{groupId}/avatar")
    suspend fun updateGroupAvatar(@Path("groupId") groupId: String, @Body request: AvatarUrlRequest): Response<StatusResponse>

    @PATCH("chats/channels/{channelId}/avatar")
    suspend fun updateChannelAvatar(@Path("channelId") channelId: String, @Body request: AvatarUrlRequest): Response<StatusResponse>

    @POST("chats/groups/{groupId}/leave")
    suspend fun leaveChatGroup(@Path("groupId") groupId: String): Response<StatusResponse>

    @POST("chats/groups/{groupId}/invite-code")
    suspend fun generateGroupInviteCode(@Path("groupId") groupId: String): Response<GroupInviteCodeResponse>

    @POST("chats/groups/join/{inviteCode}")
    suspend fun joinGroupByInvite(@Path("inviteCode") inviteCode: String): Response<GroupJoinResponse>

    @DELETE("conversations/{conversationID}")
    suspend fun deleteConversation(@Path("conversationID") conversationID: String): Response<StatusResponse>

    @POST("conversations/{conversationID}/clear")
    suspend fun clearConversation(@Path("conversationID") conversationID: String): Response<StatusResponse>

    @DELETE("conversations/{conversationID}/messages/{messageID}")
    suspend fun deleteMessage(
        @Path("conversationID") conversationID: String,
        @Path("messageID") messageId: String
    ): Response<StatusResponse>

    @PATCH("conversations/{conversationID}/messages/{messageID}")
    suspend fun editMessage(
        @Path("conversationID") conversationID: String,
        @Path("messageID") messageId: String,
        @Body request: EditMessageRequest
    ): Response<MessageResponse>

    @GET("messages/{messageID}/readers")
    suspend fun getMessageReaders(@Path("messageID") messageId: String): Response<List<ApiReader>>

    @POST("conversations/{conversationID}/messages/{messageID}/reactions")
    suspend fun reactToMessage(
        @Path("conversationID") conversationID: String,
        @Path("messageID") messageId: String,
        @Body request: ReactMessageRequest
    ): Response<ReactionsResponse>

    @POST("calls/initiate")
    suspend fun initiateCall(@Body request: InitiateCallRequest): Response<InitiateCallResponse>

    @GET("calls")
    suspend fun getCallLogs(): Response<CallLogsResponse>

    @POST("chats/channels/{channelId}/subscribe")
    suspend fun subscribeToChannel(@Path("channelId") channelId: String): Response<StatusResponse>

    @GET("calls/ice_servers")
    suspend fun getICEServers(): Response<IceServersResponse>

    @PATCH("conversations/{conversationID}/prefs")
    suspend fun updateConversationPrefs(
        @Path("conversationID") conversationID: String,
        @Body request: ConversationPrefsRequest
    ): Response<StatusResponse>

    @POST("messages/{messageID}/report")
    suspend fun reportMessage(
        @Path("messageID") messageId: String,
        @Body request: ReportMessageRequest
    ): Response<StatusResponse>

    @PATCH("conversations/{conversationID}/pinned-message")
    suspend fun pinMessage(
        @Path("conversationID") conversationID: String,
        @Body request: PinMessageRequest
    ): Response<StatusResponse>

    @POST("messages/{messageID}/star")
    suspend fun starMessage(
        @Path("messageID") messageId: String,
        @Body request: StarMessageRequest
    ): Response<StatusResponse>

    @DELETE("messages/{messageID}/star")
    suspend fun unstarMessage(@Path("messageID") messageId: String): Response<StatusResponse>

    @GET("messages/starred")
    suspend fun getStarredMessages(): Response<MessagesResponse>
}

data class AddMemberRequest(val user_id: String)
data class AvatarUrlRequest(val avatar_url: String)
data class RenameGroupRequest(val name: String)
data class EditMessageRequest(val message_text: String)
data class ReactMessageRequest(val emoji: String)
data class ReactionsResponse(val reactions: Map<String, Int> = emptyMap())
data class ConversationPrefsRequest(
    val is_muted: Boolean? = null,
    val is_pinned: Boolean? = null,
    val is_archived: Boolean? = null
)
data class ReportMessageRequest(val reason: String)
data class PinMessageRequest(val message_id: String?)
data class StarMessageRequest(val starred: Boolean)
data class ReadReceiptRequest(val last_read_message_id: String)
data class StatusUpdateRequest(val status: String)

data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

data class IceServersResponse(
    val ice_servers: List<IceServerConfig>
)

data class ChatSummary(
    val conversation_id: String,
    val title: String,
    val type: String,
    val last_message: String,
    val time: Long,
    val unread_count: Int,
    val pinned_message_id: String? = null,
    val is_muted: Boolean,
    val is_pinned: Boolean,
    val is_subscribed: Boolean = false,
    val is_following: Boolean = false,
    val is_admin: Boolean = false,
    val admins_can_post: Boolean = false,
    val contact_id: String? = null,
    val channel_id: String? = null,
    val phone_number: String? = null,
    val avatar_url: String? = null,
    val created_by: String? = null,
    val is_online: Boolean = false,
    val last_seen: Long = 0L
)

data class ChatSummaryResponse(val chats: List<ChatSummary>)

data class ApiMessage(
    val id: String = "",
    val client_uuid: String = "",
    val conversation_id: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val receiver_id: String? = null,
    val message_text: String = "",
    val media_type: String = "TEXT",
    val media_url: String? = null,
    val media_urls: List<String>? = null,
    val duration: String? = null,
    val file_name: String? = null,
    val file_size: String? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val is_starred: Boolean? = false,
    val is_delivered: Boolean? = null,
    val is_read: Boolean? = null,
    val status: String? = null,
    val created_at: Long = 0L,
    val direction: String = "incoming",
    val reactions: Map<String, Int>? = null,
    val mention_user_ids: List<String>? = null,
    val reply_to_id: String? = null,
    val reply_to_text: String? = null,
    val reply_to_sender_name: String? = null,
    val applicant_profile: ApiApplicantProfile? = null,
    val job_details: ApiJobDetails? = null,
    val link_preview: com.tpsk.tj.chat.model.LinkPreview? = null,
    val audio_amplitudes: List<Int>? = null,
    val scheduled_at: Long? = null
)

data class ApiApplicantProfile(
    val full_name: String,
    val bio: String? = null,
    val cv_url: String? = null
)

data class ApiJobDetails(
    val title: String
)

data class MessagesResponse(val messages: List<ApiMessage>, val page: Int)
data class MessageResponse(val message: ApiMessage)

data class SyncDeltaResponse(
    @com.google.gson.annotations.SerializedName("latest_sequence") val latestSequence: Long = 0,
    @com.google.gson.annotations.SerializedName("messages") val messages: List<com.google.gson.JsonElement> = emptyList()
)

data class InitiateDMRequest(val contact_id: String)
data class CreateGroupRequest(val title: String, val member_ids: List<String>)
data class ConversationResponse(val conversation_id: String)

data class InitiateCallRequest(val receiver_id: String, val call_type: String)
data class InitiateCallResponse(val session_id: String)

data class SendMessageRequest(
    val client_uuid: String,
    val message_text: String,
    val media_type: String = "TEXT",
    val media_urls: List<String> = emptyList(),
    val duration: String? = null,
    val file_name: String? = null,
    val file_size: String? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val mention_user_ids: List<String>? = null,
    val receiver_id: String? = null,
    val reply_to_id: String? = null,
    val reply_to_text: String? = null,
    val reply_to_sender_name: String? = null,
    val scheduled_at: Long? = null,
    val link_preview: com.tpsk.tj.chat.model.LinkPreview? = null,
    val audio_amplitudes: List<Int>? = null
)

data class GroupInfoResponse(
    val id: String,
    val title: String? = null,
    val name: String? = null,
    val description: String? = null,
    val avatar_url: String? = null,
    val member_ids: List<String> = emptyList(),
    val admin_ids: List<String> = emptyList(),
    val created_at: Long = 0L,
    val created_by: String? = null,
    val invite_code: String? = null,
    val conversation_id: String? = null
)

data class GroupInviteCodeResponse(val invite_code: String)
data class GroupJoinResponse(
    val status: String? = null,
    val group_id: String? = null,
    val conversation_id: String? = null
)

data class CreateChannelRequest(
    val name: String,
    val description: String,
    @com.google.gson.annotations.SerializedName("avatar_url") val avatar_url: String? = null,
    @com.google.gson.annotations.SerializedName("is_public") val is_public: Boolean = true,
    @com.google.gson.annotations.SerializedName("is_private") val is_private: Boolean = false,
    @com.google.gson.annotations.SerializedName("type") val type: String = "public",
    @com.google.gson.annotations.SerializedName("visibility") val visibility: String = "public"
)

data class ToggleAdminPostingRequest(
    val admins_can_post: Boolean
)

data class ApiChannel(
    val id: String,
    val name: String,
    val description: String?,
    val followers: Long,
    val avatar_url: String?,
    val is_verified: Boolean,
    val is_subscribed: Boolean,
    val is_admin: Boolean = false,
    @com.google.gson.annotations.SerializedName("admins_can_post") val admins_can_post: Boolean = false,
    val created_by: String? = null,
    val category: String? = null,
    val last_post_text: String? = null,
    val last_post_image_url: String? = null,
    val last_post_time: Long? = null,
    val conversation_id: String? = null,
    val is_top_in_country: Boolean = false,
    val country_code: String? = null
)

data class ChannelsResponse(val channels: List<ApiChannel>)

data class ApiReader(
    val user_id: String,
    val read_at: Long
)

data class CallLogsResponse(
    val calls: List<ApiCallLog> = emptyList()
)

data class ApiCallLog(
    val id: String? = null,
    val session_id: String? = null,
    val caller_id: String? = null,
    val callee_id: String? = null,
    val contact_id: String? = null,
    val contact_name: String? = null,
    val contact_avatar: String? = null,
    val call_type: String? = null,
    val status: String? = null,
    val duration_seconds: Int = 0,
    val started_at: Long = 0L,
    val ended_at: Long? = null
)
