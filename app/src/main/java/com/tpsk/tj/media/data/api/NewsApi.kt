package com.tpsk.tj.media.data.api

import retrofit2.Response
import retrofit2.http.*

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
    val published_at: String? = null,
    val created_at: String? = null,
    val has_liked: Boolean? = false
)

data class NewsResponse(
    val articles: List<ApiNewsArticle>?,
    val next_cursor: String? = null
)

data class ToggleLikeResponse(
    val article_id: String,
    val has_liked: Boolean,
    val like_count: Int
)

interface NewsApi {
    @GET("api/v1/news")
    suspend fun getNews(
        @Query("category") category: String? = null,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<NewsResponse>

    @POST("api/v1/news/{id}/like")
    suspend fun toggleLike(
        @Path("id") id: String
    ): Response<ToggleLikeResponse>
}
