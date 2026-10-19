package com.dudoziworkshop.dzlog.domain.model

import java.util.UUID

internal actual fun randomCellId(): String = UUID.randomUUID().toString()
