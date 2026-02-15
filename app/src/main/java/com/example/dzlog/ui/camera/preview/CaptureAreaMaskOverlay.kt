package com.example.dzlog.ui.camera.preview

import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.util.Size
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dzlog.domain.camera.computeAnchoredCaptureRect
import com.example.dzlog.ui.theme.DDZColor

private const val MASK_ALPHA = 0.8f

@Composable
internal fun CaptureAreaMaskOverlay(
    previewView: PreviewView,
    captureAspectRatio: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val contentRect = resolvePreviewContentRect(
            previewView = previewView,
            overlayWidth = size.width,
            overlayHeight = size.height
        )

        val rect = computeCaptureAreaRect(
            contentRect = contentRect,
            captureAspectRatio = captureAspectRatio
        )

        if (rect.width() <= 0f || rect.height() <= 0f) return@Canvas

        val color = DDZColor.PrimaryDark.copy(alpha = MASK_ALPHA)

        if (rect.top > 0f) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, rect.top)
            )
        }

        if (rect.bottom < size.height) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, rect.bottom),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - rect.bottom)
            )
        }

        if (rect.left > 0f) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, rect.top),
                size = androidx.compose.ui.geometry.Size(rect.left, rect.height())
            )
        }

        if (rect.right < size.width) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(rect.right, rect.top),
                size = androidx.compose.ui.geometry.Size(size.width - rect.right, rect.height())
            )
        }
    }
}

internal fun computeCaptureAreaRect(
    contentRect: RectF,
    captureAspectRatio: Float
): RectF = computeAnchoredCaptureRect(contentRect, captureAspectRatio).captureRect

internal fun resolvePreviewContentRect(
    previewView: PreviewView,
    overlayWidth: Float,
    overlayHeight: Float
): RectF {
    val full = RectF(0f, 0f, overlayWidth, overlayHeight)

    if (overlayWidth <= 0f || overlayHeight <= 0f) return full
    if (previewView.width <= 0 || previewView.height <= 0) return full

    val previewToOverlayX = overlayWidth / previewView.width.toFloat()
    val previewToOverlayY = overlayHeight / previewView.height.toFloat()

    val transformed = resolveOutputTransformRect(previewView)
        ?: resolveTransformationInfoCropRect(previewView)
        ?: return full

    transformed.left *= previewToOverlayX
    transformed.right *= previewToOverlayX
    transformed.top *= previewToOverlayY
    transformed.bottom *= previewToOverlayY

    return RectF(
        transformed.left.coerceIn(0f, overlayWidth),
        transformed.top.coerceIn(0f, overlayHeight),
        transformed.right.coerceIn(0f, overlayWidth),
        transformed.bottom.coerceIn(0f, overlayHeight)
    )
}

private fun resolveOutputTransformRect(previewView: PreviewView): RectF? {
    return runCatching {
        val outputTransform = PreviewView::class.java
            .getMethod("getOutputTransform")
            .invoke(previewView) ?: return null

        val cropRect = outputTransform.javaClass.methods
            .firstOrNull { it.name == "getCropRect" && it.parameterCount == 0 }
            ?.invoke(outputTransform) as? Rect
        if (cropRect != null) {
            return RectF(cropRect)
        }

        val matrix = outputTransform.javaClass.methods
            .firstOrNull { it.name == "getMatrix" && it.parameterCount == 0 }
            ?.invoke(outputTransform) as? Matrix ?: return null

        val sourceSize = (outputTransform.javaClass.methods
            .firstOrNull { it.name == "getViewPortSize" && it.parameterCount == 0 }
            ?.invoke(outputTransform) as? Size)
            ?: (outputTransform.javaClass.methods
                .firstOrNull { it.name == "getSize" && it.parameterCount == 0 }
                ?.invoke(outputTransform) as? Size)
            ?: return null

        val points = floatArrayOf(
            0f,
            0f,
            sourceSize.width.toFloat(),
            0f,
            sourceSize.width.toFloat(),
            sourceSize.height.toFloat(),
            0f,
            sourceSize.height.toFloat()
        )
        matrix.mapPoints(points)

        val minX = minOf(points[0], points[2], points[4], points[6])
        val maxX = maxOf(points[0], points[2], points[4], points[6])
        val minY = minOf(points[1], points[3], points[5], points[7])
        val maxY = maxOf(points[1], points[3], points[5], points[7])

        RectF(minX, minY, maxX, maxY)
    }.getOrNull()
}

private fun resolveTransformationInfoCropRect(previewView: PreviewView): RectF? {
    return runCatching {
        val info = PreviewView::class.java
            .methods
            .firstOrNull { it.name == "getTransformationInfo" && it.parameterCount == 0 }
            ?.invoke(previewView) ?: return null

        val cropRect = info.javaClass.methods
            .firstOrNull { it.name == "getCropRect" && it.parameterCount == 0 }
            ?.invoke(info) as? Rect ?: return null

        RectF(cropRect)
    }.getOrNull()
}
