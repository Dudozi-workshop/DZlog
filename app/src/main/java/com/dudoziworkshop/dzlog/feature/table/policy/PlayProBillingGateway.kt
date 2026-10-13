package com.dudoziworkshop.dzlog.feature.table.policy

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Dormant Play Billing integration. The app MUST NOT instantiate this while
 * MonetizationRolloutConfig.mode == ALL_UNLOCKED.
 *
 * verifyPurchaseToken MUST call a trusted backend backed by Google Play Developer API.
 * Never trust a client-side PURCHASED signal alone to enable Pro.
 */
internal class PlayProBillingGateway(
    context: Context,
    private val scope: CoroutineScope,
    private val verifyPurchaseToken: suspend (token: String) -> Boolean,
    private val onEvent: (PlayPurchaseEvent) -> Unit,
) {
    init {
        check(MonetizationRolloutConfig.purchasesEnabled) {
            "Play Billing cannot be started while every feature is unlocked."
        }
    }

    private var availableProduct: ProductDetails? = null
    private val listener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases.orEmpty().forEach(::handlePurchase)
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onEvent(PlayPurchaseEvent.Cancelled)
        } else {
            onEvent(PlayPurchaseEvent.Error(result.responseCode))
        }
    }
    private val client = BillingClient.newBuilder(context.applicationContext)
        .setListener(listener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    onEvent(PlayPurchaseEvent.Error(result.responseCode))
                    return
                }
                queryProduct()
                restorePurchases()
            }

            override fun onBillingServiceDisconnected() {
                onEvent(PlayPurchaseEvent.ConnectionLost)
            }
        })
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(MonetizationRolloutConfig.PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product)).build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onEvent(PlayPurchaseEvent.Error(result.responseCode))
                return@queryProductDetailsAsync
            }
            availableProduct = queryResult.productDetailsList.firstOrNull {
                it.productId == MonetizationRolloutConfig.PRO_PRODUCT_ID
            }
            onEvent(
                if (availableProduct == null) PlayPurchaseEvent.ProductUnavailable
                else PlayPurchaseEvent.ProductReady
            )
        }
    }

    fun beginPurchase(activity: Activity): Boolean {
        val product = availableProduct ?: return false
        val offer = product.oneTimePurchaseOfferDetailsList?.firstOrNull() ?: return false
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product)
            .setOfferToken(offer.offerToken)
            .build()
        val result = client.launchBillingFlow(activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onEvent(PlayPurchaseEvent.Error(result.responseCode))
            return false
        }
        return true
    }

    /** Also call when returning to foreground so pending purchases can finish. */
    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach(::handlePurchase)
            } else onEvent(PlayPurchaseEvent.Error(result.responseCode))
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (MonetizationRolloutConfig.PRO_PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> onEvent(PlayPurchaseEvent.Pending)
            Purchase.PurchaseState.PURCHASED -> scope.launch(Dispatchers.IO) {
                val verified = runCatching { verifyPurchaseToken(purchase.purchaseToken) }
                    .getOrDefault(false)
                if (!verified) {
                    onEvent(PlayPurchaseEvent.Unverified)
                    return@launch
                }
                // Entitlement storage is owned by the trusted verifier, not this UI adapter.
                if (purchase.isAcknowledged) {
                    onEvent(PlayPurchaseEvent.Verified)
                } else {
                    val params = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken).build()
                    client.acknowledgePurchase(params) { result ->
                        if (result.responseCode == BillingClient.BillingResponseCode.OK)
                            onEvent(PlayPurchaseEvent.Verified)
                        else onEvent(PlayPurchaseEvent.Error(result.responseCode))
                    }
                }
            }
        }
    }

    fun close() = client.endConnection()
}

internal sealed interface PlayPurchaseEvent {
    data object ProductReady : PlayPurchaseEvent
    data object ProductUnavailable : PlayPurchaseEvent
    data object Pending : PlayPurchaseEvent
    data object Cancelled : PlayPurchaseEvent
    data object ConnectionLost : PlayPurchaseEvent
    data object Unverified : PlayPurchaseEvent
    data object Verified : PlayPurchaseEvent
    data class Error(val code: Int) : PlayPurchaseEvent
}
