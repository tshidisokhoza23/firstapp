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

interface NearbyApi {
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
}
