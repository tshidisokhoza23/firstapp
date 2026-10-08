package com.tpsk.tj.wallet.data

import com.tpsk.tj.wallet.data.api.BankAccountDto
import com.tpsk.tj.wallet.data.api.BankRequest
import com.tpsk.tj.wallet.data.api.CreatorEarningsDto
import com.tpsk.tj.wallet.data.api.CreatorWalletApi
import com.tpsk.tj.wallet.data.api.CreatorWalletDto
import com.tpsk.tj.wallet.data.api.PayoutDto
import com.tpsk.tj.wallet.data.api.PayoutRequest
import com.tpsk.tj.wallet.data.api.TipRequest
import com.tpsk.tj.wallet.data.api.TipResultDto
import com.tpsk.tj.wallet.data.api.VerifyPurchaseRequest
import org.json.JSONObject
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreatorWalletRepository @Inject constructor(
    private val api: CreatorWalletApi
) {
    suspend fun balance(): CreatorWalletDto = api.balance().bodyOrThrow()
    suspend fun earnings(): CreatorEarningsDto = api.earnings().bodyOrThrow()

    suspend fun verifyAndroidPurchase(productId: String, purchaseToken: String): CreatorWalletDto {
        return api.verifyPurchase(
            VerifyPurchaseRequest(platform = "ANDROID", productId = productId, purchaseToken = purchaseToken)
        ).bodyOrThrow()
    }

    suspend fun tip(postId: String, coins: Int, key: String): TipResultDto {
        return api.tip(TipRequest(postId, coins, key)).bodyOrThrow()
    }

    suspend fun saveBank(legalName: String, accountNumber: String, bankCode: String): BankAccountDto {
        return api.saveBank(BankRequest(legalName, accountNumber, bankCode)).bodyOrThrow()
    }

    suspend fun requestPayout(key: String): PayoutDto {
        return api.requestPayout(PayoutRequest(key)).bodyOrThrow()
    }
}

private fun <T> Response<T>.bodyOrThrow(): T {
    val payload = body()
    if (isSuccessful && payload != null) return payload
    val raw = errorBody()?.string().orEmpty()
    val message = runCatching { JSONObject(raw).optString("error") }.getOrDefault("")
    throw IllegalStateException(message.ifBlank { "Request rejected" })
}
