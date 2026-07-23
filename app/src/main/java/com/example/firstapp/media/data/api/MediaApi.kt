package com.example.firstapp.media.data.api

import com.google.gson.annotations.SerializedName
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
}

data class ApiAdvert(
    val id: String?,
    val advertiser_name: String?,
    val advertiser_id: String?,
    val title: String?,
    val time: String?,
    val avatar_url: String?,
    val content_url: String?,
    val content_text: String?,
    val action_text: String?,
    val target_url: String?,
    val is_sponsored: Boolean = false,
    val impressions: Long = 0,
    val clicks: Long = 0,
    val is_native_ad: Boolean = false,
    val is_viewed: Boolean = false,
    val created_at: Long? = null
)

data class AdvertsResponse(val adverts: List<ApiAdvert>)
