package com.dudoziworkshop.dzlog.data.platform

import java.security.MessageDigest
import java.util.UUID

internal actual fun newTemplateId(): String = UUID.randomUUID().toString()
internal actual fun backupNowMillis(): Long = System.currentTimeMillis()
internal actual fun backupSha256(payload: String): String = MessageDigest.getInstance("SHA-256")
    .digest(payload.toByteArray(Charsets.UTF_8))
    .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
