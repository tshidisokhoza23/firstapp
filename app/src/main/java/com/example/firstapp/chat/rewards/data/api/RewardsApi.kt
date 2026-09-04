package com.example.firstapp.chat.rewards.data.api

import com.example.firstapp.chat.model.RewardDashboard
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface RewardsApi {
    @GET("rewards/balance")
    suspend fun getBalance(): Response<RewardsBalanceResponse>

    @GET("rewards/dashboard")
    suspend fun getDashboard(): Response<RewardDashboard>

    @POST("rewards/award")
    suspend fun awardPoints(@Body request: AwardPointsRequest): Response<AwardPointsResponse>
}

data class RewardsBalanceResponse(
    val balance: Double
)

data class AwardPointsRequest(
    val amount: Double,
    val source: String,
    val description: String
)

data class AwardPointsResponse(
    val status: String,
    val transaction_id: String? = null,
    val balance: Double? = null
)
