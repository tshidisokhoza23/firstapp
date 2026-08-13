package com.example.firstapp.chat.data.api

import com.example.firstapp.common.data.api.StatusResponse
import com.example.firstapp.common.data.api.ApiUser
import retrofit2.Response
import retrofit2.http.*
import kotlin.collections.emptyList

interface ChatApi {
    @GET("conversations")
    suspend fun getChats(): Response<ChatSummaryResponse>

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

    @DELETE("chats/messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String): Response<StatusResponse>

    @GET("calls/ice_servers")
    suspend fun getICEServers(): Response<IceServersResponse>
}

data class AddMemberRequest(val user_id: String)
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
    val is_muted: Boolean,
    val is_pinned: Boolean,
    val is_admin: Boolean = false,
    val contact_id: String? = null,
    val avatar_url: String? = null
)

data class ChatSummaryResponse(val chats: List<ChatSummary>)

data class ApiMessage(
    val id: String,
    val client_uuid: String,
    val conversation_id: String,
    val sender_id: String,
    val sender_name: String,
    val receiver_id: String? = null,
    val message_text: String,
    val media_type: String,
    val media_urls: List<String>? = null,
    val duration: String? = null,
    val file_name: String? = null,
    val file_size: String? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val created_at: Long,
    val direction: String,
    val reactions: Map<String, Int>?,
    val mention_user_ids: List<String>?,
    val reply_to_id: String? = null,
    val reply_to_text: String? = null,
    val reply_to_sender_name: String? = null,
    val applicant_profile: ApiApplicantProfile? = null,
    val job_details: ApiJobDetails? = null
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

data class InitiateDMRequest(val contact_id: String)
data class CreateGroupRequest(val title: String, val member_ids: List<String>)
data class ConversationResponse(val conversation_id: String)

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
    val reply_to_id: String? = null
)

data class GroupInfoResponse(
    val id: String,
    val title: String,
    val member_ids: List<String>,
    val admin_ids: List<String> = emptyList(),
    val created_at: Long
)

data class ApiReader(
    val user_id: String,
    val read_at: Long
)

data class ApiCallLog(
    val id: String,
    val session_id: String,
    val caller_id: String,
    val callee_id: String,
    val contact_id: String,
    val contact_name: String,
    val contact_avatar: String?,
    val call_type: String,
    val status: String,
    val duration_seconds: Int,
    val started_at: Long,
    val ended_at: Long?
)
