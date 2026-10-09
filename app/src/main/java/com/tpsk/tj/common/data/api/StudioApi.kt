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

    @GET("compose/draft")
    suspend fun loadDraft(): Response<ComposeDraftDto>

    @PUT("compose/draft")
    suspend fun saveDraft(@Body body: ComposeMetaDto): Response<StatusResponse>

    @PUT("compose/posts/{id}")
    suspend fun applyCompose(@Path("id") id: String, @Body body: ComposeMetaDto): Response<StatusResponse>

    @GET("compose/people")
    suspend fun searchPeople(@Query("q") query: String): Response<PeopleResponse>

    @GET("posts/{id}/notes")
    suspend fun listNotes(@Path("id") id: String): Response<NotesResponse>

    @POST("posts/{id}/notes")
    suspend fun addNote(@Path("id") id: String, @Body body: NoteRequest): Response<StatusResponse>

    @POST("posts/{id}/notes/{noteId}/vote")
    suspend fun voteNote(@Path("id") id: String, @Path("noteId") noteId: String, @Body body: NoteVoteRequest): Response<StatusResponse>

    @GET("posts/{id}/context")
    suspend fun postContext(@Path("id") id: String): Response<PostContextDto>

    @GET("commissions")
    suspend fun commissions(): Response<CommissionsResponse>

    @POST("commissions")
    suspend fun createCommission(@Body body: CommissionCreateRequest): Response<StatusResponse>

    @PUT("commissions/{id}")
    suspend fun updateCommission(@Path("id") id: String, @Body body: CommissionStatusRequest): Response<StatusResponse>

    @GET("topics")
    suspend fun topics(): Response<TopicsResponse>

    @PUT("topics")
    suspend fun saveTopics(@Body body: TopicsRequest): Response<TopicsResponse>

    @GET("gifs")
    suspend fun searchGifs(@Query("q") query: String): Response<GifsResponse>

    @GET("posts/{id}/edits")
    suspend fun editHistory(@Path("id") id: String): Response<EditsResponse>

    @PUT("posts/{id}")
    suspend fun editPost(@Path("id") id: String, @Body body: EditPostRequest): Response<StatusResponse>

    @GET("reputation/{id}")
    suspend fun reputation(@Path("id") id: String): Response<ReputationDto>

    @GET("export")
    suspend fun exportMine(): Response<ExportResponse>

    @GET("ai/smart-replies")
    suspend fun smartReplies(@Query("post_id") postId: String): Response<SmartRepliesResponse>

    @POST("ai/artist-helper")
    suspend fun artistHelper(@Body body: ArtistHelperRequest): Response<ArtistHelperResponse>
}

data class ComposeMetaDto(
    @SerializedName("content") val content: String = "",
    @SerializedName("mood") val mood: String = "",
    @SerializedName("audience") val audience: String = "public",
    @SerializedName("circle_id") val circleId: String = "",
    @SerializedName("scheduled_at") val scheduledAt: Long? = null,
    @SerializedName("ai_assisted") val aiAssisted: Boolean = false,
    @SerializedName("collaborators") val collaborators: List<String> = emptyList(),
    @SerializedName("splits") val splits: List<SplitInputDto> = emptyList()
)

data class SplitInputDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("percent") val percent: Int
)

data class ComposeDraftDto(
    @SerializedName("content") val content: String = "",
    @SerializedName("mood") val mood: String? = null,
    @SerializedName("audience") val audience: String = "public",
    @SerializedName("circle_id") val circleId: String? = null
)

data class PeopleResponse(@SerializedName("people") val people: List<PersonDto> = emptyList())

data class NoteRequest(@SerializedName("body") val body: String)
data class NotesResponse(@SerializedName("notes") val notes: List<NoteDto> = emptyList())
data class NoteVoteRequest(@SerializedName("helpful") val helpful: Boolean = true)
data class NoteDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("helpful_count") val helpfulCount: Int = 0
)
data class PostContextDto(
    @SerializedName("links") val links: List<String> = emptyList(),
    @SerializedName("notes") val notes: List<NoteDto> = emptyList(),
    @SerializedName("edit_count") val editCount: Int = 0,
    @SerializedName("note") val note: String = ""
)
data class CommissionDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("brief") val brief: String = "",
    @SerializedName("budget_cents") val budgetCents: Int = 0,
    @SerializedName("status") val status: String = "",
    @SerializedName("requester") val requester: String = "",
    @SerializedName("artist") val artist: String = ""
)
data class CommissionsResponse(
    @SerializedName("requests") val requests: List<CommissionDto> = emptyList(),
    @SerializedName("active") val active: List<CommissionDto> = emptyList(),
    @SerializedName("sent") val sent: List<CommissionDto> = emptyList()
)
data class CommissionCreateRequest(
    @SerializedName("artist_id") val artistId: String,
    @SerializedName("brief") val brief: String,
    @SerializedName("budget_cents") val budgetCents: Int = 0
)
data class CommissionStatusRequest(@SerializedName("status") val status: String)
data class TopicsResponse(
    @SerializedName("followed") val followed: List<String> = emptyList(),
    @SerializedName("from_your_posts") val fromYourPosts: List<String> = emptyList()
)
data class TopicsRequest(@SerializedName("topics") val topics: List<String>)
data class GifDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("preview") val preview: String = ""
)
data class GifsResponse(
    @SerializedName("gifs") val gifs: List<GifDto> = emptyList(),
    @SerializedName("error") val error: String? = null
)
data class RevenueSplitDto(
    @SerializedName("post_id") val postId: String = "",
    @SerializedName("text") val text: String = "",
    @SerializedName("percent") val percent: Int = 0,
    @SerializedName("recorded_tip_cents") val recordedTipCents: Int = 0,
    @SerializedName("agreed_share_cents") val agreedShareCents: Int = 0
)
data class EditPostRequest(@SerializedName("content") val content: String)
data class EditsResponse(@SerializedName("edits") val edits: List<EditDto> = emptyList())
data class EditDto(@SerializedName("previous_text") val previousText: String = "")
data class ReputationDto(
    @SerializedName("score") val score: Int = 0,
    @SerializedName("posts") val posts: Int = 0,
    @SerializedName("helpful_replies") val helpfulReplies: Int = 0,
    @SerializedName("followers") val followers: Int = 0,
    @SerializedName("note") val note: String = ""
)
data class ExportResponse(@SerializedName("posts") val posts: List<ExportPostDto> = emptyList())
data class ExportPostDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("content") val content: String = ""
)

data class PersonDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("handle") val handle: String = ""
)

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
    @SerializedName("commission_requests") val commissionRequests: List<CommissionDto> = emptyList(),
    @SerializedName("active_commissions") val activeCommissions: List<CommissionDto> = emptyList(),
    @SerializedName("revenue_splits") val revenueSplits: List<RevenueSplitDto> = emptyList(),
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
