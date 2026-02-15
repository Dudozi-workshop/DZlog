package com.example.dzlog.ui.camera.preview

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.dzlog.ui.theme.DDZColor

@Composable
internal fun CameraGridOverlay(
    previewView: PreviewView,
    captureAspectRatio: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val contentRect = resolvePreviewContentRect(
            previewView = previewView,
            overlayWidth = size.width,
            overlayHeight = size.height
        )
        val captureRect = computeCaptureAreaRect(
            contentRect = contentRect,
            captureAspectRatio = captureAspectRatio
        )

        if (captureRect.width() <= 0f || captureRect.height() <= 0f) return@Canvas

        val oneThirdW = captureRect.width() / 3f
        val twoThirdW = oneThirdW * 2f
        val oneThirdH = captureRect.height() / 3f
        val twoThirdH = oneThirdH * 2f
        val stroke = 1.5f
        val color = DDZColor.Surface.copy(alpha = 0.42f)

        drawLine(
            color = color,
            start = Offset(captureRect.left + oneThirdW, captureRect.top),
            end = Offset(captureRect.left + oneThirdW, captureRect.bottom),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(captureRect.left + twoThirdW, captureRect.top),
            end = Offset(captureRect.left + twoThirdW, captureRect.bottom),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(captureRect.left, captureRect.top + oneThirdH),
            end = Offset(captureRect.right, captureRect.top + oneThirdH),
            strokeWidth = stroke
        )
        drawLine(
            color = color,
            start = Offset(captureRect.left, captureRect.top + twoThirdH),
            end = Offset(captureRect.right, captureRect.top + twoThirdH),
            strokeWidth = stroke
        )
    }
}
