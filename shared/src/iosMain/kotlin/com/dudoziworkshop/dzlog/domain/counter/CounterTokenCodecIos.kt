package com.dudoziworkshop.dzlog.domain.counter

internal actual fun encodeCounterToken(value: String): String = encodeFormTokenPortable(value)
