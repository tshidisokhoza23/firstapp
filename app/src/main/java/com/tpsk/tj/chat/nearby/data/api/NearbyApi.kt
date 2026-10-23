package com.tpsk.tj.chat.nearby.data.api

import retrofit2.Response
import retrofit2.http.*

data class ApiNearbyPost(
    val id: String,
    val user_id: String,
    val author_name: String,
    val author_avatar_url: String? = null,
    val content: String,
    val area_name: String = "Nearby",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val like_count: Int = 0,
    val reply_count: Int = 0,
    val is_liked: Boolean = false,
    val created_at: String? = null,
    val distance_km: Double = 1.0,
    val is_resolved: Boolean = false,
    val replies: List<ApiNearbyReply>? = null
)

data class ApiNearbyReply(
    val id: String,
    val post_id: String,
    val user_id: String,
    val author_name: String,
    val author_avatar_url: String? = null,
    val content: String,
    val created_at: String? = null
)

data class NearbyPostsResponse(
    val posts: List<ApiNearbyPost>
)

data class NearbyRepliesResponse(
    val replies: List<ApiNearbyReply>
)

data class CreateNearbyPostRequest(
    val content: String,
    val area_name: String = "Nearby",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

data class CreateNearbyReplyRequest(
    val content: String
)

data class ToggleLikeResponse(
    val is_liked: Boolean,
    val like_count: Int
)

data class ApiArea(
    val name: String = "",
    val town: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val post_count: Int = 0,
    val display_name: String = ""
)

data class AreasResponse(
    val areas: List<ApiArea> = emptyList()
)

data class UpdateNearbyPostRequest(
    val content: String,
    val area_name: String
)

data class ResolveNearbyPostRequest(
    val is_resolved: Boolean
)

data class ApiChatRequest(
    val id: String,
    val sender_id: String,
    val sender_name: String = "",
    val sender_avatar_url: String? = null,
    val recipient_id: String = "",
    val recipient_name: String = "",
    val post_id: String = "",
    val post_reference_snippet: String = "",
    val inquiry_message: String = "",
    val created_at: String? = null
)

data class ChatRequestsResponse(
    val requests: List<ApiChatRequest> = emptyList()
)

data class CreateChatRequestBody(
    val post_id: String,
    val message: String
)

data class RespondChatRequestBody(
    val accept: Boolean
)

interface NearbyApi {
    @GET("nearby/areas/search")
    suspend fun searchAreas(
        @Query("q") query: String
    ): Response<AreasResponse>

    @GET("nearby/areas/suggest")
    suspend fun suggestAreas(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null
    ): Response<AreasResponse>

    @GET("nearby/posts")
    suspend fun getPosts(
        @Query("lat") lat: Double? = null,
        @Query("lng") lng: Double? = null,
        @Query("radius") radius: Int? = null,
        @Query("area") area: String? = null
    ): Response<NearbyPostsResponse>

    @POST("nearby/posts")
    suspend fun createPost(
        @Body request: CreateNearbyPostRequest
    ): Response<ApiNearbyPost>

    @POST("nearby/posts/{id}/like")
    suspend fun toggleLike(
        @Path("id") id: String
    ): Response<ToggleLikeResponse>

    @GET("nearby/posts/{id}/replies")
    suspend fun getReplies(
        @Path("id") id: String
    ): Response<NearbyRepliesResponse>

    @POST("nearby/posts/{id}/replies")
    suspend fun createReply(
        @Path("id") id: String,
        @Body request: CreateNearbyReplyRequest
    ): Response<ApiNearbyReply>

    @GET("nearby/posts/mine")
    suspend fun getMyPosts(): Response<NearbyPostsResponse>

    @PATCH("nearby/posts/{id}")
    suspend fun updatePost(
        @Path("id") id: String,
        @Body request: UpdateNearbyPostRequest
    ): Response<Map<String, String>>

    @DELETE("nearby/posts/{id}")
    suspend fun deletePost(
        @Path("id") id: String
    ): Response<Unit>

    @POST("nearby/posts/{id}/resolve")
    suspend fun resolvePost(
        @Path("id") id: String,
        @Body request: ResolveNearbyPostRequest
    ): Response<Map<String, Boolean>>

    @GET("nearby/requests")
    suspend fun getChatRequests(): Response<ChatRequestsResponse>

    @POST("nearby/requests")
    suspend fun createChatRequest(
        @Body request: CreateChatRequestBody
    ): Response<ApiChatRequest>

    @POST("nearby/requests/{id}/respond")
    suspend fun respondChatRequest(
        @Path("id") id: String,
        @Body request: RespondChatRequestBody
    ): Response<Map<String, String>>
}
