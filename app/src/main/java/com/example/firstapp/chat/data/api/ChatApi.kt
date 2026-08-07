package com.example.firstapp.chat.data.api

import com.example.firstapp.common.data.api.StatusResponse
import retrofit2.Response
import retrofit2.http.*
import kotlin.collections.emptyList

interface ChatApi {
    @GET("chats")
    suspend fun getChats(): Response<ChatSummaryResponse>

    @POST("chats/dm")
    suspend fun initiateDM(@Body request: InitiateDMRequest): Response<ConversationResponse>

    @POST("chats/group")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<ConversationResponse>

    @GET("chats/{conversationID}/messages")
    suspend fun getMessages(
        @Path("conversationID") conversationID: String,
        @Query("page") page: Int = 1,
        @Query("before_ts") beforeTs: Long? = null,
        @Query("limit") limit: Int? = null
    ): Response<MessagesResponse>

    @POST("chats/{conversationID}/messages")
    suspend fun sendMessage(
        @Path("conversationID") conversationID: String,
        @Body request: SendMessageRequest
    ): Response<MessageResponse>

    @POST("chats/{conversationID}/read")
    suspend fun markAsRead(@Path("conversationID") conversationID: String): Response<StatusResponse>

    @GET("calls/ice_servers")
    suspend fun getICEServers(): Response<IceServersResponse>
}

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
    val media_urls: List<String>,
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
