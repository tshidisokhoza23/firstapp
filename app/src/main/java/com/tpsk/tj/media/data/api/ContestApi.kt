package com.tpsk.tj.media.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class ApiContest(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val hashtag: String = "",
    @SerializedName("participant_limit") val participantLimit: Int = 20,
    @SerializedName("ends_at") val endsAt: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("vote_count") val voteCount: Int = 0,
    @SerializedName("entry_count") val entryCount: Int = 0,
    @SerializedName("host_name") val hostName: String? = null,
    @SerializedName("preview_name") val previewName: String? = null,
    @SerializedName("preview_avatar_url") val previewAvatarUrl: String? = null,
    @SerializedName("cover_url") val coverUrl: String? = null,
    val codes: List<String>? = null
)

data class ApiContestListResponse(val contests: List<ApiContest> = emptyList())

data class ApiLeaderboardEntry(
    @SerializedName("post_id") val postId: String = "",
    @SerializedName("author_id") val authorId: String = "",
    @SerializedName("author_name") val authorName: String = "",
    @SerializedName("avatar_url") val avatarUrl: String? = null,
    val votes: Int = 0
)

data class ApiContestDetailResponse(
    val contest: ApiContest? = null,
    val leaderboard: List<ApiLeaderboardEntry> = emptyList()
)

data class CreateContestBody(
    val title: String,
    val description: String,
    val hashtag: String,
    @SerializedName("participant_limit") val participantLimit: Int,
    @SerializedName("duration_days") val durationDays: Int = 7
)

data class RedeemCodeBody(val code: String)
data class CastVoteBody(
    @SerializedName("post_id") val postId: String,
    val votes: Int
)
data class TopUpBody(val amount: Int = 10)
data class PointsResponse(val points: Int = 0)
data class VoteResponse(val votes: Int = 0, val points: Int = 0)
data class RedeemResponse(val ok: Boolean = false)

interface ContestApi {
    @GET("contests")
    suspend fun list(
        @Query("q") query: String? = null,
        @Query("sort") sort: String? = null
    ): Response<ApiContestListResponse>

    @POST("contests")
    suspend fun create(@Body body: CreateContestBody): Response<ApiContest>

    @GET("contests/{hashtag}")
    suspend fun get(@Path("hashtag") hashtag: String): Response<ApiContestDetailResponse>

    @POST("contests/{hashtag}/redeem")
    suspend fun redeem(
        @Path("hashtag") hashtag: String,
        @Body body: RedeemCodeBody
    ): Response<RedeemResponse>

    @POST("contests/votes")
    suspend fun vote(@Body body: CastVoteBody): Response<VoteResponse>

    @GET("contests/points")
    suspend fun points(): Response<PointsResponse>

    @POST("contests/points/topup")
    suspend fun topUp(@Body body: TopUpBody): Response<PointsResponse>
}
