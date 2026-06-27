package com.example.firstapp.common.data.api

import com.example.firstapp.common.model.User
import com.example.firstapp.common.model.PrivacySettings
import com.example.firstapp.chat.data.api.ChatSummary
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.music.data.api.ApiSong
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @PUT("users/me")
    suspend fun updateUser(@Body request: UpdateUserRequest): Response<UserResponse>

    @DELETE("users/me")
    suspend fun deleteAccount(): Response<StatusResponse>

    @POST("devices/push-token")
    suspend fun upsertPushToken(@Body request: PushTokenRequest): Response<StatusResponse>

    @POST("users/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: Int): Response<StatusResponse>

    @POST("users/{userId}/unblock")
    suspend fun unblockUser(@Path("userId") userId: Int): Response<StatusResponse>

    @GET("users/blocked")
    suspend fun getBlockedUsers(): Response<ContactsResponse>

    @GET("users/privacy")
    suspend fun getPrivacySettings(): Response<PrivacySettings>

    @PUT("users/privacy")
    suspend fun updatePrivacySettings(@Body request: PrivacySettings): Response<StatusResponse>

    @GET("users/{userId}/public-key")
    suspend fun getPublicKey(@Path("userId") userId: Int): Response<PublicKeyResponse>

    @PUT("users/me/public-key")
    suspend fun setPublicKey(@Body request: PublicKeyResponse): Response<StatusResponse>

    @POST("users/{userId}/report")
    suspend fun reportUser(@Path("userId") userId: Int, @Body request: ReportRequest): Response<StatusResponse>
    
    @GET("search")
    suspend fun search(@Query("query") query: String): Response<SearchResponse>
}

data class UserResponse(val user: User)

data class UpdateUserRequest(
    val display_name: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val avatar_url: String? = null,
    val banner_url: String? = null,
    val is_artist: Boolean? = null,
    val xp: Int? = null,
    val level: Int? = null,
    val streak: Int? = null,
    val is_premium: Boolean? = null,
    val last_daily_unlock: Long? = null,
    val ads_watched_today: Int? = null,
    val unlocked_lesson_count: Int? = null,
    val native_language: String? = null,
    val target_language: String? = null,
    val streak_freezes: Int? = null,
    val last_streak_activity: Long? = null
)

data class ApiUser(
    val id: Int,
    val username: String,
    val display_name: String,
    val phone_number: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false,
    val bio: String? = null,
    val public_key: String? = null
)

data class ContactsResponse(val contacts: List<ApiUser>)

data class SearchResponse(
    val chats: List<ChatSummary>,
    val contacts: List<ApiUser>,
    val messages: List<ApiMessage> = emptyList(),
    val songs: List<ApiSong> = emptyList(),
    val artists: List<ApiUser> = emptyList()
)

data class PublicKeyResponse(val public_key: String)
