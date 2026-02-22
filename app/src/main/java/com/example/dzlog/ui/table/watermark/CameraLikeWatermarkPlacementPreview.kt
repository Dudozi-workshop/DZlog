package com.example.dzlog.ui.table.watermark

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.watermark.drawWatermarkTableOnCanvas
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun CameraLikeWatermarkPlacementPreview(
    captureAspect: CaptureAspect,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    watermarkCells: List<WatermarkBuilder.WatermarkCell>,
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    bgStyle: Int,
    bgAlpha: Int,
    valueScale: Int,
    textColorMode: Int,
    manualTextColor: Int,
    textAlign: Int,
    armed: Boolean,
    onArmedChange: (Boolean) -> Unit,
    onDragPreview: (Int, Int) -> Unit,
    onDragCommit: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffsetXRatio by remember(anchor, offsetXRatio) { mutableIntStateOf(offsetXRatio.coerceIn(0, 100)) }
    var dragOffsetYRatio by remember(anchor, offsetYRatio) { mutableIntStateOf(offsetYRatio.coerceIn(0, 100)) }
    var dragActive by remember { mutableStateOf(false) }
    var dragLeftPx by remember { mutableFloatStateOf(0f) }
    var dragTopPx by remember { mutableFloatStateOf(0f) }
    var dragMaxXPx by remember { mutableFloatStateOf(0f) }
    var dragMaxYPx by remember { mutableFloatStateOf(0f) }
    var hasOverride by remember { mutableStateOf(false) }
    var overrideReleaseTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(overrideReleaseTick) {
        if (overrideReleaseTick == 0) return@LaunchedEffect
        delay(80)
        if (!dragActive) {
            hasOverride = false
        }
    }

    val tapModifier = Modifier.pointerInput(captureAspect, anchor, offsetXRatio, offsetYRatio, tableWidthRatio, tableHeightRatio) {
        detectTapGestures { tapOffset ->
            val contentRect = computeContentRect(size.width.toFloat(), size.height.toFloat(), captureAspect.ratioF)
            val tableRect = computeTableRect(
                contentRect = contentRect,
                anchor = anchor,
                offsetXRatio = dragOffsetXRatio,
                offsetYRatio = dragOffsetYRatio,
                tableWidthRatio = tableWidthRatio,
                tableHeightRatio = tableHeightRatio
            )
            val nextArmed = tableRect.contains(tapOffset.x, tapOffset.y)
            onArmedChange(nextArmed)
            if (!nextArmed) {
                dragActive = false
                hasOverride = false
            }
        }
    }

    val dragModifier = if (armed) {
        Modifier.pointerInput(captureAspect, anchor, tableWidthRatio, tableHeightRatio, dragOffsetXRatio, dragOffsetYRatio) {
            detectDragGestures(
                onDragStart = { start ->
                    val contentRect = computeContentRect(size.width.toFloat(), size.height.toFloat(), captureAspect.ratioF)
                    val tableRect = computeTableRect(
                        contentRect = contentRect,
                        anchor = anchor,
                        offsetXRatio = dragOffsetXRatio,
                        offsetYRatio = dragOffsetYRatio,
                        tableWidthRatio = tableWidthRatio,
                        tableHeightRatio = tableHeightRatio
                    )
                    dragActive = tableRect.contains(start.x, start.y)
                    if (!dragActive) {
                        onArmedChange(false)
                        hasOverride = false
                        return@detectDragGestures
                    }
                    dragLeftPx = tableRect.left - contentRect.left
                    dragTopPx = tableRect.top - contentRect.top
                    dragMaxXPx = (contentRect.width() - tableRect.width()).coerceAtLeast(0f)
                    dragMaxYPx = (contentRect.height() - tableRect.height()).coerceAtLeast(0f)
                    hasOverride = true
                },
                onDragCancel = {
                    dragActive = false
                    overrideReleaseTick += 1
                },
                onDragEnd = {
                    if (dragActive) {
                        val ratioX = if (dragMaxXPx <= 0f) 0 else ((dragLeftPx / dragMaxXPx) * 100f).roundToInt().coerceIn(0, 100)
                        val ratioY = if (dragMaxYPx <= 0f) 0 else ((dragTopPx / dragMaxYPx) * 100f).roundToInt().coerceIn(0, 100)
                        onDragCommit(ratioX, ratioY)
                        overrideReleaseTick += 1
                    }
                    dragActive = false
                },
                onDrag = { change, dragAmount ->
                    if (!dragActive) return@detectDragGestures
                    dragLeftPx = (dragLeftPx + dragAmount.x).coerceIn(0f, dragMaxXPx)
                    dragTopPx = (dragTopPx + dragAmount.y).coerceIn(0f, dragMaxYPx)
                    val ratioX = if (dragMaxXPx <= 0f) 0 else ((dragLeftPx / dragMaxXPx) * 100f).roundToInt().coerceIn(0, 100)
                    val ratioY = if (dragMaxYPx <= 0f) 0 else ((dragTopPx / dragMaxYPx) * 100f).roundToInt().coerceIn(0, 100)
                    dragOffsetXRatio = ratioX
                    dragOffsetYRatio = ratioY
                    onDragPreview(ratioX, ratioY)
                    change.consume()
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(tapModifier)
            .then(dragModifier)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW = size.width
            val boxH = size.height
            if (boxW <= 0f || boxH <= 0f) return@Canvas

            val contentRect = computeContentRect(boxW, boxH, captureAspect.ratioF)
            val baseTableRect = computeTableRect(
                contentRect = contentRect,
                anchor = anchor,
                offsetXRatio = dragOffsetXRatio,
                offsetYRatio = dragOffsetYRatio,
                tableWidthRatio = tableWidthRatio,
                tableHeightRatio = tableHeightRatio
            )
            val base = contentRect.width()
            val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
            val tableH = base * (tableHeightRatio.coerceIn(10, 35) / 100f)
            val drawTableRect = if (hasOverride) {
                RectF(
                    contentRect.left + dragLeftPx,
                    contentRect.top + dragTopPx,
                    contentRect.left + dragLeftPx + tableW,
                    contentRect.top + dragTopPx + tableH
                )
            } else {
                baseTableRect
            }

            if (contentRect.top > 0f) {
                drawRect(DDZColor.PrimaryDark.copy(alpha = 0.7f), Offset(0f, 0f), Size(boxW, contentRect.top))
                drawRect(
                    DDZColor.PrimaryDark.copy(alpha = 0.7f),
                    Offset(0f, contentRect.bottom),
                    Size(boxW, (boxH - contentRect.bottom).coerceAtLeast(0f))
                )
            }
            if (contentRect.left > 0f) {
                drawRect(DDZColor.PrimaryDark.copy(alpha = 0.7f), Offset(0f, contentRect.top), Size(contentRect.left, contentRect.height()))
                drawRect(
                    DDZColor.PrimaryDark.copy(alpha = 0.7f),
                    Offset(contentRect.right, contentRect.top),
                    Size((boxW - contentRect.right).coerceAtLeast(0f), contentRect.height())
                )
            }

            drawRect(DDZColor.Surface.copy(alpha = 0.13f), Offset(contentRect.centerX() - 0.5f, contentRect.top), Size(1f, contentRect.height()))
            drawRect(DDZColor.Surface.copy(alpha = 0.13f), Offset(contentRect.left, contentRect.centerY() - 0.5f), Size(contentRect.width(), 1f))

            val displayAnchor = if (hasOverride || anchor == WatermarkTableAnchor.CUSTOM || dragActive) {
                WatermarkTableAnchor.CUSTOM
            } else {
                anchor
            }

            drawIntoCanvas { canvas ->
                drawWatermarkTableOnCanvas(
                    canvas = canvas.nativeCanvas,
                    bounds = contentRect,
                    cells = watermarkCells,
                    rows = rows.coerceAtLeast(1),
                    cols = cols.coerceAtLeast(1),
                    anchor = displayAnchor,
                    offsetXRatio = if (displayAnchor == WatermarkTableAnchor.CUSTOM) dragOffsetXRatio else 0,
                    offsetYRatio = if (displayAnchor == WatermarkTableAnchor.CUSTOM) dragOffsetYRatio else 0,
                    tableHeightRatio = tableHeightRatio,
                    tableWidthRatio = tableWidthRatio,
                    bgAlpha = bgAlpha.coerceIn(0, 255),
                    bgStyle = bgStyle,
                    valueScale = valueScale.coerceIn(60, 160),
                    textColorMode = textColorMode,
                    manualTextColor = manualTextColor,
                    textAlign = textAlign,
                    rowWeights = rowWeights,
                    colWeights = colWeights,
                    overrideOffsetLeftPx = if (hasOverride) dragLeftPx else null,
                    overrideOffsetTopPx = if (hasOverride) dragTopPx else null
                )
            }

            if (armed) {
                drawRect(
                    color = DDZColor.Surface,
                    topLeft = Offset(drawTableRect.left, drawTableRect.top),
                    size = Size(drawTableRect.width(), drawTableRect.height()),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

private fun computeContentRect(boxWidth: Float, boxHeight: Float, aspectRatio: Float): RectF {
    if (boxWidth <= 0f || boxHeight <= 0f || aspectRatio <= 0f) {
        return RectF(0f, 0f, boxWidth.coerceAtLeast(0f), boxHeight.coerceAtLeast(0f))
    }
    val boxRatio = boxWidth / boxHeight
    return if (aspectRatio > boxRatio) {
        val contentWidth = boxWidth
        val contentHeight = boxWidth / aspectRatio
        val topOffset = (boxHeight - contentHeight) / 2f
        RectF(0f, topOffset, contentWidth, topOffset + contentHeight)
    } else {
        val contentHeight = boxHeight
        val contentWidth = boxHeight * aspectRatio
        val leftOffset = (boxWidth - contentWidth) / 2f
        RectF(leftOffset, 0f, leftOffset + contentWidth, contentHeight)
    }
}

private fun computeTableRect(
    contentRect: RectF,
    anchor: WatermarkTableAnchor,
    offsetXRatio: Int,
    offsetYRatio: Int,
    tableWidthRatio: Int,
    tableHeightRatio: Int
): RectF {
    val base = contentRect.width()
    val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
    val tableH = base * (tableHeightRatio.coerceIn(10, 35) / 100f)
    val maxX = (contentRect.width() - tableW).coerceAtLeast(0f)
    val maxY = (contentRect.height() - tableH).coerceAtLeast(0f)

    val left = when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.BOTTOM_LEFT -> 0f

        WatermarkTableAnchor.TOP_RIGHT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxX

        WatermarkTableAnchor.CUSTOM -> (maxX * (offsetXRatio.coerceIn(0, 100) / 100f)).coerceIn(0f, maxX)
    }

    val top = when (anchor) {
        WatermarkTableAnchor.TOP_LEFT,
        WatermarkTableAnchor.TOP_RIGHT -> 0f

        WatermarkTableAnchor.BOTTOM_LEFT,
        WatermarkTableAnchor.BOTTOM_RIGHT -> maxY

        WatermarkTableAnchor.CUSTOM -> (maxY * (offsetYRatio.coerceIn(0, 100) / 100f)).coerceIn(0f, maxY)
    }

    return RectF(
        contentRect.left + left,
        contentRect.top + top,
        contentRect.left + left + tableW,
        contentRect.top + top + tableH
    )
}
