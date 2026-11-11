package com.tpsk.tj.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.tpsk.tj.mingle.data.api.MingleApi
import com.tpsk.tj.mingle.data.api.MingleDtosKt
import com.tpsk.tj.mingle.data.api.MingleLocationRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GooglePlayBillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MingleApi
) : PurchasesUpdatedListener {

    private val TAG = "GooglePlayBilling"
    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _newBalance = MutableStateFlow<Int?>(null)
    val newBalance: StateFlow<Int?> = _newBalance

    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError: StateFlow<String?> = _purchaseError

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Google Play Billing Client connected successfully.")
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected. Retrying connection...")
                startConnection()
            }
        })
    }

    fun launchPurchase(activity: Activity, productId: String) {
        if (!billingClient.isReady) {
            _purchaseError.value = "Billing service is connecting. Please try again."
            startConnection()
            return
        }

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )

                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()

                billingClient.launchBillingFlow(activity, billingFlowParams)
            } else {
                _purchaseError.value = "Product unavailable in Google Play Store."
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && !purchases.isNullOrEmpty()) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.i(TAG, "User canceled Google Play purchase flow.")
        } else {
            _purchaseError.value = "Purchase failed: ${billingResult.debugMessage}"
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            val productId = purchase.products.firstOrNull() ?: "tj_coins_10"
            val purchaseToken = purchase.purchaseToken
            val orderId = purchase.orderId ?: UUID.randomUUID().toString()
            val idempotencyKey = "play-verify-${purchaseToken.hashCode()}"

            // Verify with backend via verification API endpoint
            scope.launch {
                try {
                    // Send purchase token to backend verification endpoint
                    val response = api.extendFeed(mapOf(
                        "product_id" to productId,
                        "purchase_token" to purchaseToken,
                        "order_id" to orderId,
                        "idempotency_key" to idempotencyKey
                    ))

                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        _newBalance.value = body.balance.toInt()

                        // Consume token asynchronously after backend credits coins
                        val consumeParams = ConsumeParams.newBuilder()
                            .setPurchaseToken(purchaseToken)
                            .build()

                        billingClient.consumeAsync(consumeParams) { consumeResult, _ ->
                            if (consumeResult.responseCode == BillingClient.BillingResponseCode.OK) {
                                Log.d(TAG, "Consumable token consumed successfully.")
                            }
                        }
                    } else {
                        _purchaseError.value = "Backend purchase verification failed."
                    }
                } catch (e: Exception) {
                    _purchaseError.value = "Network error during purchase verification."
                }
            }
        }
    }

    fun clearError() {
        _purchaseError.value = null
    }
}
