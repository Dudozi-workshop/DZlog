package com.dudoziworkshop.dzlog.domain.counter

internal expect fun encodeCounterToken(value: String): String
internal expect fun decodeCounterToken(value: String): String

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

internal fun decodeFormTokenPortable(value: String): String = buildString {
    var i = 0
    while (i < value.length) {
        when (val c = value[i]) {
            '+' -> { append(' '); i++ }
            '%' -> {
                val bytes = mutableListOf<Byte>()
                while (i < value.length && value[i] == '%') {
                    require(i + 2 < value.length) { "Incomplete percent escape" }
                    val high = value[i + 1].digitToIntOrNull(16)
                    val low = value[i + 2].digitToIntOrNull(16)
                    require(high != null && low != null) { "Invalid percent escape" }
                    bytes += ((high shl 4) or low).toByte()
                    i += 3
                }
                append(bytes.toByteArray().decodeToString())
            }
            else -> { append(c); i++ }
        }
    }
}
