package com.example.firstapp.media.data.api

import com.example.firstapp.common.data.api.*
import com.example.firstapp.media.data.api.ApiPostReply
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface MediaApi {
    @Multipart
    @POST("uploads")
    suspend fun uploadMedia(
        @Part file: MultipartBody.Part,
        @Part("folder") folder: okhttp3.RequestBody? = null
    ): Response<UploadResponse>

    @GET("adverts")
    suspend fun getAdverts(): Response<AdvertsResponse>

    @GET("adverts/viewed")
    suspend fun getViewedAdverts(): Response<AdvertsResponse>

    @POST("adverts/{advertId}/view")
    suspend fun markAdvertViewed(@Path("advertId") advertId: Int): Response<StatusResponse>

    @GET("shorts")
    suspend fun getShorts(): Response<ShortsResponse>

    @GET("shorts/search")
    suspend fun searchShorts(@Query("query") query: String, @Query("page") page: Int = 1): Response<ShortsResponse>

    @POST("shorts/{shortId}/like")
    suspend fun likeShort(@Path("shortId") shortId: Int): Response<ShortLikeResponse>

    @POST("shorts/{shortId}/subscribe")
    suspend fun subscribeToShortCreator(@Path("shortId") shortId: Int): Response<ShortSubscribeResponse>

    @POST("shorts")
    suspend fun createShort(@Body request: CreateShortRequest): Response<CreateShortResponse>

    @POST("shorts/{shortId}/repost")
    suspend fun repostShort(@Path("shortId") shortId: Int): Response<StatusResponse>

    @POST("shorts/{shortId}/bookmark")
    suspend fun bookmarkShort(@Path("shortId") shortId: Int): Response<StatusResponse>

    @POST("shorts/{shortId}/replies")
    suspend fun replyToShort(@Path("shortId") shortId: Int, @Body request: CreateShortReplyRequest): Response<StatusResponse>

    @GET("shorts/{shortId}/replies")
    suspend fun getShortReplies(@Path("shortId") shortId: Int): Response<ShortRepliesResponse>
}

data class CreateShortRequest(val description: String, val video_url: String, val thumbnail_url: String)
data class CreateShortReplyRequest(val content: String)
data class CreateShortResponse(val id: Long)

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

data class ShortRepliesResponse(val replies: List<ApiPostReply>)
