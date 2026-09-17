package com.example.firstapp.mingle.data.api

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface MingleApi {
    @GET("mingle/me")
    suspend fun getMe(): Response<MingleProfileResponse>

    @PUT("mingle/me")
    suspend fun updateProfile(@Body request: MingleUpdateProfileRequest): Response<MingleProfileResponse>

    @POST("mingle/location")
    suspend fun updateLocation(@Body request: MingleLocationRequest): Response<MingleProfileResponse>

    @GET("mingle/feed")
    suspend fun getFeed(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius_km") radiusKm: Int,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<MingleFeedResponse>

    @POST("mingle/swipe")
    suspend fun swipe(@Body request: MingleSwipeRequest): Response<MingleSwipeResultDto>

    @POST("mingle/swipes/sync")
    suspend fun syncSwipes(@Body request: MingleSwipeSyncRequest): Response<MingleSwipeSyncResponse>

    @POST("mingle/rewind")
    suspend fun rewind(): Response<MingleRewindResultDto>

    @POST("mingle/like/{userId}")
    suspend fun like(@Path("userId") userId: String): Response<MingleSwipeResultDto>

    @Multipart
    @POST("mingle/photos")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Response<MinglePhotoResponse>

    @DELETE("mingle/photos/{photoId}")
    suspend fun deletePhoto(@Path("photoId") photoId: String): Response<Map<String, String>>

    @GET("mingle/activity")
    suspend fun getActivity(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<MingleActivityResponse>

    @POST("mingle/matches/{userId}/chat")
    suspend fun openChat(@Path("userId") userId: String): Response<MingleChatResponse>
}
