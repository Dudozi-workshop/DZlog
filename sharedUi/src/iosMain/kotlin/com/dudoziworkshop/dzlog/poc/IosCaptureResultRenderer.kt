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
    fun compositeJpeg(originalJpeg: ByteArray, overlayPng: ByteArray, qualityMode: Int = 1): ByteArray {
        require(qualityMode in 0..2) { "사진 품질 설정이 올바르지 않습니다." }
        require(originalJpeg.isNotEmpty()) { "원본 JPEG가 비어 있습니다." }
        require(overlayPng.isNotEmpty()) { "표 오버레이가 비어 있습니다." }
        val original = Image.makeFromEncoded(originalJpeg)
        val overlay = Image.makeFromEncoded(overlayPng)
        require(original.width > 0 && original.height > 0) { "원본 이미지 크기가 잘못되었습니다." }
        require(original.width == overlay.width && original.height == overlay.height) {
            "표 오버레이 크기가 원본 이미지와 다릅니다."
        }
        // Scale the photo and its full-frame overlay together, before the only JPEG encode.
        val longEdge = maxOf(original.width, original.height)
        val scale = if (qualityMode == 0 && longEdge > 1800) 1800f / longEdge else 1f
        val width = (original.width * scale).toInt().coerceAtLeast(1)
        val height = (original.height * scale).toInt().coerceAtLeast(1)
        val surface = Surface.makeRasterN32Premul(width, height)
        val bounds = Rect.makeWH(width.toFloat(), height.toFloat())
        surface.canvas.drawImageRect(original, bounds, Paint())
        surface.canvas.drawImageRect(overlay, bounds, Paint())
        val jpegQuality = when (qualityMode) { 0 -> 88; 2 -> 99; else -> 94 }
        return checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, jpegQuality)) {
            "표 합성 JPEG 인코딩에 실패했습니다."
        }.bytes
    }
}
