package com.example.firstapp.media.data.api

import com.example.firstapp.common.data.api.*
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.*
import java.util.ArrayList

interface SocialApi {
    @GET("media/feed")
    suspend fun getFeed(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("category_id") categoryId: String? = null
    ): Response<FeedResponse>

    @GET("media/posts/{id}/replies")
    suspend fun getReplies(
        @Path("id") postId: String
    ): Response<RepliesResponse>

    @POST("media/posts/{id}/boost")
    suspend fun boostPost(
        @Path("id") postId: String,
        @Body request: BoostRequest
    ): Response<StatusResponse>

    @POST("media/posts/{id}/replies")
    suspend fun createMediaReply(
        @Path("id") postId: String,
        @Body request: CreateMediaReplyRequest
    ): Response<CreateMediaReplyResponse>

    @PUT("media/posts/{id}/replies/lock")
    suspend fun setRepliesLock(
        @Path("id") postId: String,
        @Body request: ToggleRepliesLockRequest
    ): Response<StatusResponse>

    @GET("posts")
    suspend fun getPosts(
        @Query("page") page: Int, 
        @Query("following") following: Boolean,
        @Query("channel_id") channelId: String? = null,
        @Query("user_id") authorId: String,
        @Query("type") feedType: String,
        @Query("category_id") categoryId: String? = null
    ): Response<List<ApiPost>>

    @GET("media/feed/shorts")
    suspend fun getShorts(
        @Query("page") page: Int,
        @Query("category_id") categoryId: String? = null
    ): Response<List<ApiPost>>

    @GET("media/feed/media")
    suspend fun getMediaFeed(
        @Query("page") page: Int,
        @Query("category_id") categoryId: String? = null
    ): Response<List<ApiPost>>

    @POST("media/ads/impression-revenue")
    suspend fun trackAdRevenue(@Body request: AdRevenueRequest): Response<StatusResponse>

    @POST("engagement")
    suspend fun trackEngagement(@Body request: EngagementRequest): Response<StatusResponse>

    @GET("posts/{postId}")
    suspend fun getPost(@Path("postId") postId: String): Response<ApiPost>

    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequest): Response<ApiPost>

    @DELETE("posts/{postId}")
    suspend fun deletePost(@Path("postId") postId: String): Response<StatusResponse>

    @POST("posts/{postId}/like")
    suspend fun likePost(@Path("postId") postId: String): Response<StatusResponse>

    @POST("posts/{postId}/repost")
    suspend fun repostPost(@Path("postId") postId: String): Response<StatusResponse>

    @POST("posts/{postId}/bookmark")
    suspend fun bookmarkPost(@Path("postId") postId: String): Response<StatusResponse>

    @POST("posts/{postId}/view")
    suspend fun viewPost(@Path("postId") postId: String): Response<StatusResponse>

    @POST("posts/{postId}/share")
    suspend fun sharePost(@Path("postId") postId: String): Response<StatusResponse>

    @POST("posts/{postId}/replies")
    suspend fun createPostReply(
        @Path("postId") postId: String,
        @Body request: CreatePostReplyRequest
    ): Response<PostReplyResponse>

    @POST("post-replies/{replyId}/like")
    suspend fun likePostReply(@Path("replyId") replyId: String): Response<StatusResponse>

    @POST("post-replies/{replyId}/repost")
    suspend fun repostPostReply(@Path("replyId") replyId: String): Response<StatusResponse>

    @POST("post-replies/{replyId}/bookmark")
    suspend fun bookmarkPostReply(@Path("replyId") replyId: String): Response<StatusResponse>

    @POST("posts/{postId}/poll/vote")
    suspend fun votePoll(@Path("postId") postId: String, @Body request: VotePollRequest): Response<StatusResponse>

    @POST("users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: String): Response<StatusResponse>

    @POST("users/{userId}/unfollow")
    suspend fun unfollowUser(@Path("userId") userId: String): Response<StatusResponse>

    @GET("trending")
    suspend fun getTrendingTopics(): Response<TrendingTopicsResponse>

    @GET("suggested-users")
    suspend fun getSuggestedUsers(): Response<ContactsResponse>

    @GET("chats/stories")
    suspend fun getStories(@Query("since") since: Long? = null): Response<StoriesResponse>

    @POST("chats/stories")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<CreateStoryResponse>

    @POST("chats/stories/{storyId}/view")
    suspend fun markStoryViewed(@Path("storyId") storyId: String): Response<StatusResponse>

    @DELETE("chats/stories/{storyId}")
    suspend fun deleteStory(@Path("storyId") storyId: String): Response<StatusResponse>

    @POST("users/{userId}/mute")
    suspend fun muteUser(@Path("userId") userId: String, @Body request: MuteRequest): Response<StatusResponse>

    @GET("chats/stories/{storyId}/viewers")
    suspend fun getStoryViewers(@Path("storyId") storyId: String): Response<ViewersResponse>

    @GET("channels")
    suspend fun getChannels(): Response<ChannelsResponse>

    @GET("chats/channels")
    suspend fun getChatsChannels(): Response<ChannelsResponse>

    @POST("chats/channels")
    suspend fun createChannel(@Body request: CreateChannelRequest): Response<ApiChannel>

    @POST("chats/channels/{channelId}/subscribe")
    suspend fun subscribeToChannel(@Path("channelId") channelId: String): Response<StatusResponse>

    @POST("chats/channels/{channelId}/mute")
    suspend fun muteChannel(@Path("channelId") channelId: String, @Body request: MuteRequest): Response<StatusResponse>

    @POST("chats/channels/{channelId}/admins")
    suspend fun addChannelAdmin(@Path("channelId") channelId: String, @Body request: UserIdRequest): Response<StatusResponse>

    @POST("chats/channels/{channelId}/admin-posting")
    suspend fun toggleAdminPosting(@Path("channelId") channelId: String, @Body request: ToggleAdminPostingRequest): Response<StatusResponse>

    @HTTP(method = "DELETE", path = "chats/channels/{channelId}/admins", hasBody = true)
    suspend fun removeChannelAdmin(@Path("channelId") channelId: String, @Body request: UserIdRequest): Response<StatusResponse>
}

