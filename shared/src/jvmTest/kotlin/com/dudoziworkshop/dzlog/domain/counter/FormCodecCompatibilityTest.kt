package com.dudoziworkshop.dzlog.domain.counter

import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.test.Test
import kotlin.test.assertEquals

class FormCodecCompatibilityTest {
    @Test
    fun portableCodecMatchesJavaWireEncodingForExistingCounterTokens() {
        val tokens = listOf("", "실험 A", "a,b|c%+", "~!*'()", "한글/사진", "🌱", "\uD800", "a\uDC00b") +
            (0..127).map { it.toChar().toString() }
        for (token in tokens) {
            val expected = URLEncoder.encode(token, "UTF-8")
            assertEquals(expected, encodeFormTokenPortable(token))
            assertEquals(URLDecoder.decode(expected, "UTF-8"), decodeFormTokenPortable(expected))
        }
    }
}
