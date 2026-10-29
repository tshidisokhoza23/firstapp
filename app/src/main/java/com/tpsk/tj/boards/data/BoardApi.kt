package com.tpsk.tj.boards.data

import retrofit2.Response
import retrofit2.http.*

interface BoardApi {
    @GET("boards/{board}")
    suspend fun list(@Path("board") board: String, @Query("q") query: String? = null): Response<ListingsResponse>

    @GET("boards/{board}/mine")
    suspend fun mine(@Path("board") board: String): Response<ListingsResponse>

    @POST("boards/{board}")
    suspend fun create(@Path("board") board: String, @Body body: CreateListingRequest): Response<ListingResponse>

    @PATCH("boards/{board}/{id}")
    suspend fun update(@Path("board") board: String, @Path("id") id: String, @Body body: UpdateListingRequest): Response<StatusBody>

    @POST("boards/{board}/{id}/interest")
    suspend fun interest(@Path("board") board: String, @Path("id") id: String, @Body body: InterestRequest): Response<InterestResponse>
}

data class ListingsResponse(val listings: List<BoardListing> = emptyList())
data class ListingResponse(val listing: BoardListing)
data class BoardListing(
    val id: String,
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priceLabel: String = "",
    val area: String = "",
    val photoUrl: String = "",
    val status: String = "active",
    val sellerName: String = ""
)
data class CreateListingRequest(
    val title: String,
    val description: String,
    val priceLabel: String,
    val area: String,
    val photoUrl: String
)
data class UpdateListingRequest(val status: String)
data class InterestRequest(val note: String)
data class InterestResponse(val conversationId: String = "")
data class StatusBody(val success: Boolean = false)
