package com.dudoziworkshop.dzlog.feature.table.policy

import android.app.Activity
import android.content.Context
import android.util.Base64
import com.dudoziworkshop.dzlog.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Single runtime owner of paid access and the Play Billing connection. */
object BillingEntitlementRuntime {
    private val mutableTier = MutableStateFlow(SaveRuleTier.FREE)
    val tier: StateFlow<SaveRuleTier> = mutableTier

    private val mutableEvent = MutableStateFlow<PlayPurchaseEvent?>(null)
    internal val event: StateFlow<PlayPurchaseEvent?> = mutableEvent

    private var repository: ProEntitlementRepository? = null
    private var gateway: PlayProBillingGateway? = null

    fun start(context: Context, scope: CoroutineScope) {
        if (!MonetizationRolloutConfig.purchasesEnabled) return
        if (repository != null) return
        val backend = BuildConfig.DZLOG_BILLING_URL
        val publicKey = runCatching {
            String(Base64.decode(BuildConfig.DZLOG_BILLING_PUBLIC_KEY_B64,
                Base64.DEFAULT), Charsets.UTF_8)
        }.getOrDefault("")
        if (!backend.startsWith("https://") ||
            !publicKey.contains("-----BEGIN PUBLIC KEY-----")) {
            mutableEvent.value = PlayPurchaseEvent.ProductUnavailable
            return // No secrets/configured backend: do not start checkout or grant rights
        }
        val repo = ProEntitlementRepository(context.applicationContext, backend, publicKey)
        repository = repo
        scope.launch { refresh() }
        gateway = PlayProBillingGateway(context, scope, repo::verifyPurchaseToken) { event ->
            mutableEvent.value = event
            when (event) {
                is PlayPurchaseEvent.Verified -> scope.launch { refresh() }
                is PlayPurchaseEvent.NotOwned -> scope.launch {
                    repo.clearEntitlement()
                    mutableTier.value = SaveRuleTier.FREE
                }
                else -> Unit
            }
        }.also { it.connect() }
    }

    suspend fun refresh() {
        if (!MonetizationRolloutConfig.purchasesEnabled) return
        mutableTier.value = runCatching { repository?.readAccess()?.tier }
            .getOrNull() ?: SaveRuleTier.FREE
    }

    fun startPurchase(activity: Activity): Boolean =
        MonetizationRolloutConfig.purchasesEnabled &&
            gateway?.beginPurchase(activity) == true

    fun restorePurchases() {
        if (MonetizationRolloutConfig.purchasesEnabled) gateway?.restorePurchases()
    }

    fun stop() {
        gateway?.close()
        gateway = null
        repository = null
        mutableEvent.value = null
        mutableTier.value = SaveRuleTier.FREE
    }
}
