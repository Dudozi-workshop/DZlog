package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Backend configuration is supplied via trusted build configuration, never user preferences. */
internal class ProEntitlementRepository(
    private val context: Context,
    private val endpoint: String,
    private val publicKeyPem: String,
) {
    private val grantKey = stringPreferencesKey("verified_pro_signed_grant_v1")
    private val observationKey = longPreferencesKey("verified_pro_latest_observation_v1")

    init {
        require(endpoint.startsWith("https://"))
        require(publicKeyPem.contains("-----BEGIN PUBLIC KEY-----"))
    }

    /** Used by PlayProBillingGateway after a PURCHASED result; never grants on network error. */
    suspend fun verifyPurchaseToken(purchaseToken: String): Boolean = withContext(Dispatchers.IO) {
        val response = try {
            val conn = (URL(endpoint.trimEnd('/') + "/v1/verify")
                .openConnection() as HttpURLConnection)
            try {
                conn.requestMethod = "POST"
                conn.connectTimeout = 8000
                conn.readTimeout = 10000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use {
                    it.write(JSONObject().put("purchaseToken", purchaseToken).toString()
                        .toByteArray(Charsets.UTF_8))
                }
                if (conn.responseCode !in 200..299) return@withContext false
                JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            return@withContext false // unknown, preserve still-valid cached grant
        }
        when (response.optString("state")) {
            "VERIFIED" -> {
                val candidate = response.optString("grant")
                val now = System.currentTimeMillis()
                val current = SignedProGrantVerifier.verify(
                    candidate, publicKeyPem, now,
                ) ?: return@withContext false
                context.dataStore.edit {
                    it[grantKey] = candidate
                    it[observationKey] = current.latestObservedAtMillis
                }
                true
            }
            "REVOKED", "NOT_OWNED" -> {
                // A definitive Play state supersedes the 30-day grace interval.
                clearEntitlement()
                false
            }
            else -> false
        }
    }

    suspend fun readAccess(nowMillis: Long = System.currentTimeMillis()): ProAccessDecision {
        val prefs = context.dataStore.data.first()
        val grant = prefs[grantKey]
            ?: return ProEntitlementAccessPolicy.evaluate(ProPurchaseEvidence.Unknown, nowMillis)
        val lastObserved = prefs[observationKey] ?: 0L
        val evidence = SignedProGrantVerifier.verify(
            grant, publicKeyPem, nowMillis, lastObserved,
        ) ?: return ProEntitlementAccessPolicy.evaluate(ProPurchaseEvidence.Unknown, nowMillis)
        if (nowMillis - lastObserved >= 60_000L) {
            context.dataStore.edit { it[observationKey] = nowMillis }
        }
        return ProEntitlementAccessPolicy.evaluate(evidence, nowMillis)
    }

    suspend fun clearEntitlement() {
        context.dataStore.edit {
            it.remove(grantKey)
            it.remove(observationKey)
        }
    }
}
