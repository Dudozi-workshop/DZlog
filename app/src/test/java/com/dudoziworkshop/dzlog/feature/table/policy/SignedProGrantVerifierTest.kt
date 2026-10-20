package com.dudoziworkshop.dzlog.feature.table.policy

import android.util.Base64
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.security.KeyPairGenerator
import java.security.Signature
import java.nio.charset.StandardCharsets

@RunWith(RobolectricTestRunner::class)
class SignedProGrantVerifierTest {
    private val pair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicPem = "-----BEGIN PUBLIC KEY-----\n" +
        Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP) +
        "\n-----END PUBLIC KEY-----"
    private val nowSeconds = 1_790_000_000L

    private fun b64(input: ByteArray) = Base64.encodeToString(
        input, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
    )
    private fun makeGrant(
        audience: String = "com.dudoziworkshop.dzlog",
        sku: String = "dzlog_pro_lifetime",
        exp: Long = nowSeconds + 30L * 24L * 3600L,
    ): String {
        val header = b64("""{"alg":"RS256","typ":"JWT"}""".toByteArray())
        val payload = b64(JSONObject()
            .put("iss", "dzlog-billing")
            .put("aud", audience)
            .put("sku", sku)
            .put("sub", "a".repeat(64))
            .put("iat", nowSeconds)
            .put("exp", exp)
            .toString().toByteArray())
        val msg = "$header.$payload"
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(pair.private)
        signature.update(msg.toByteArray(StandardCharsets.US_ASCII))
        return "$msg.${b64(signature.sign())}"
    }

    @Test
    fun `valid signed proof supports 30-day offline use`() {
        val proof = makeGrant()
        val right = SignedProGrantVerifier.verify(proof, publicPem, nowSeconds * 1000L)
        assertNotNull(right)
        assertEquals(nowSeconds * 1000L, right!!.lastVerifiedAtMillis)
        assertNotNull(SignedProGrantVerifier.verify(proof, publicPem,
            (nowSeconds + 30L * 24L * 3600L) * 1000L))
        assertNull(SignedProGrantVerifier.verify(proof, publicPem,
            (nowSeconds + 30L * 24L * 3600L) * 1000L + 1))
    }

    @Test
    fun `forged proof foreign package and clock rollback are rejected`() {
        val signed = makeGrant()
        // Mutate a decoded bit: replacing trailing Base64 characters can preserve the bytes.
        val signature = Base64.decode(signed.substringAfterLast('.'), Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        signature[0] = (signature[0].toInt() xor 1).toByte()
        val forged = signed.substringBeforeLast('.') + "." + b64(signature)
        assertNull(SignedProGrantVerifier.verify(forged,
            publicPem, nowSeconds * 1000L))
        assertNull(SignedProGrantVerifier.verify(
            makeGrant(audience = "invalid.package"), publicPem, nowSeconds * 1000L))
        assertNull(SignedProGrantVerifier.verify(
            makeGrant(sku = "fake_sku"), publicPem, nowSeconds * 1000L))
        assertNull(SignedProGrantVerifier.verify(
            signed, publicPem, nowSeconds * 1000L - 1))
        assertNull(SignedProGrantVerifier.verify(
            signed, publicPem, nowSeconds * 1000L + 2,
            lastObservedMillis = nowSeconds * 1000L + 3))
    }
}
