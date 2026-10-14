package com.dudoziworkshop.dzlog.feature.table.policy

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Validates short-lived, backend-signed Play purchase rights, including without network.
 * No client-side owned=true, SharedPreferences toggle, or raw purchase token grants access.
 */
internal object SignedProGrantVerifier {
    private const val PACKAGE_ID = "com.dudoziworkshop.dzlog"
    private const val PRODUCT_ID = "dzlog_pro_lifetime"
    private const val ISSUER = "dzlog-billing"
    private const val MAX_AGE_SECONDS = 30L * 24L * 3600L

    private fun decode(input: String): ByteArray = Base64.decode(
        input, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    fun verify(
        signedGrant: String,
        publicKeyPem: String,
        nowMillis: Long,
        lastObservedMillis: Long = 0L,
    ): ProPurchaseEvidence.Verified? = runCatching {
        require(publicKeyPem.isNotBlank() && nowMillis > 0)
        val parts = signedGrant.split('.')
        require(parts.size == 3)
        val header = JSONObject(String(decode(parts[0]), StandardCharsets.UTF_8))
        require(header.optString("alg") == "RS256")
        val der = Base64.decode(
            publicKeyPem.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s+".toRegex(), ""),
            Base64.DEFAULT,
        )
        val key = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(der))
        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(key)
        verifier.update((parts[0] + "." + parts[1]).toByteArray(StandardCharsets.US_ASCII))
        require(verifier.verify(decode(parts[2])))
        val payload = JSONObject(String(decode(parts[1]), StandardCharsets.UTF_8))
        require(payload.getString("iss") == ISSUER)
        require(payload.getString("aud") == PACKAGE_ID)
        require(payload.getString("sku") == PRODUCT_ID)
        require(payload.getString("sub").length == 64)
        val issued = payload.getLong("iat")
        val expires = payload.getLong("exp")
        require(issued > 0 && expires > issued && expires - issued <= MAX_AGE_SECONDS)
        val lastVerifiedAt = Math.multiplyExact(issued, 1000L)
        val expirationAt = Math.multiplyExact(expires, 1000L)
        require(nowMillis >= lastVerifiedAt && nowMillis <= expirationAt)
        require(nowMillis >= lastObservedMillis)
        ProPurchaseEvidence.Verified(
            lastVerifiedAtMillis = lastVerifiedAt,
            latestObservedAtMillis = maxOf(lastObservedMillis, nowMillis),
        )
    }.getOrNull()
}
