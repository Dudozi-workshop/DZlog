package com.dudoziworkshop.dzlog.domain.counter

import java.net.URLEncoder

internal actual fun encodeCounterToken(value: String): String = URLEncoder.encode(value, "UTF-8")
