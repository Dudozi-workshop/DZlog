package com.dudoziworkshop.dzlog.poc

import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface

/**
 * Raster-only composition of a resolved full-frame transparent table overlay.
 * Table layout, cell values and styles must be rendered upstream; this adapter
 * does not infer them and does not touch PhotoKit or workspace state.
 */
internal object IosCaptureResultRenderer {
    fun compositeJpeg(originalJpeg: ByteArray, overlayPng: ByteArray): ByteArray {
        require(originalJpeg.isNotEmpty()) { "원본 JPEG가 비어 있습니다." }
        require(overlayPng.isNotEmpty()) { "표 오버레이가 비어 있습니다." }
        val original = Image.makeFromEncoded(originalJpeg)
        val overlay = Image.makeFromEncoded(overlayPng)
        require(original.width > 0 && original.height > 0) { "원본 이미지 크기가 잘못되었습니다." }
        require(original.width == overlay.width && original.height == overlay.height) {
            "표 오버레이 크기가 원본 이미지와 다릅니다."
        }
        val surface = Surface.makeRasterN32Premul(original.width, original.height)
        val bounds = Rect.makeWH(original.width.toFloat(), original.height.toFloat())
        surface.canvas.drawImageRect(original, bounds, Paint())
        surface.canvas.drawImageRect(overlay, bounds, Paint())
        return checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 90)) {
            "표 합성 JPEG 인코딩에 실패했습니다."
        }.bytes
    }
}
