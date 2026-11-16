package com.dudoziworkshop.dzlog.poc

import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

/** Center-crops camera pixels to the persisted capture ratio; no scaling is applied. */
internal fun cropIosCaptureJpeg(bytes: ByteArray, aspect: CaptureAspect): ByteArray {
    require(bytes.isNotEmpty()) { "사진 데이터가 없습니다." }
    val upright = uprightIosImage(bytes)
    val image = Image.makeFromEncoded(upright)
    val (width, height) = aspect.centerCropSize(image.width, image.height)
    if (width == image.width && height == image.height) return bytes

    val left = (image.width - width) / 2f
    val top = (image.height - height) / 2f
    val surface = Surface.makeRasterN32Premul(width, height)
    surface.canvas.drawImageRect(
        image,
        Rect.makeXYWH(-left, -top, image.width.toFloat(), image.height.toFloat()),
        Paint(),
    )
    return checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 99)) {
        "촬영 비율 적용에 실패했습니다."
    }.bytes
}

internal fun checkIosCaptureAspectRaster() {
    val source = Surface.makeRasterN32Premul(160, 240)
    source.canvas.clear(0xFF4B805A.toInt())
    val jpeg = checkNotNull(source.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 95)).bytes
    val expected = mapOf(
        CaptureAspect.R3_4 to (160 to 213),
        CaptureAspect.R9_16 to (135 to 240),
        CaptureAspect.R1_1 to (160 to 160),
    )
    for ((aspect, size) in expected) {
        val cropped = Image.makeFromEncoded(cropIosCaptureJpeg(jpeg, aspect))
        check(cropped.width == size.first && cropped.height == size.second) {
            "${aspect.label} 촬영 비율 크기가 올바르지 않습니다."
        }
    }
}
