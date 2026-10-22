package com.tpsk.tj.media.data.api

import retrofit2.Response
import retrofit2.http.*

interface NewsApi {
    @GET("api/v1/news")
    suspend fun getNews(
        @Query("category") category: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<ApiNewsResponse>

    @GET("api/v1/news/{id}/comments")
    suspend fun getComments(
        @Path("id") articleId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 30
    ): Response<ApiNewsCommentsResponse>

    @POST("api/v1/news/{id}/comments")
    suspend fun createComment(
        @Path("id") articleId: String,
        @Body request: CreateNewsCommentRequest
    ): Response<ApiNewsComment>

    @POST("api/v1/news/{id}/like")
    suspend fun toggleLike(
        @Path("id") articleId: String
    ): Response<ToggleNewsLikeResponse>
}

data class ApiNewsResponse(
    val articles: List<ApiNewsArticle>,
    val next_cursor: String? = null
)

data class ApiNewsArticle(
    val id: String,
    val source: String,
    val category: String,
    val title: String,
    val summary: String,
    val image_url: String? = null,
    val original_url: String,
    val like_count: Int = 0,
    val comment_count: Int = 0,
    val published_at: String,
    val created_at: String? = null,
    val has_liked: Boolean? = false
)

data class ApiNewsCommentsResponse(
    val comments: List<ApiNewsComment>,
    val next_cursor: String? = null
)

data class ApiNewsComment(
    val id: String,
    val article_id: String,
    val user_id: String,
    val content: String,
    val created_at: String,
    val display_name: String? = null,
    val avatar_url: String? = null
)

data class CreateNewsCommentRequest(
    val content: String
)

data class ToggleNewsLikeResponse(
    val article_id: String,
    val has_liked: Boolean,
    val like_count: Int
)
