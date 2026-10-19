package com.dudoziworkshop.dzlog.domain.counter

internal actual fun encodeCounterToken(value: String): String = encodeFormTokenPortable(value)
internal actual fun decodeCounterToken(value: String): String = decodeFormTokenPortable(value)
