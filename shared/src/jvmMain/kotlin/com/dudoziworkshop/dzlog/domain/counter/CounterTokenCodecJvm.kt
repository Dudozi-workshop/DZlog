package com.dudoziworkshop.dzlog.domain.counter

import java.net.URLDecoder
import java.net.URLEncoder

internal actual fun encodeCounterToken(value: String): String = URLEncoder.encode(value, "UTF-8")
internal actual fun decodeCounterToken(value: String): String = URLDecoder.decode(value, "UTF-8")
