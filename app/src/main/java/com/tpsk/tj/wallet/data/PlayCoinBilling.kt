package com.tpsk.tj.wallet.data

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicReference

@Singleton
class PlayCoinBilling @Inject constructor(
    @ApplicationContext context: Context
) {
    private val pending = AtomicReference<CompletableDeferred<Purchase>?>(null)
    private val listener = PurchasesUpdatedListener { result, purchases ->
        val waiter = pending.getAndSet(null) ?: return@PurchasesUpdatedListener
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val bought = purchases?.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                if (bought != null) waiter.complete(bought)
                else waiter.completeExceptionally(IllegalStateException("Purchase is pending store approval"))
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> waiter.completeExceptionally(IllegalStateException("Purchase cancelled"))
            else -> waiter.completeExceptionally(IllegalStateException("Store purchase was not completed"))
        }
    }
    private val client = BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    suspend fun connect() {
        if (client.isReady) return
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) cont.resume(Unit)
                    else cont.resumeWithException(IllegalStateException("Google Play billing is unavailable"))
                }
                override fun onBillingServiceDisconnected() = Unit
            })
        }
    }

    suspend fun products(): List<ProductDetails> {
        connect()
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            PRODUCT_IDS.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
        ).build()
        return suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { result, details ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(details)
                } else cont.resume(emptyList())
            }
        }
    }

    suspend fun buy(activity: Activity, product: ProductDetails): Purchase {
        connect()
        val waiter = CompletableDeferred<Purchase>()
        pending.set(waiter)
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build())
            )
            .build()
        val launched = client.launchBillingFlow(activity, flow)
        if (launched.responseCode != BillingClient.BillingResponseCode.OK) {
            pending.set(null)
            throw IllegalStateException("Could not open Google Play")
        }
        return waiter.await()
    }

    suspend fun unconsumed(): List<Purchase> {
        connect()
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        return suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { result, purchases ->
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    cont.resume(purchases.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED })
                } else cont.resume(emptyList())
            }
        }
    }

    suspend fun consume(token: String) {
        if (token.isBlank()) return
        connect()
        val params = ConsumeParams.newBuilder().setPurchaseToken(token).build()
        suspendCancellableCoroutine { cont ->
            client.consumeAsync(params) { _, _ -> cont.resume(Unit) }
        }
    }

    companion object {
        val PRODUCT_IDS = listOf("tj_coins_100", "tj_coins_500", "tj_coins_1000")
    }
}
