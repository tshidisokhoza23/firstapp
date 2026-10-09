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

    @GET("posts/{id}/edits")
    suspend fun editHistory(@Path("id") id: String): Response<EditsResponse>

    @PUT("posts/{id}")
    suspend fun editPost(@Path("id") id: String, @Body body: EditPostRequest): Response<StatusResponse>

    @GET("reputation/{id}")
    suspend fun reputation(@Path("id") id: String): Response<ReputationDto>

    @GET("export")
    suspend fun exportMine(): Response<ExportResponse>
}

data class ComposeMetaDto(
    @SerializedName("content") val content: String = "",
    @SerializedName("mood") val mood: String = "",
    @SerializedName("audience") val audience: String = "public",
    @SerializedName("circle_id") val circleId: String = "",
    @SerializedName("scheduled_at") val scheduledAt: Long? = null,
    @SerializedName("ai_assisted") val aiAssisted: Boolean = false,
    @SerializedName("collaborators") val collaborators: List<String> = emptyList()
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
data class NoteDto(
    @SerializedName("id") val id: String = "",
    @SerializedName("body") val body: String = "",
    @SerializedName("author") val author: String = ""
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
