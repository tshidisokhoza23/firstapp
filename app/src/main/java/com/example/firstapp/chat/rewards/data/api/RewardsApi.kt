package com.example.firstapp.chat.rewards.data.api

import com.example.firstapp.common.data.api.StatusResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RewardsApi {
    @GET("chats/rewards/balance")
    suspend fun getBalance(): Response<RewardsBalanceResponse>

    @POST("chats/rewards/award")
    suspend fun awardPoints(@Body request: AwardPointsRequest): Response<StatusResponse>
}

data class RewardsBalanceResponse(
    val balance: Double
)

data class AwardPointsRequest(
    val amount: Double,
    val source: String,
    val description: String
)
