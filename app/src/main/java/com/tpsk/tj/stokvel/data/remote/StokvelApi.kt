package com.tpsk.tj.stokvel.data.remote

import com.tpsk.tj.stokvel.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface StokvelApi {

    @GET("stokvels")
    suspend fun getStokvels(): Response<List<StokvelGroup>>

    @POST("stokvels")
    suspend fun createStokvel(@Body group: CreateGroupRequestDto): Response<StokvelGroup>

    @POST("stokvels/{id}/cycles/advance")
    suspend fun advanceCycle(@Path("id") groupId: String): Response<StokvelGroup>

    @POST("stokvels/{id}/shuffle-order")
    suspend fun shuffleOrder(@Path("id") groupId: String): Response<List<StokvelMember>>

    @POST("stokvels/{id}/swap-request")
    suspend fun requestTurnSwap(
        @Path("id") groupId: String,
        @Body request: SwapRequestDto
    ): Response<SwapRequest>

    @POST("stokvels/{id}/swap-confirm")
    suspend fun confirmTurnSwap(
        @Path("id") groupId: String,
        @Body request: SwapConfirmRequestDto
    ): Response<Unit>

    @Multipart
    @POST("stokvels/{id}/contributions")
    suspend fun uploadContribution(
        @Path("id") groupId: String,
        @Header("X-Idempotency-Key") idempotencyKey: String?,
        @Part("member_id") memberId: RequestBody,
        @Part("amount") amount: RequestBody,
        @Part("method") method: RequestBody,
        @Part("voucher_serial") voucherSerial: RequestBody?,
        @Part file: MultipartBody.Part?
    ): Response<StokvelContribution>

    @PATCH("stokvels/{id}/contributions/{contrib_id}/verify")
    suspend fun verifyContribution(
        @Path("id") groupId: String,
        @Path("contrib_id") contributionId: String,
        @Body verification: ContributionVerificationDto
    ): Response<StokvelContribution>

    @POST("stokvels/{id}/rfqs")
    suspend fun createRfq(
        @Path("id") groupId: String,
        @Body request: CreateRfqRequestDto
    ): Response<StokvelRfq>

    @GET("stokvels/{id}/rfqs/{rfq_id}/bids")
    suspend fun getBidsForRfq(
        @Path("id") groupId: String,
        @Path("rfq_id") rfqId: String
    ): Response<List<SupplierBid>>

    @POST("stokvels/{id}/rfqs/{rfq_id}/bids/{bid_id}/accept")
    suspend fun acceptBid(
        @Path("id") groupId: String,
        @Path("rfq_id") rfqId: String,
        @Path("bid_id") bidId: String
    ): Response<NonCustodialFulfillmentResponse>
}

data class CreateGroupRequestDto(
    val name: String,
    val type: String,
    val monthlyTargetAmount: Double,
    val cycleInterval: String = "monthly",
    val creatorUserId: String,
    val bankName: String? = null,
    val accountNumberHint: String? = null,
    val payshapId: String? = null,
    val initialMembers: List<InitialMemberDto>? = null
)

data class InitialMemberDto(
    val userId: String,
    val role: String,
    val payoutOrder: Int? = null
)

data class SwapRequestDto(
    val requesterMemberId: String,
    val targetMemberId: String
)

data class SwapConfirmRequestDto(
    val swapRequestId: String,
    val actorMemberId: String
)

data class ContributionVerificationDto(
    val verifierUserId: String,
    val action: String, // "approve" or "reject"
    val rejectionReason: String? = null
)

data class CreateRfqRequestDto(
    val targetRegion: String,
    val itemsManifest: List<RfqItem>,
    val maxBudget: Double? = null,
    val deliveryPreference: String = "any",
    val expiresInHours: Int = 72
)
