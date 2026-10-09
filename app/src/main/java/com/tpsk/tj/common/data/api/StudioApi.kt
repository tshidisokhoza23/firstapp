package com.tpsk.tj.common.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface StudioApi {
    @GET("circles")
    suspend fun listCircles(): Response<CirclesResponse>

    @POST("circles")
    suspend fun createCircle(@Body body: CreateCircleRequest): Response<CircleDto>

    @DELETE("circles/{id}")
    suspend fun deleteCircle(@Path("id") id: String): Response<StatusResponse>

    @GET("creator/dashboard")
    suspend fun dashboard(@Query("range") range: String): Response<CreatorDashboardDto>

    @PUT("creator/settings")
    suspend fun updateSettings(@Body body: CreatorSettingsRequest): Response<CreatorSettingsDto>
}

data class CirclesResponse(
    @SerializedName("circles") val circles: List<CircleDto> = emptyList()
)

data class CreateCircleRequest(
    @SerializedName("name") val name: String,
    @SerializedName("is_temporary") val isTemporary: Boolean = false
)

data class CircleDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("member_count") val memberCount: Int = 0,
    @SerializedName("is_temporary") val isTemporary: Boolean = false,
    @SerializedName("expires_at") val expiresAt: String? = null
)

data class CreatorDashboardDto(
    @SerializedName("earnings_this_month_cents") val earningsThisMonthCents: Int = 0,
    @SerializedName("earnings_all_time_cents") val earningsAllTimeCents: Int = 0,
    @SerializedName("earnings_range_cents") val earningsRangeCents: Int = 0,
    @SerializedName("tips_count") val tipsCount: Int = 0,
    @SerializedName("tips_coins") val tipsCoins: Int = 0,
    @SerializedName("tips_cents") val tipsCents: Int = 0,
    @SerializedName("post_views") val postViews: Int = 0,
    @SerializedName("new_followers") val newFollowers: Int = 0,
    @SerializedName("accept_tips") val acceptTips: Boolean = true,
    @SerializedName("accept_commissions") val acceptCommissions: Boolean = false,
    @SerializedName("is_artist") val isArtist: Boolean = false,
    @SerializedName("posts") val posts: List<CreatorPostStatDto> = emptyList(),
    @SerializedName("note") val note: String? = null
)

data class CreatorPostStatDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("views") val views: Int = 0,
    @SerializedName("likes") val likes: Int = 0
)

data class CreatorSettingsRequest(
    @SerializedName("accept_tips") val acceptTips: Boolean,
    @SerializedName("accept_commissions") val acceptCommissions: Boolean
)

data class CreatorSettingsDto(
    @SerializedName("accept_tips") val acceptTips: Boolean = true,
    @SerializedName("accept_commissions") val acceptCommissions: Boolean = false
)
