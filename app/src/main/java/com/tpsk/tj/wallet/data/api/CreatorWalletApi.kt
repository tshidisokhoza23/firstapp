package com.tpsk.tj.wallet.data.api

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface CreatorWalletApi {
    @GET("wallet")
    suspend fun balance(): Response<CreatorWalletDto>

    @GET("wallet/earnings")
    suspend fun earnings(): Response<CreatorEarningsDto>

    @POST("wallet/purchases")
    suspend fun verifyPurchase(@Body body: VerifyPurchaseRequest): Response<CreatorWalletDto>

    @POST("wallet/tips")
    suspend fun tip(@Body body: TipRequest): Response<TipResultDto>

    @PUT("wallet/bank")
    suspend fun saveBank(@Body body: BankRequest): Response<BankAccountDto>

    @POST("wallet/payouts")
    suspend fun requestPayout(@Body body: PayoutRequest): Response<PayoutDto>
}

data class CreatorWalletDto(
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("purchased_coins") val purchasedCoins: Long = 0,
    @SerializedName("ad_points") val adPoints: Long = 0,
    @SerializedName("diamond_debt_cents") val diamondDebtCents: Long = 0
)

data class CreatorEarningsDto(
    @SerializedName("held_diamonds") val heldDiamonds: Long = 0,
    @SerializedName("available_diamonds") val availableDiamonds: Long = 0,
    @SerializedName("withdrawable_cents") val withdrawableCents: Long = 0,
    @SerializedName("debt_cents") val debtCents: Long = 0,
    @SerializedName("can_withdraw") val canWithdraw: Boolean = false,
    @SerializedName("minimum_cents") val minimumCents: Int = 50000,
    @SerializedName("minimum_diamonds") val minimumDiamonds: Int = 7143,
    @SerializedName("purchased_coins") val purchasedCoins: Long = 0
)

data class VerifyPurchaseRequest(
    val platform: String,
    @SerializedName("product_id") val productId: String,
    @SerializedName("purchase_token") val purchaseToken: String = "",
    @SerializedName("signed_transaction") val signedTransaction: String = ""
)

data class TipRequest(
    @SerializedName("post_id") val postId: String,
    val coins: Int,
    @SerializedName("idempotency_key") val idempotencyKey: String
)

data class TipResultDto(
    @SerializedName("tip_id") val tipId: String = "",
    val coins: Int = 0,
    @SerializedName("purchased_coins") val purchasedCoins: Long = 0
)

data class BankRequest(
    @SerializedName("legal_name") val legalName: String,
    @SerializedName("account_number") val accountNumber: String,
    @SerializedName("bank_code") val bankCode: String
)

data class BankAccountDto(
    @SerializedName("account_last4") val accountLast4: String = "",
    val verified: Boolean = false,
    @SerializedName("resolved_account_name") val resolvedAccountName: String = ""
)

data class PayoutRequest(
    @SerializedName("idempotency_key") val idempotencyKey: String
)

data class PayoutDto(
    val id: String = "",
    val status: String = "",
    @SerializedName("gross_amount_cents") val grossAmountCents: Int = 0,
    @SerializedName("diamonds_count") val diamondsCount: Long = 0
)
