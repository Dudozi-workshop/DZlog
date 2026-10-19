package com.dudoziworkshop.dzlog.domain.counter

internal expect fun encodeCounterToken(value: String): String

/** java.net form-URL wire format, including space as '+' and literal '*' preservation. */
internal fun encodeFormTokenPortable(value: String): String {
    // Java's UTF-8 encoder replaces isolated UTF-16 surrogates with '?'.
    val normalized = buildString {
        var i = 0
        while (i < value.length) {
            val c = value[i++]
            when {
                c.isHighSurrogate() && i < value.length && value[i].isLowSurrogate() -> {
                    append(c); append(value[i++])
                }
                c.isSurrogate() -> append('?')
                else -> append(c)
            }
        }
    }
    val hex = "0123456789ABCDEF"
    return buildString {
        for (byte in normalized.encodeToByteArray()) {
            val n = byte.toInt() and 255
            when {
                n in 65..90 || n in 97..122 || n in 48..57 || n.toChar() in "-_. *" ->
                    append(if (n == 32) '+' else n.toChar())
                else -> { append('%'); append(hex[n ushr 4]); append(hex[n and 15]) }
            }
        }
    }
}
