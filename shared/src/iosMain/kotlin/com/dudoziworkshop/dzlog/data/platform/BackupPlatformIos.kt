@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.data.platform

import kotlinx.cinterop.*
import platform.CoreCrypto.CC_SHA256
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970

internal actual fun newTemplateId(): String = NSUUID().UUIDString.lowercase()
internal actual fun backupNowMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
internal actual fun backupSha256(payload: String): String {
    val input = payload.encodeToByteArray()
    val storage = if (input.isEmpty()) byteArrayOf(0) else input
    val digest = ByteArray(32)
    storage.usePinned { src -> digest.usePinned { dst ->
        CC_SHA256(src.addressOf(0), input.size.convert(), dst.addressOf(0).reinterpret())
    } }
    return digest.joinToString("") { (it.toInt() and 255).toString(16).padStart(2, '0') }
}
