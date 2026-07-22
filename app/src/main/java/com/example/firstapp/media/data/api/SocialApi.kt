package com.example.firstapp.media.data.api

import com.example.firstapp.common.data.api.*
import com.example.firstapp.media.model.Post
import retrofit2.Response
import retrofit2.http.*

interface SocialApi {
    @GET("media/feed")
    suspend fun getFeed(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
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
        @Query("page") page: Int = 1, 
        @Query("following") following: Boolean = false,
        @Query("channel_id") channelId: String? = null,
        @Query("user_id") authorId: String? = null
    ): Response<List<ApiPost>>

    @POST("posts")
    suspend fun createPost(@Body request: CreatePostRequest): Response<Post>

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
    suspend fun getStories(): Response<StoriesResponse>

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

    @POST("channels-create")
    suspend fun createChannel(@Body request: CreateChannelRequest): Response<ApiChannel>

    @POST("channels/{channelId}/subscribe")
    suspend fun subscribeToChannel(@Path("channelId") channelId: String): Response<StatusResponse>

    @POST("channels/{channelId}/mute")
    suspend fun muteChannel(@Path("channelId") channelId: String, @Body request: MuteRequest): Response<StatusResponse>

    @POST("channels/{channelId}/admins")
    suspend fun addChannelAdmin(@Path("channelId") channelId: String, @Body request: UserIdRequest): Response<StatusResponse>

    @HTTP(method = "DELETE", path = "channels/{channelId}/admins", hasBody = true)
    suspend fun removeChannelAdmin(@Path("channelId") channelId: String, @Body request: UserIdRequest): Response<StatusResponse>
}

data class FeedResponse(val items: List<ApiFeedItem>)
data class RepliesResponse(val items: List<ApiReplyItem>)
data class BoostRequest(val total_budget_cents: Int, val duration_days: Int)
data class CreateMediaReplyRequest(val reply_text: String)
data class ToggleRepliesLockRequest(val replies_locked: Boolean)
data class CreateMediaReplyResponse(val type: String, val reply: ApiMediaReply)

data class ApiExternalAdPlaceholder(
    val network: String?,
    val placement: String?
)

data class ApiFeedItem(
    val type: String,
    val post: ApiMediaPost?,
    val placeholder: ApiExternalAdPlaceholder?,
    val ad_placement_id: String? = null,
    val format: String? = null
)

data class ApiReplyItem(
    val type: String,
    val reply: ApiMediaReply?,
    val placeholder: ApiExternalAdPlaceholder?
)

data class ApiMediaPost(
    val id: String,
    val user_id: String,
    val author_name: String,
    val author_handle: String,
    val author_avatar_url: String?,
    val is_verified_author: Boolean = false,
    val content_text: String,
    val media_urls: List<String>?,
    val is_boosted: Boolean,
    val total_budget_cents: Int,
    val boost_expires_at: String?,
    val replies_locked: Boolean,
    val created_at: String?
)

data class ApiMediaReply(
    val id: String,
    val media_post_id: String,
    val user_id: String,
    val author_name: String,
    val handle: String,
    val reply_text: String,
    val created_at: String?
)

data class UserIdRequest(val user_id: String)

data class CreateChannelRequest(
    val name: String,
    val description: String
)

data class CreatePostRequest(
    val content: String,
    val media_urls: List<String> = emptyList(),
    val post_type: String = "text_only",
    val location: String? = null,
    val tags: List<String> = emptyList(),
    val poll: CreatePollRequest? = null,
    val channel_id: String? = null
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
    val id: String,
    val username: String,
    val display_name: String,
    val avatar_url: String?,
    val is_verified: Boolean
)

data class ApiPostReply(
    val id: String,
    val author_id: String,
    val post_id: String,
    val content: String,
    val created_at: Long?,
    val likes: Long,
    val is_liked: Boolean,
    val is_reposted: Boolean,
    val parent_reply_id: String?,
    val author: ApiPostAuthor?,
    val nested_replies: List<ApiPostReply>?,
    val ad: ApiAdvert?,
    val post_type: String?
)

data class ApiPost(
    val id: String,
    val author_id: String,
    val content: String,
    val media_urls: List<String>?,
    val created_at: Long?,
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
    val is_followed: Boolean,
    val replies_locked: Boolean = false,
    val location: String?,
    val tags: List<String>?,
    val author: ApiPostAuthor?,
    val replies: List<ApiPostReply>?,
    val poll: ApiPostPoll?,
    val ad: ApiAdvert?
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
data class PostReplyResponse(val reply: ApiPostReply)

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
    val created_by: String? = null,
    val category: String? = null,
    val last_post_text: String? = null,
    val last_post_image_url: String? = null,
    val last_post_time: String? = null
)

data class ChannelsResponse(val channels: List<ApiChannel>)
