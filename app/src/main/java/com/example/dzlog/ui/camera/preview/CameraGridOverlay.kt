package com.example.dzlog.ui.camera.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.example.dzlog.ui.theme.DDZColor

@Composable
internal fun CameraGridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val oneThirdW = size.width / 3f
        val twoThirdW = oneThirdW * 2f
        val oneThirdH = size.height / 3f
        val twoThirdH = oneThirdH * 2f
        val stroke = 1.5f
        val color = DDZColor.Surface.copy(alpha = 0.42f)

        drawLine(color = color, start = Offset(oneThirdW, 0f), end = Offset(oneThirdW, size.height), strokeWidth = stroke)
        drawLine(color = color, start = Offset(twoThirdW, 0f), end = Offset(twoThirdW, size.height), strokeWidth = stroke)
        drawLine(color = color, start = Offset(0f, oneThirdH), end = Offset(size.width, oneThirdH), strokeWidth = stroke)
        drawLine(color = color, start = Offset(0f, twoThirdH), end = Offset(size.width, twoThirdH), strokeWidth = stroke)
    }
}
