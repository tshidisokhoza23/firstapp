package com.example.firstapp.media.data.api

import com.example.firstapp.common.data.api.*
import com.example.firstapp.media.model.Post
import retrofit2.Response
import retrofit2.http.*

interface SocialApi {
    @GET("posts")
    suspend fun getPosts(
        @Query("page") page: Int = 1, 
        @Query("following") following: Boolean = false,
        @Query("channel_id") channelId: Int? = null
    ): Response<PostsResponse>

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

    @POST("posts/{postId}/poll/vote")
    suspend fun votePoll(@Path("postId") postId: Int, @Body request: VotePollRequest): Response<StatusResponse>

    @POST("users/{userId}/follow")
    suspend fun followUser(@Path("userId") userId: Int): Response<StatusResponse>

    @POST("users/{userId}/unfollow")
    suspend fun unfollowUser(@Path("userId") userId: Int): Response<StatusResponse>

    @GET("trending")
    suspend fun getTrendingTopics(): Response<TrendingTopicsResponse>

    @GET("suggested-users")
    suspend fun getSuggestedUsers(): Response<ContactsResponse>

    @GET("stories")
    suspend fun getStories(): Response<StoriesResponse>

    @POST("stories")
    suspend fun createStory(@Body request: CreateStoryRequest): Response<CreateStoryResponse>

    @POST("stories/{storyId}/view")
    suspend fun markStoryViewed(@Path("storyId") storyId: Int): Response<StatusResponse>

    @DELETE("stories/{storyId}")
    suspend fun deleteStory(@Path("storyId") storyId: Int): Response<StatusResponse>

    @POST("users/{userId}/mute")
    suspend fun muteUser(@Path("userId") userId: Int, @Body request: MuteRequest): Response<StatusResponse>

    @GET("stories/{storyId}/viewers")
    suspend fun getStoryViewers(@Path("storyId") storyId: Int): Response<ViewersResponse>

    @GET("channels")
    suspend fun getChannels(): Response<ChannelsResponse>

    @POST("channels-create")
    suspend fun createChannel(@Body request: CreateChannelRequest): Response<ApiChannel>

    @POST("channels/{channelId}/subscribe")
    suspend fun subscribeToChannel(@Path("channelId") channelId: Int): Response<StatusResponse>

    @POST("channels/{channelId}/admins")
    suspend fun addChannelAdmin(@Path("channelId") channelId: Int, @Body request: UserIdRequest): Response<StatusResponse>

    @HTTP(method = "DELETE", path = "channels/{channelId}/admins", hasBody = true)
    suspend fun removeChannelAdmin(@Path("channelId") channelId: Int, @Body request: UserIdRequest): Response<StatusResponse>
}

data class UserIdRequest(val user_id: Int)

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
    val channel_id: Int? = null
)

data class CreatePollRequest(
    val options: List<String>,
    val duration_h: Int = 24
)

data class VotePollRequest(val option_id: Int)
data class CreatePostReplyRequest(val content: String, val parent_reply_id: Int? = null)
data class CreateStoryRequest(
    val content: String = "",
    val content_url: String? = null,
    val story_type: String = "text",
    val duration_ms: Int = 5000
)

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
    val is_followed: Boolean,
    val location: String?,
    val tags: List<String>?,
    val author: ApiPostAuthor?,
    val replies: List<ApiPostReply>?,
    val poll: ApiPostPoll?
)

data class ApiPostPoll(
    val post_id: Int,
    val expires_at: String,
    val options: List<ApiPollOption>,
    val user_vote: Int?
)

data class ApiPollOption(
    val id: Int,
    val post_id: Int,
    val option_text: String,
    val vote_count: Long
)

data class PostsResponse(val posts: List<ApiPost>, val page: Int)

data class TrendingTopic(val tag: String, val post_count: Long)
data class TrendingTopicsResponse(val topics: List<TrendingTopic>)
data class PostReplyResponse(val reply: ApiPostReply)

data class ApiStoryItem(
    val id: Int,
    val content: String?,
    val content_url: String?,
    val story_type: String?,
    val created_at: Long?,
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

data class CreateStoryResponse(val story: ApiStoryItem)

data class ViewersResponse(val viewers: List<ApiViewer>)
data class ApiViewer(val id: Int, val display_name: String, val avatar_url: String?, val viewed_at: String)

data class ApiChannel(
    val id: Int,
    val name: String,
    val description: String?,
    val followers: Long,
    val avatar_url: String?,
    val is_verified: Boolean,
    val is_subscribed: Boolean,
    val is_admin: Boolean = false,
    val created_by: Int? = null,
    val category: String? = null,
    val last_post_text: String? = null,
    val last_post_image_url: String? = null,
    val last_post_time: String? = null
)

data class ChannelsResponse(val channels: List<ApiChannel>)
