package com.example.firstapp.common.data.api

import com.example.firstapp.common.model.User
import com.example.firstapp.common.model.PrivacySettings
import com.example.firstapp.chat.data.api.ChatSummary
import com.example.firstapp.chat.data.api.ApiMessage
import com.example.firstapp.music.data.api.ApiSong
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*

interface UserApi {
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @GET("users/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<UserResponse>

    @PUT("users/me")
    suspend fun updateUser(@Body request: UpdateUserRequest): Response<UserResponse>

    @DELETE("users/me")
    suspend fun deleteAccount(): Response<StatusResponse>

    @POST("devices/push-token")
    suspend fun upsertPushToken(@Body request: PushTokenRequest): Response<StatusResponse>

    @POST("users/{userId}/block")
    suspend fun blockUser(@Path("userId") userId: String): Response<StatusResponse>

    @POST("users/{userId}/unblock")
    suspend fun unblockUser(@Path("userId") userId: String): Response<StatusResponse>

    @POST("users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Response<StatusResponse>

    @POST("users/{userId}/unfollow")
    suspend fun unfollowUser(@Path("userId") userId: String): Response<StatusResponse>

    @GET("users/blocked")
    suspend fun getBlockedUsers(): Response<BlockedUsersResponse>

    @GET("users/privacy")
    suspend fun getPrivacySettings(): Response<PrivacySettings>

    @PUT("users/privacy")
    suspend fun updatePrivacySettings(@Body request: PrivacySettings): Response<StatusResponse>

    @GET("users/{userId}/public-key")
    suspend fun getPublicKey(@Path("userId") userId: String): Response<PublicKeyResponse>

    @PUT("users/me/public-key")
    suspend fun setPublicKey(@Body request: PublicKeyResponse): Response<StatusResponse>

    @POST("users/{userId}/report")
    suspend fun reportUser(@Path("userId") userId: String, @Body request: ReportRequest): Response<StatusResponse>

    @POST("users/sync-contacts")
    suspend fun syncContacts(@Body request: SyncContactsRequest): Response<ContactsResponse>
    
    @GET("search")
    suspend fun search(@Query("query") query: String, @Query("type") type: String? = null): Response<SearchResponse>

    // Work Experience
    @GET("profile/experience")
    suspend fun getWorkExperiences(): Response<List<com.example.firstapp.profile.model.WorkExperience>>

    @POST("profile/experience")
    suspend fun addWorkExperience(@Body experience: com.example.firstapp.profile.model.WorkExperience): Response<ExperienceIdResponse>

    @PUT("profile/experience/{experienceId}")
    suspend fun updateWorkExperience(@Path("experienceId") experienceId: String, @Body experience: com.example.firstapp.profile.model.WorkExperience): Response<StatusResponse>

    @DELETE("profile/experience/{experienceId}")
    suspend fun deleteWorkExperience(@Path("experienceId") experienceId: String): Response<StatusResponse>
}

data class UserResponse(val user: User)

data class BlockedUsersResponse(val users: List<ApiUser>)

data class ContactsResponse(val contacts: List<ApiUser>)

data class ExperienceIdResponse(val id: String)

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
    @SerializedName("id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("display_name") val display_name: String,
    @SerializedName("email") val email: String,
    @SerializedName("phone_number") val phone_number: String? = null,
    @SerializedName("avatar_url") val avatar_url: String? = null,
    @SerializedName("is_verified") val is_verified: Boolean = false,
    @SerializedName("bio") val bio: String? = null,
    @SerializedName("public_key") val public_key: String? = null
)

data class SyncContactsRequest(val phone_numbers: List<String>)

data class SearchResponse(
    val chats: List<ChatSummary>,
    val contacts: List<ApiUser>,
    val messages: List<ApiMessage> = emptyList(),
    val songs: List<ApiSong> = emptyList(),
    val artists: List<ApiUser> = emptyList(),
    val channels: List<com.example.firstapp.media.data.api.ApiChannel> = emptyList(),
    val jobs: List<com.example.firstapp.chat.jobs.data.api.ApiJob> = emptyList()
)

data class PublicKeyResponse(val public_key: String)
