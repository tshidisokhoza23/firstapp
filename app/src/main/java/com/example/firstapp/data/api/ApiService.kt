package com.example.firstapp.data.api

import com.example.firstapp.model.*
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*
/**
 * Interface for API calls.
 * Base URL: http://10.0.2.2:8080/api/v1/ (for Android emulator)
 */
interface ApiService {

    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: TokenRefreshRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: TokenRefreshRequest): Response<StatusResponse>

    // User endpoints
    @GET("users/me")
    suspend fun getCurrentUser(): Response<UserResponse>

    @PUT("users/me")
    suspend fun updateUser(@Body request: UpdateUserRequest): Response<UserResponse>

    @POST("devices/push-token")
    suspend fun upsertPushToken(@Body request: PushTokenRequest): Response<StatusResponse>

    // Contacts and Messages
    @GET("contacts")
    suspend fun getContacts(): Response<ContactsResponse>

    @POST("contacts/sync")
    suspend fun syncContacts(@Body request: SyncContactsRequest): Response<ContactsResponse>

    @GET("search")
    suspend fun search(@Query("query") query: String): Response<SearchResponse>

    @GET("chats")
    suspend fun getChats(): Response<ChatsResponse>

    @POST("chats")
    suspend fun createChat(@Body request: CreateChatRequest): Response<Any>

    @GET("calls")
    suspend fun getCalls(): Response<CallsResponse>

    @GET("conversations/{contactId}/messages")
    suspend fun getMessages(@Path("contactId") contactId: Int, @Query("page") page: Int = 1): Response<MessagesResponse>

    @POST("conversations/{contactId}/messages")
    suspend fun sendMessage(@Path("contactId") contactId: Int, @Body request: SendMessageRequest): Response<MessageResponse>

    @POST("conversations/{contactId}/read")
    suspend fun markAsRead(@Path("contactId") contactId: Int): Response<StatusResponse>

    @Multipart
    @POST("uploads")
    suspend fun uploadMedia(@Part file: MultipartBody.Part): Response<UploadResponse>

    // Jobs
    @GET("jobs")
    suspend fun getJobs(@Query("page") page: Int = 1): Response<JobsResponse>

    @POST("jobs/{jobId}/apply")
    suspend fun applyForJob(@Path("jobId") jobId: Int): Response<StatusResponse>

    @GET("applications")
    suspend fun getJobApplications(): Response<JobApplicationsResponse>

    @DELETE("applications/{applicationId}")
    suspend fun withdrawApplication(@Path("applicationId") applicationId: Int): Response<StatusResponse>

    // Adverts
    @GET("adverts")
    suspend fun getAdverts(): Response<AdvertsResponse>

    @POST("adverts/{advertId}/view")
    suspend fun markAdvertViewed(@Path("advertId") advertId: Int): Response<StatusResponse>

    // Channels
    @GET("channels")
    suspend fun getChannels(): Response<ChannelsResponse>

    @POST("channels/{channelId}/subscribe")
    suspend fun subscribeToChannel(@Path("channelId") channelId: Int): Response<StatusResponse>

    // Shorts
    @GET("shorts")
    suspend fun getShorts(): Response<ShortsResponse>

    @POST("shorts/{shortId}/like")
    suspend fun likeShort(@Path("shortId") shortId: Int): Response<ShortLikeResponse>

    @POST("shorts/{shortId}/subscribe")
    suspend fun subscribeToShortCreator(@Path("shortId") shortId: Int): Response<ShortSubscribeResponse>

    // Songs
    @GET("songs")
    suspend fun getSongs(@Query("category") category: String? = null): Response<SongsResponse>

    @POST("songs/{songId}/like")
    suspend fun likeSong(@Path("songId") songId: Int): Response<StatusResponse>

    @POST("songs/{songId}/play")
    suspend fun incrementSongPlay(@Path("songId") songId: Int): Response<StatusResponse>

    // Posts
    @GET("posts")
    suspend fun getPosts(@Query("page") page: Int = 1): Response<PostsResponse>

    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequest): Response<Post>

    @DELETE("posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: Int): Response<StatusResponse>

    @POST("posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: Int): Response<StatusResponse>

    @POST("posts/{postId}/repost")
    suspend fun repostPost(@Path("postId") postId: Int): Response<StatusResponse>

    @POST("posts/{postId}/bookmark")
    suspend fun bookmarkPost(@Path("postId") postId: Int): Response<StatusResponse>

    @POST("posts/{postId}/view")
    suspend fun viewPost(@Path("postId") postId: Int): Response<StatusResponse>

    @POST("posts/{postId}/replies")
    suspend fun createPostReply(
        @Path("postId") postId: Int,
        @Body request: CreatePostReplyRequest
    ): Response<PostReplyResponse>

    @POST("post-replies/{replyId}/like")
    suspend fun likePostReply(@Path("replyId") replyId: Int): Response<StatusResponse>

    @POST("post-replies/{replyId}/repost")
    suspend fun repostPostReply(@Path("replyId") replyId: Int): Response<StatusResponse>

    @POST("post-replies/{replyId}/bookmark")
    suspend fun bookmarkPostReply(@Path("replyId") replyId: Int): Response<StatusResponse>

    // Stories
    @GET("stories")
    suspend fun getStories(): Response<StoriesResponse>

    @POST("stories")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<CreateStoryResponse>

    // Ride options
    @GET("ride-options")
    suspend fun getRideOptions(): Response<RideOptionsResponse>

    // Destinations
    @GET("destinations")
    suspend fun getDestinations(): Response<DestinationsResponse>

    @POST("destinations")
    suspend fun saveDestination(@Body request: SaveDestinationRequest): Response<SaveDestinationResponse>
}

// Request/Response models
data class RegisterRequest(val username: String, val display_name: String, val phone_number: String, val password: String)
data class LoginRequest(val username: String, val password: String)
data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("refresh_token") val refreshToken: String = "",
    @SerializedName("user") val user: User
)
data class UserResponse(val user: User)
data class UpdateUserRequest(val display_name: String? = null, val bio: String? = null, val location: String? = null)
data class PushTokenRequest(val token: String, val device_id: String, val platform: String = "android")

