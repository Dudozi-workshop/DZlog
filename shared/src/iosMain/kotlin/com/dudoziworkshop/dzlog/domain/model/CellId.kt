package com.dudoziworkshop.dzlog.domain.model

import platform.Foundation.NSUUID

internal actual fun randomCellId(): String = NSUUID().UUIDString.lowercase()
