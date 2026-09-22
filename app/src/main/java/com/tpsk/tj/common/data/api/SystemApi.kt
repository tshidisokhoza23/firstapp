package com.tpsk.tj.common.data.api

import retrofit2.Response
import retrofit2.http.*

interface SystemApi {
    @POST("dev/reset-and-seed")
    suspend fun devResetAndSeed(@Body request: DevResetRequest): Response<DevResetResponse>
}
