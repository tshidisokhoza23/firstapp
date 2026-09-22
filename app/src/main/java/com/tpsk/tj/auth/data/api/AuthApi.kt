package com.tpsk.tj.auth.data.api

import com.tpsk.tj.common.data.api.*
import retrofit2.Response
import retrofit2.http.*

interface AuthApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refresh(@Body request: TokenRefreshRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(@Body request: TokenRefreshRequest): Response<StatusResponse>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<StatusResponse>

    @POST("auth/request-otp")
    suspend fun requestOTP(@Body request: Map<String, String>): Response<StatusResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOTP(@Body request: Map<String, String>): Response<StatusResponse>

    @GET("auth/availability")
    suspend fun checkAvailability(
        @Query("username") username: String?,
        @Query("email") email: String?,
        @Query("phone") phone: String?
    ): Response<Map<String, Boolean>>
}
