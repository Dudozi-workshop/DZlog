package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/** Keyed by asset by the caller, so zoom never leaks into the next photo. */
@Composable
internal fun IosGalleryImageViewer(image: ImageBitmap, enabled: Boolean) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    fun constrained(value: Offset, zoom: Float): Offset {
        if (viewport.width == 0 || viewport.height == 0) return Offset.Zero
        val fit = minOf(viewport.width.toFloat() / image.width, viewport.height.toFloat() / image.height)
        val maxX = ((image.width * fit * zoom - viewport.width) / 2f).coerceAtLeast(0f)
        val maxY = ((image.height * fit * zoom - viewport.height) / 2f).coerceAtLeast(0f)
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }
    fun setZoom(value: Float) {
        val next = value.coerceIn(1f, 4f)
        offset = constrained(offset * (next / scale), next)
        scale = next
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(enabled = enabled && scale > 1f, onClick = { setZoom(scale - 0.5f) }) { Text("축소") }
            Text("배율 ${(scale * 100).roundToInt()}%")
            TextButton(enabled = enabled && scale < 4f, onClick = { setZoom(scale + 0.5f) }) { Text("확대") }
            TextButton(enabled = enabled && scale > 1f, onClick = { setZoom(1f) }) { Text("화면 맞춤") }
        }
        Box(Modifier.fillMaxWidth().weight(1f).clipToBounds()
            .onSizeChanged {
                if (viewport != it) { viewport = it; scale = 1f; offset = Offset.Zero }
            }
            .pointerInput(image, enabled) {
                if (enabled) detectTapGestures(onDoubleTap = { setZoom(if (scale > 1f) 1f else 2f) })
            }
            .pointerInput(image, enabled) {
                if (enabled) detectTransformGestures { centroid, pan, zoom, _ ->
                    val next = (scale * zoom).coerceIn(1f, 4f)
                    val anchor = centroid - Offset(viewport.width / 2f, viewport.height / 2f)
                    offset = constrained((offset - anchor) * (next / scale) + anchor + pan, next)
                    scale = next
                }
            }, contentAlignment = Alignment.Center) {
            Image(image, "사진 확대 보기", Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }, contentScale = ContentScale.Fit)
        }
    }
}
