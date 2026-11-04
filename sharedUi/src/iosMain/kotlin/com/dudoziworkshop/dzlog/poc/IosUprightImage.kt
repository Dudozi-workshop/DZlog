@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import kotlinx.cinterop.*
import platform.Foundation.NSData
import platform.Foundation.create
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*

/** Only rendering uses this raster. ORIGINAL keeps the camera's exact encoded bytes/metadata. */
internal fun uprightIosImage(bytes: ByteArray): ByteArray {
    require(bytes.isNotEmpty()) { "사진 데이터가 없습니다." }
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    val image = checkNotNull(UIImage.imageWithData(data)) { "사진을 읽을 수 없습니다." }
    if (image.imageOrientation == UIImageOrientationUp) return bytes
    val size = image.size.useContents { width to height }
    UIGraphicsBeginImageContextWithOptions(image.size, false, 1.0)
    try {
        image.drawInRect(CGRectMake(0.0, 0.0, size.first, size.second))
        val normalized = checkNotNull(UIGraphicsGetImageFromCurrentImageContext())
        // Lossless intermediate: the final result renderer encodes JPEG only once.
        val raster = checkNotNull(UIImagePNGRepresentation(normalized))
        return checkNotNull(raster.bytes).readBytes(raster.length.toInt())
    } finally { UIGraphicsEndImageContext() }
}
