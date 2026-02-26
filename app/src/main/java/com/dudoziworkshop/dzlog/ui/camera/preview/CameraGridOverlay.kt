package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun CameraGridOverlay(
    captureRect: RectF?
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = captureRect ?: return@Canvas
        if (rect.width() <= 0f || rect.height() <= 0f) return@Canvas

        val oneThirdW = rect.width() / 3f
        val twoThirdW = oneThirdW * 2f
        val oneThirdH = rect.height() / 3f
        val twoThirdH = oneThirdH * 2f
        val stroke = 1.5f
        val color = DDZColor.Surface.copy(alpha = 0.42f)

        drawLine(
            color = color,
            start = Offset(rect.left + oneThirdW, rect.top),
            end = Offset(rect.left + oneThirdW, rect.bottom),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(rect.left + twoThirdW, rect.top),
            end = Offset(rect.left + twoThirdW, rect.bottom),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(rect.left, rect.top + oneThirdH),
            end = Offset(rect.right, rect.top + oneThirdH),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(rect.left, rect.top + twoThirdH),
            end = Offset(rect.right, rect.top + twoThirdH),
            strokeWidth = stroke
        )
    }
}