data class FeedResponse(val items: List<ApiFeedItem>)
data class AdRevenueRequest(val post_id: String, val revenue: Double, val ad_unit_id: String)
data class RepliesResponse(val items: List<ApiReplyItem>)
data class BoostRequest(val total_budget_cents: Int, val duration_days: Int)
data class CreateMediaReplyRequest(val reply_text: String)
data class ToggleRepliesLockRequest(val replies_locked: Boolean)
data class CreateMediaReplyResponse(val type: String, val reply: ApiPost)

data class ApiExternalAdPlaceholder(
    val network: String?,
    val placement: String?
)

data class ApiFeedItem(
    val type: String,
    val post: ApiPost?,
    val placeholder: ApiExternalAdPlaceholder?,
    val ad_placement_id: String? = null,
    val format: String? = null
)

data class ApiReplyItem(
    val type: String,
    val reply: ApiPost? = null,
    val placeholder: ApiExternalAdPlaceholder? = null
)

data class ApiPost(
    val id: String?,
    @SerializedName("author_id") val author_id: String?,
    @SerializedName("user_id") val user_id: String?,
    val content: String?,
    @SerializedName("content_text") val content_text: String?,
    @SerializedName("media_urls") val media_urls: List<String>?,
    @SerializedName("media_type") val media_type: String? = null,
    @SerializedName("post_type") val post_type: String? = null,
    @SerializedName("alt_text") val alt_text: String? = null,
    @SerializedName("thumbnail_url") val thumbnail_url: String? = null,
    @SerializedName("aspect_ratio") val aspect_ratio: Double? = null,
    @SerializedName("duration_sec") val duration_sec: Int? = null,
    val blurhash: String? = null,
    @SerializedName("parent_post_id") val parent_post_id: String? = null,
    @SerializedName("root_post_id") val root_post_id: String? = null,
    @SerializedName("repost_of_id") val repost_of_id: String? = null,
    val likes: Long = 0,
    @SerializedName("like_count") val like_count: Long = 0,
    @SerializedName("replies_count") val replies_count: Long = 0,
    @SerializedName("reply_count") val reply_count: Long = 0,
    val shares: Long = 0,
    @SerializedName("repost_count") val repost_count: Long = 0,
    @SerializedName("actual_shares") val actual_shares: Long = 0,
    val views: Long = 0,
    @SerializedName("view_count") val view_count: Long = 0,
    @SerializedName("quote_count") val quote_count: Long = 0,
    @SerializedName("is_boosted") val is_boosted: Boolean = false,
    @SerializedName("total_budget_cents") val total_budget_cents: Int = 0,
    @SerializedName("boost_expires_at") val boost_expires_at: String? = null,
    @SerializedName("replies_locked") val replies_locked: Boolean = false,
    val status: String? = "active",
    @SerializedName("is_pinned") val is_pinned: Boolean = false,
    @SerializedName("reply_settings") val reply_settings: String? = "everyone",
    @SerializedName("created_at") val created_at: Long?,
    @SerializedName("updated_at") val updated_at: Long? = null,
    @SerializedName("author_name") val author_name: String? = null,
    @SerializedName("author_handle") val author_handle: String? = null,
    @SerializedName("author_avatar_url") val author_avatar_url: String? = null,
    @SerializedName("author_email") val author_email: String? = null,
    @SerializedName("is_liked") val is_liked: Boolean = false,
    @SerializedName("is_reposted") val is_reposted: Boolean = false,
    @SerializedName("is_bookmarked") val is_bookmarked: Boolean = false,
    @SerializedName("is_followed") val is_followed: Boolean = false,
    @SerializedName("has_thread") val has_thread: Boolean = false,
    @SerializedName("thread_count") val thread_count: Int = 0,
    val location: String? = null,
    val tags: List<String>? = null,
    val author: ApiPostAuthor? = null,
    val replies: List<ApiPost>? = null,
    @SerializedName("original_post") val original_post: ApiPost? = null,
    val poll: ApiPostPoll? = null,
    val ad: ApiAdvert? = null
)

