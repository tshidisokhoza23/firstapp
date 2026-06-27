package com.example.firstapp.chat.data.api

import com.example.firstapp.common.data.api.*
import retrofit2.Response
import retrofit2.http.*

interface ChatApi {
    @GET("contacts")
    suspend fun getContacts(): Response<ContactsResponse>

    @POST("contacts/sync")
    suspend fun syncContacts(@Body request: SyncContactsRequest): Response<ContactsResponse>

    @GET("chats")
    suspend fun getChats(): Response<ChatsResponse>

    @POST("chats")
    suspend fun createChat(@Body request: CreateChatRequest): Response<ChatSummary>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<ChatSummary>

    @GET("groups/{groupId}")
    suspend fun getGroupInfo(@Path("groupId") groupId: Int): Response<GroupInfoResponse>

    @POST("groups/{groupId}/members")
    suspend fun addGroupMember(@Path("groupId") groupId: Int, @Body request: GroupMemberRequest): Response<StatusResponse>

    @DELETE("groups/{groupId}/members")
    suspend fun removeGroupMember(@Path("groupId") groupId: Int, @Body request: GroupMemberRequest): Response<StatusResponse>

    @POST("groups/{groupId}/promote")
    suspend fun promoteToAdmin(@Path("groupId") groupId: Int, @Body request: GroupMemberRequest): Response<StatusResponse>

    @POST("groups/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: Int): Response<StatusResponse>

    @GET("calls")
    suspend fun getCalls(): Response<CallsResponse>

    @GET("conversations/{contactId}/messages")
    suspend fun getMessages(@Path("contactId") contactId: Int, @Query("page") page: Int = 1): Response<MessagesResponse>

    @POST("conversations/{contactId}/messages")
    suspend fun sendMessage(@Path("contactId") contactId: Int, @Body request: SendMessageRequest): Response<MessageResponse>

    @POST("messages/{messageId}/forward")
    suspend fun forwardMessage(@Path("messageId") messageId: String, @Body request: ForwardMessageRequest): Response<StatusResponse>

    @DELETE("messages/{messageId}")
    suspend fun deleteMessage(@Path("messageId") messageId: String, @Query("delete_for_everyone") forEveryone: Boolean = false): Response<StatusResponse>

    @PUT("messages/{messageId}")
    suspend fun editMessage(@Path("messageId") messageId: String, @Body request: EditMessageRequest): Response<MessageResponse>

    @POST("messages/{messageId}/star")
    suspend fun starMessage(@Path("messageId") messageId: String, @Body request: StarMessageRequest): Response<StatusResponse>

    @POST("messages/{messageId}/reactions")
    suspend fun reactToMessage(@Path("messageId") messageId: String, @Body request: ReactionRequest): Response<StatusResponse>

    @POST("conversations/{contactId}/read")
    suspend fun markAsRead(@Path("contactId") contactId: Int): Response<StatusResponse>

    @POST("chats/{chatId}/pin")
    suspend fun pinChat(@Path("chatId") chatId: Int, @Body request: PinRequest): Response<StatusResponse>

    @POST("chats/{chatId}/mute")
    suspend fun muteChat(@Path("chatId") chatId: Int, @Body request: MuteRequest): Response<StatusResponse>

    @POST("chats/{chatId}/archive")
    suspend fun archiveChat(@Path("chatId") chatId: Int, @Body request: ArchiveRequest): Response<StatusResponse>

    @POST("chats/{chatId}/wallpaper")
    suspend fun updateWallpaper(@Path("chatId") chatId: Int, @Body request: UpdateWallpaperRequest): Response<StatusResponse>

    @DELETE("chats/{chatId}")
    suspend fun deleteChat(@Path("chatId") chatId: Int): Response<StatusResponse>
    
    @POST("chats/{chatId}/pin-message")
    suspend fun pinMessageInChat(@Path("chatId") chatId: Int, @Body request: PinMessageRequest): Response<StatusResponse>

    @GET("messages/{messageId}/readers")
    suspend fun getMessageReaders(@Path("messageId") messageId: String): Response<ReadersResponse>
    
    @POST("messages/report")
    suspend fun reportMessage(@Body request: ReportMessageRequest): Response<StatusResponse>
}

data class SyncContactsRequest(val phone_numbers: List<String>)
data class CreateChatRequest(val contact_id: Int)
data class CreateGroupRequest(val name: String, val contact_ids: List<Int>)
data class GroupMemberRequest(val user_id: Int)
data class ForwardMessageRequest(val receiver_ids: List<Int>, val group_ids: List<Int>)
data class StarMessageRequest(val is_starred: Boolean)
data class UpdateWallpaperRequest(val wallpaper_url: String?)
data class PinMessageRequest(val message_id: String?)
data class ReportMessageRequest(val message_id: String, val reason: String)

data class ChatSummary(
    val contact_id: Int,
    val contact_name: String? = null,
    val phone_number: String? = null,
    val last_message: String? = null,
    val time: Long? = null,
    val unread_count: Int? = null,
    val avatar_url: String? = null,
    val is_group: Boolean? = false,
    val group_id: Int? = null,
    val wallpaper_url: String? = null,
    val is_online: Boolean? = false,
    val last_seen: Long? = null,
    val is_archived: Boolean? = false,
    val is_muted: Boolean? = false,
    val is_pinned: Boolean? = false,
    val public_key: String? = null
)

data class ChatsResponse(val chats: List<ChatSummary>)

data class ApiCallLog(
    val id: String,
    val session_id: String,
    val caller_id: Int,
    val callee_id: Int,
    val contact_id: Int,
    val contact_name: String,
    val contact_avatar: String? = null,
    val call_type: String,
    val status: String,
    val duration_seconds: Int,
    val started_at: Long,
    val ended_at: Long? = null
)

data class CallsResponse(val calls: List<ApiCallLog>)

data class ApiMessage(
    val id: String,
    val client_id: String? = null,
    val direction: String,
    val sender_id: Int? = null,
    val sender_name: String? = null,
    val body: String,
    val created_at: Long,
    val read_at: Long? = null,
    val delivered_at: Long? = null,
    val message_type: String? = null,
    val media_url: String? = null,
    val duration: String? = null,
    val file_name: String? = null,
    val file_size: String? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val parent_id: String? = null,
    val parent_body: String? = null,
    val parent_sender_name: String? = null,
    val reactions: Map<String, Int>? = null,
    val is_view_once: Boolean? = null,
    val expires_at: Long? = null
)

data class MessagesResponse(val messages: List<ApiMessage>, val page: Int)

data class SendMessageRequest(
    val client_id: String? = null,
    val body: String,
    val message_type: String? = null,
    val media_url: String? = null,
    val duration: String? = null,
    val file_name: String? = null,
    val file_size: String? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val parent_id: String? = null,
    val parent_body: String? = null,
    val parent_sender_name: String? = null,
    val reactions: Map<String, Int>? = null,
    val is_view_once: Boolean = false,
    val expires_in_h: Int? = null
)

data class MessageResponse(val message: ApiMessage)
data class EditMessageRequest(val body: String)
data class ReactionRequest(val emoji: String)
data class ReadersResponse(val readers: List<ApiReader>)
data class ApiReader(val id: Int, val display_name: String, val avatar_url: String?, val read_at: Long)
data class GroupInfoResponse(val group: ApiGroup, val members: List<ApiGroupMember>)
data class ApiGroup(val id: Int, val name: String, val avatar_url: String?, val created_by: Int)
data class ApiGroupMember(val id: Int, val username: String, val display_name: String, val avatar_url: String?, val is_admin: Boolean)