data class ApiUser(
    val id: Int,
    val username: String,
    val display_name: String,
    val phone_number: String? = null,
    val avatar_url: String? = null,
    val is_verified: Boolean = false
)

data class ContactsResponse(val contacts: List<ApiUser>)

data class SyncContactsRequest(val phone_numbers: List<String>)
data class CreateChatRequest(val contact_id: Int)

data class SearchResponse(val chats: List<ChatSummary>, val contacts: List<ApiUser>)

data class ChatSummary(
    val contact_id: Int,
    val contact_name: String,
    val last_message: String,
    val time: String,
    val unread_count: Int,
    val avatar_url: String? = null
)

data class ChatsResponse(val chats: List<ChatSummary>)

data class ApiCallLog(
    val id: Int,
    val session_id: String,
    val caller_id: Int,
    val callee_id: Int,
    val contact_id: Int,
    val contact_name: String,
    val contact_avatar: String? = null,
    val call_type: String,
    val status: String,
    val duration_seconds: Int,
    val started_at: String,
    val ended_at: String? = null
)

data class CallsResponse(val calls: List<ApiCallLog>)

data class ApiMessage(
    val id: Int,
    val direction: String,
    val body: String,
    val created_at: String,
    val read_at: String? = null,
    val message_type: String? = null,
    val media_url: String? = null,
    val duration: String? = null,
    val file_name: String? = null,
    val file_size: String? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class MessagesResponse(val messages: List<ApiMessage>, val page: Int)

data class SendMessageRequest(
    val body: String,
    val message_type: String? = null,
    val media_url: String? = null,
    val duration: String? = null,
    val file_name: String? = null,
    val file_size: String? = null,
    val contact_name: String? = null,
    val contact_phone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class MessageResponse(val message: ApiMessage)

data class UploadResponse(
    val url: String,
    val file_name: String,
    val file_size: Long
)

data class StatusResponse(val status: String)

data class ApiJob(
    val id: Int,
    val title: String,
    val company_name: String,
    val company_logo_url: String?,
    val location: String,
    val salary_range: String?,
    val job_type: String?,
    val posted_time: String?,
    val description: String?,
    val requirements: List<String>?,
    val is_verified: Boolean,
    val created_by: Int?,
    val is_applied: Boolean,
    val is_saved: Boolean
)

data class JobsResponse(val jobs: List<ApiJob>, val page: Int)

data class ApiJobApplication(
    val id: Int,
    val user_id: Int,
    val job_id: Int,
    val applied_date: String,
    val status: String,
    val title: String? = null,
    val company_name: String? = null,
    val location: String? = null
)

data class JobApplicationsResponse(val applications: List<ApiJobApplication>)

data class ApiAdvert(
    val id: Int,
    val advertiser_name: String,
    val advertiser_id: String?,
    val title: String?,
    val time: String?,
    val avatar_url: String?,
    val content_url: String?,
    val content_text: String,
    val action_text: String?,
    val target_url: String?,
    val is_sponsored: Boolean,
    val impressions: Long,
    val clicks: Long,
    val is_native_ad: Boolean,
    val is_viewed: Boolean,
    val created_at: String? = null
)

data class AdvertsResponse(val adverts: List<ApiAdvert>)

data class ApiChannel(
    val id: Int,
    val name: String,
    val description: String?,
    val followers: Long,
    val avatar_url: String?,
    val is_verified: Boolean,
    val is_subscribed: Boolean,
    val category: String?,
    val last_post_text: String?,
    val last_post_image_url: String?,
    val last_post_time: String?
)

data class ChannelsResponse(val channels: List<ApiChannel>)

data class ApiShort(
    val id: Int,
    val creator_name: String,
    val description: String,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val video_url: String?,
    val thumbnail_url: String?,
    val is_liked: Boolean,
    val is_subscribed: Boolean
)

data class ShortsResponse(val shorts: List<ApiShort>)

data class ShortLikeResponse(
    val status: String,
    val is_liked: Boolean,
    val likes: Long
)

data class ShortSubscribeResponse(
    val status: String,
    val is_subscribed: Boolean
)

data class ApiSong(
    val id: Int,
    val title: String,
    val artist: String,
    val artist_id: String?,
    val album_name: String?,
    val album_art_url: String?,
    val audio_url: String?,
    val duration: String?,
    val category: String?,
    val release_date: String?,
    val play_count: Long,
    val is_liked: Boolean,
    val is_explicit: Boolean
)

data class SongsResponse(val songs: List<ApiSong>)

data class ApiPostAuthor(
    val id: Int,
    val username: String,
    val display_name: String,
    val avatar_url: String?,
    val is_verified: Boolean
)

data class ApiPostReply(
    val id: Int,
    val author_id: Int,
    val post_id: Int,
    val content: String,
    val created_at: String?,
    val likes: Long,
    val is_liked: Boolean,
    val is_reposted: Boolean,
    val parent_reply_id: Int?,
    val author: ApiPostAuthor?,
    val nested_replies: List<ApiPostReply>?
)

data class ApiPost(
    val id: Int,
    val author_id: Int,
    val content: String,
    val media_urls: List<String>?,
    val created_at: String?,
    val likes: Long,
    val replies_count: Long,
    val shares: Long,
    val views: Long,
    val post_type: String?,
    val has_thread: Boolean,
    val thread_count: Int,
    val is_liked: Boolean,
    val is_reposted: Boolean,
    val is_bookmarked: Boolean,
    val location: String?,
    val tags: List<String>?,
    val author: ApiPostAuthor?,
    val replies: List<ApiPostReply>?
)

data class PostsResponse(val posts: List<ApiPost>, val page: Int)

data class CreatePostRequest(val content: String, val media_urls: List<String> = emptyList(), val post_type: String = "text_only", val location: String? = null, val tags: List<String> = emptyList())
data class CreatePostReplyRequest(val content: String, val parent_reply_id: Int? = null)
data class PostReplyResponse(val reply: ApiPostReply)

data class ApiStoryItem(
    val id: Int,
    val content: String?,
    val content_url: String?,
    val story_type: String?,
    val created_at: String?,
    val duration_ms: Int?,
    val is_viewed: Boolean?
)

data class ApiStoryResponse(
    val user_id: Int,
    val user_name: String,
    val avatar_url: String?,
    val is_verified: Boolean,
    val stories: List<ApiStoryItem>
)

data class StoriesResponse(val stories: List<ApiStoryResponse>)

data class CreateStoryRequest(
    val content: String = "",
    val content_url: String? = null,
    val story_type: String = "text",
    val duration_ms: Int = 5000
)

data class CreateStoryResponse(val story: ApiStoryItem)

data class RideOptionsResponse(val ride_options: List<RideOption>)

data class DestinationsResponse(val destinations: List<Destination>)

data class SaveDestinationRequest(
    val name: String,
    val address: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val is_favorite: Boolean = false,
    val destination_type: String = "recent",
    val place_id: String? = null
)

data class SaveDestinationResponse(val destination: Destination)

// Update User model to match backend
data class User(
    val id: Int,
    val username: String,
    val display_name: String,
    val phone_number: String? = null,
    val created_at: String,
    val avatar_url: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val is_verified: Boolean = false
)
