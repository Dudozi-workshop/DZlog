package com.example.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.dzlog.ui.theme.DDZColor

@Composable
internal fun CaptureAreaMaskOverlay(
    captureAspectRatio: Float,
    maskAlpha: Float = 0.2f
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = computeCaptureAreaRect(
            widthPx = size.width,
            heightPx = size.height,
            captureAspectRatio = captureAspectRatio
        )
        if (rect.width() <= 0f || rect.height() <= 0f) return@Canvas

        val color = DDZColor.PrimaryDark.copy(alpha = maskAlpha)

        // Top band
        if (rect.top > 0f) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(size.width, rect.top)
            )
        }
        // Bottom band
        if (rect.bottom < size.height) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, rect.bottom),
                size = androidx.compose.ui.geometry.Size(size.width, size.height - rect.bottom)
            )
        }
        // Left band
        if (rect.left > 0f) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(0f, rect.top),
                size = androidx.compose.ui.geometry.Size(rect.left, rect.height())
            )
        }
        // Right band
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
    widthPx: Float,
    heightPx: Float,
    captureAspectRatio: Float
): RectF {
    if (widthPx <= 0f || heightPx <= 0f) return RectF(0f, 0f, 0f, 0f)

    val clampedAspect = captureAspectRatio.coerceAtLeast(0.01f)
    val viewAspect = widthPx / heightPx

    return if (viewAspect > clampedAspect) {
        // View is wider than capture aspect: height is fully used, mask left/right.
        val targetWidth = heightPx * clampedAspect
        val side = ((widthPx - targetWidth) / 2f).coerceAtLeast(0f)
        RectF(side, 0f, widthPx - side, heightPx)
    } else {
        // View is taller than capture aspect: width is fully used, mask top/bottom.
        val targetHeight = widthPx / clampedAspect
        val topBottom = ((heightPx - targetHeight) / 2f).coerceAtLeast(0f)
        RectF(0f, topBottom, widthPx, heightPx - topBottom)
    }
}
