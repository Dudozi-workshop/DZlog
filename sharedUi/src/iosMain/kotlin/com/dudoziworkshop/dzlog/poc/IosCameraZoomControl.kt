package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.camera.controls.CAMERA_MAX_ZOOM_TENTHS
import com.dudoziworkshop.dzlog.ui.camera.controls.CAMERA_MIN_ZOOM_TENTHS
import com.dudoziworkshop.dzlog.ui.camera.controls.ZoomTickWeight
import com.dudoziworkshop.dzlog.ui.camera.controls.cameraZoomNeighbors
import com.dudoziworkshop.dzlog.ui.camera.controls.cameraZoomTickWeight
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlin.math.roundToInt

@Composable
internal fun IosCameraZoomControl(
    zoom: Float,
    maximumZoom: Float,
    expanded: Boolean,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onZoomFinished: (Float) -> Unit,
) {
    val maximumTenths = (maximumZoom * 10f).roundToInt()
        .coerceIn(CAMERA_MIN_ZOOM_TENTHS, CAMERA_MAX_ZOOM_TENTHS)
    val zoomTenths = (zoom * 10f).roundToInt()
        .coerceIn(CAMERA_MIN_ZOOM_TENTHS, maximumTenths)
    val (left, right) = cameraZoomNeighbors(zoomTenths, maximumTenths)

    Column(horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (expanded) IosZoomRuler(
            zoomTenths = zoomTenths,
            maximumTenths = maximumTenths,
            enabled = enabled,
            onZoomChange = { onZoomChange(it / 10f) },
            onZoomFinished = { onZoomFinished(it / 10f) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            IosZoomNeighborChip(left, enabled) {
                onZoomChange(it / 10f)
                onZoomFinished(it / 10f)
            }
            IosZoomCurrentChip(
                zoomTenths = zoomTenths,
                maximumTenths = maximumTenths,
                enabled = enabled,
                onToggleExpanded = onToggleExpanded,
                onZoomChange = { onZoomChange(it / 10f) },
                onZoomFinished = { onZoomFinished(it / 10f) },
            )
            IosZoomNeighborChip(right, enabled) {
                onZoomChange(it / 10f)
                onZoomFinished(it / 10f)
            }
        }
    }
}

@Composable
private fun IosZoomNeighborChip(value: Int?, enabled: Boolean, onSelect: (Int) -> Unit) {
    Box(Modifier.defaultMinSize(minWidth = 36.dp, minHeight = 30.dp),
        contentAlignment = Alignment.Center) {
        if (value != null) Box(
            Modifier.clip(CircleShape)
                .background(DDZColor.Surface.copy(alpha = 0.72f), CircleShape)
                .border(1.dp, DDZColor.SageBorder, CircleShape)
                .clickable(enabled = enabled) { onSelect(value) }
                .padding(horizontal = 7.dp, vertical = 5.dp),
        ) {
            Text("${formatIosZoom(value / 10f)}×", style = DDZTypography.Caption,
                color = if (enabled) DDZColor.TextStrong else DDZColor.TextMuted)
        }
    }
}

@Composable
private fun IosZoomCurrentChip(
    zoomTenths: Int,
    maximumTenths: Int,
    enabled: Boolean,
    onToggleExpanded: () -> Unit,
    onZoomChange: (Int) -> Unit,
    onZoomFinished: (Int) -> Unit,
) {
    val latestZoom by rememberUpdatedState(zoomTenths)
    val latestChange by rememberUpdatedState(onZoomChange)
    val latestFinish by rememberUpdatedState(onZoomFinished)
    var dragValue by remember { mutableIntStateOf(zoomTenths) }
    Box(
        Modifier.defaultMinSize(minWidth = 52.dp, minHeight = 30.dp)
            .clip(CircleShape)
            .background(DDZColor.SelectedSoft.copy(alpha = 0.76f), CircleShape)
            .border(1.dp, DDZColor.SelectedDark.copy(alpha = 0.75f), CircleShape)
            .pointerInput(enabled, maximumTenths) {
                if (!enabled) return@pointerInput
                var fractional = 0f
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragValue = latestZoom
                        fractional = 0f
                    },
                    onDragEnd = { latestFinish(dragValue) },
                    onDragCancel = { latestFinish(dragValue) },
                ) { change, delta ->
                    fractional += delta / 12.dp.toPx()
                    val steps = fractional.toInt()
                    if (steps != 0) {
                        fractional -= steps
                        dragValue = (dragValue + steps)
                            .coerceIn(CAMERA_MIN_ZOOM_TENTHS, maximumTenths)
                        latestChange(dragValue)
                    }
                    change.consume()
                }
            }
            .clickable(enabled = enabled, onClick = onToggleExpanded)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("${formatIosZoom(zoomTenths / 10f)}×", color = DDZColor.SelectionEmphasis,
            style = DDZTypography.Caption)
    }
}