data class UserIdRequest(val user_id: String)

data class CreateChannelRequest(
    val name: String,
    val description: String
)

data class ToggleAdminPostingRequest(
    val admins_can_post: Boolean
)

data class CreatePostRequest(
    val content: String,
    val media_urls: List<String>,
    val post_type: String = "text_only",
    val media_type: String? = null,
    val location: String? = null,
    val tags: List<String>,
    val poll: CreatePollRequest? = null,
    val channel_id: String? = null,
    @SerializedName("parent_post_id") val parent_post_id: String? = null,
    @SerializedName("root_post_id") val root_post_id: String? = null,
    @SerializedName("category_id") val category_id: String? = null,
    @SerializedName("repost_of_id") val repost_of_id: String? = null,
    @SerializedName("thumbnail_url") val thumbnail_url: String? = null
)

data class CreatePollRequest(
    val options: List<String>,
    val duration_h: Int = 24
)

data class VotePollRequest(val option_id: String)
data class CreatePostReplyRequest(val content: String, val parent_reply_id: String? = null)

data class CreateStoryRequest(
    val content: String = "",
    val content_url: String? = null,
    val story_type: String = "text",
    val duration_ms: Int = 5000
)

data class ApiPostAuthor(
    val id: String?,
    val username: String,
    val display_name: String,
    val email: String,
    val avatar_url: String?,
    val is_verified: Boolean = false
)

data class ApiPostPoll(
    val post_id: String,
    val expires_at: Long?,
    val options: List<ApiPollOption>,
    val user_vote: String?
)

data class ApiPollOption(
    val id: String,
    val post_id: String,
    val option_text: String,
    val vote_count: Long
)

data class TrendingTopic(val tag: String, val post_count: Long)
data class TrendingTopicsResponse(val trending: List<TrendingTopic>)
data class PostReplyResponse(val reply: ApiPost)

data class ApiStoryItem(
    val id: String,
    val content: String?,
    val content_url: String?,
    val story_type: String?,
    val created_at: Long?,
    val duration_ms: Int?,
    val is_viewed: Boolean?
)

data class ApiStoryResponse(
    val user_id: String,
    val user_name: String,
    @SerializedName("phone_number") val phone_number: String? = null,
    val avatar_url: String?,
    val is_verified: Boolean,
    val is_muted: Boolean = false,
    val stories: List<ApiStoryItem>
)

data class StoriesResponse(val stories: List<ApiStoryResponse>)

data class CreateStoryResponse(val story: ApiStoryItem)

data class ViewersResponse(val viewers: List<ApiViewer>)
data class ApiViewer(val id: String, val display_name: String, val avatar_url: String?, val viewed_at: String)

data class ApiChannel(
    val id: String,
    val name: String,
    val description: String?,
    val followers: Long,
    val avatar_url: String?,
    val is_verified: Boolean,
    val is_subscribed: Boolean,
    val is_admin: Boolean = false,
    @SerializedName("admins_can_post") val admins_can_post: Boolean = false,
    val created_by: String? = null,
    val category: String? = null,
    val last_post_text: String? = null,
    val last_post_image_url: String? = null,
    val last_post_time: Long? = null,
    val conversation_id: String? = null
)

data class ChannelsResponse(val channels: List<ApiChannel>)

data class EngagementRequest(
    val post_id: String,
    val event_type: String,
    val category: String,
    val watch_time_ms: Int? = null,
    val completed: Boolean? = null
)
