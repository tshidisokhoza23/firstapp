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

    @POST("dev/reset-ads")
    suspend fun resetAds(): Response<StatusResponse>

    @GET("adverts/viewed")
    suspend fun getViewedAdverts(): Response<AdvertsResponse>

    @POST("adverts/{advertId}/view")
    suspend fun markAdvertViewed(@Path("advertId") advertId: String): Response<StatusResponse>

    @POST("adverts/{advertId}/click")
    suspend fun trackAdvertClick(@Path("advertId") advertId: String): Response<StatusResponse>

    @POST("adverts/{advertId}/share")
    suspend fun trackAdvertShare(@Path("advertId") advertId: String): Response<StatusResponse>

    @POST("adverts/{advertId}/like")
    suspend fun likeAdvert(@Path("advertId") advertId: String): Response<StatusResponse>

    @GET("shorts")
    suspend fun getShorts(): Response<ShortsResponse>

    @GET("shorts/feed")
    suspend fun getShortsFeed(@Query("page") page: Int = 1): Response<ShortsFeedResponse>

    @GET("users/{userId}/shorts")
    suspend fun getShortsByUserId(@Path("userId") userId: String): Response<ShortsResponse>

    @GET("shorts/search")
    suspend fun searchShorts(@Query("query") query: String, @Query("page") page: Int = 1): Response<ShortsResponse>

    @POST("shorts/{shortId}/like")
    suspend fun likeShort(@Path("shortId") shortId: String): Response<ShortLikeResponse>

    @POST("shorts/{shortId}/subscribe")
    suspend fun subscribeToShortCreator(@Path("shortId") shortId: String): Response<ShortSubscribeResponse>

    @POST("shorts")
    suspend fun createShort(@Body request: CreateShortRequest): Response<CreateShortResponse>

    @POST("shorts/{shortId}/repost")
    suspend fun repostShort(@Path("shortId") shortId: String): Response<StatusResponse>

    @POST("shorts/{shortId}/bookmark")
    suspend fun bookmarkShort(@Path("shortId") shortId: String): Response<StatusResponse>

    @POST("shorts/{shortId}/share")
    suspend fun trackShortShare(@Path("shortId") shortId: String): Response<StatusResponse>

    @POST("shorts/{shortId}/replies")
    suspend fun replyToShort(@Path("shortId") shortId: String, @Body request: CreateShortReplyRequest): Response<StatusResponse>

    @GET("shorts/{shortId}/replies")
    suspend fun getShortReplies(@Path("shortId") shortId: String): Response<ShortRepliesResponse>

    @POST("shorts/replies/{replyId}/like")
    suspend fun likeShortReply(@Path("replyId") replyId: String): Response<StatusResponse>
}

data class ApiAdvert(
    val id: String,
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
    val created_at: Long? = null
)

data class AdvertsResponse(val adverts: List<ApiAdvert>)

data class ApiShort(
    val id: String,
    val creator_id: String,
    val creator_name: String,
    val creator_username: String? = null,
    val creator_avatar_url: String? = null,
    val description: String,
    val likes: Long,
    val comments: Long,
    val shares: Long,
    val video_url: String?,
    val thumbnail_url: String?,
    val is_liked: Boolean,
    val is_subscribed: Boolean,
    val is_bookmarked: Boolean = false
)

data class ShortsResponse(val shorts: List<ApiShort>)

data class ApiShortFeedItem(
    val type: String,
    val short: ApiShort?,
    val placeholder: ApiExternalAdPlaceholder?
)

data class ShortsFeedResponse(val items: List<ApiShortFeedItem>, val page: Int = 1)

data class ShortLikeResponse(
    val status: String,
    val is_liked: Boolean,
    val likes: Long
)

data class ShortSubscribeResponse(
    val status: String,
    val is_subscribed: Boolean
)