@Composable
private fun IosZoomRuler(
    zoomTenths: Int,
    maximumTenths: Int,
    enabled: Boolean,
    onZoomChange: (Int) -> Unit,
    onZoomFinished: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val tickSpacingPx = with(density) { 8.dp.toPx() }
    val latestZoom by rememberUpdatedState(zoomTenths)
    val latestChange by rememberUpdatedState(onZoomChange)
    val latestFinish by rememberUpdatedState(onZoomFinished)
    val shape = RoundedCornerShape(DDZLayout.Radius.Medium)

    Column(
        Modifier.widthIn(max = 280.dp).fillMaxWidth(0.86f).clip(shape)
            .background(DDZColor.OverlayScrim.copy(alpha = 0.57f), shape)
            .border(1.dp, DDZColor.BorderStrong, shape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("${formatIosZoom(zoomTenths / 10f)}×", color = DDZColor.SelectedSoft,
            style = DDZTypography.Body)
        Canvas(
            Modifier.fillMaxWidth().height(50.dp)
                .pointerInput(enabled, maximumTenths) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val mapped = (latestZoom + (offset.x - size.width / 2f) / tickSpacingPx)
                            .roundToInt().coerceIn(CAMERA_MIN_ZOOM_TENTHS, maximumTenths)
                        latestChange(mapped)
                        latestFinish(mapped)
                    }
                }
                .pointerInput(enabled, maximumTenths) {
                    if (!enabled) return@pointerInput
                    var startZoom = latestZoom
                    var startX = 0f
                    var lastValue = latestZoom
                    detectDragGestures(
                        onDragStart = {
                            startZoom = latestZoom
                            startX = it.x
                        },
                        onDragEnd = { latestFinish(lastValue) },
                        onDragCancel = { latestFinish(lastValue) },
                    ) { change, _ ->
                        val mapped = (startZoom + (change.position.x - startX) / tickSpacingPx)
                            .roundToInt().coerceIn(CAMERA_MIN_ZOOM_TENTHS, maximumTenths)
                        lastValue = mapped
                        latestChange(mapped)
                        change.consume()
                    }
                },
        ) {
            val centerX = size.width / 2f
            val baselineY = size.height - 18.dp.toPx()
            drawLine(DDZColor.OverlayText.copy(alpha = 0.30f), Offset(0f, baselineY),
                Offset(size.width, baselineY), 1.dp.toPx())
            for (tick in CAMERA_MIN_ZOOM_TENTHS..maximumTenths) {
                val x = centerX + (tick - zoomTenths) * tickSpacingPx
                if (x < 0f || x > size.width || kotlin.math.abs(x - centerX) < 2.dp.toPx()) continue
                val weight = cameraZoomTickWeight(tick)
                val height = when (weight) {
                    ZoomTickWeight.MAJOR -> 25.dp.toPx()
                    ZoomTickWeight.HALF -> 15.dp.toPx()
                    ZoomTickWeight.MINOR -> 9.dp.toPx()
                }
                drawLine(DDZColor.OverlayText.copy(alpha = when (weight) {
                    ZoomTickWeight.MAJOR -> 0.94f
                    ZoomTickWeight.HALF -> 0.74f
                    ZoomTickWeight.MINOR -> 0.42f
                }), Offset(x, baselineY), Offset(x, baselineY - height),
                    if (weight == ZoomTickWeight.MAJOR) 2.dp.toPx() else 1.dp.toPx())
            }
            val pointer = Path().apply {
                moveTo(centerX, baselineY - 21.dp.toPx())
                lineTo(centerX - 5.dp.toPx(), baselineY - 14.dp.toPx())
                lineTo(centerX, baselineY)
                lineTo(centerX + 5.dp.toPx(), baselineY - 14.dp.toPx())
                close()
            }
            drawPath(pointer, DDZColor.SelectedSoft)
        }
    }
}
