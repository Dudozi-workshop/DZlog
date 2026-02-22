package com.example.dzlog.ui.table.watermark

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.domain.watermark.WatermarkBuilder
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.watermark.computeWatermarkTableRect
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
    drawGrid: Boolean,
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
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val previewContentRect = remember(canvasSize, captureAspect) {
        computeContentRect(canvasSize.width.toFloat(), canvasSize.height.toFloat(), captureAspect.ratioF)
    }
    val tableWidthPx = remember(previewContentRect, tableWidthRatio) {
        (previewContentRect.width() * (tableWidthRatio.coerceIn(40, 100) / 100f)).roundToInt().coerceAtLeast(1)
    }
    val tableHeightPx = remember(previewContentRect, tableHeightRatio) {
        (previewContentRect.width() * (tableHeightRatio.coerceIn(10, 200) / 100f)).roundToInt().coerceAtLeast(1)
    }
    val normalizedTableHeightRatio = remember(tableWidthRatio, tableHeightRatio) {
        if (tableWidthRatio <= 0) 10 else ((tableHeightRatio * 100f) / tableWidthRatio)
            .roundToInt()
            .coerceIn(10, 200)
    }

    val tableBitmap = remember(
        watermarkCells,
        rows,
        cols,
        rowWeights,
        colWeights,
        bgStyle,
        bgAlpha,
        valueScale,
        textColorMode,
        manualTextColor,
        textAlign,
        tableWidthPx,
        tableHeightPx,
        normalizedTableHeightRatio,
        drawGrid
    ) {
        createTableBitmap(
            width = tableWidthPx,
            height = tableHeightPx,
            cells = watermarkCells,
            rows = rows,
            cols = cols,
            rowWeights = rowWeights,
            colWeights = colWeights,
            bgStyle = bgStyle,
            bgAlpha = bgAlpha,
            valueScale = valueScale,
            textColorMode = textColorMode,
            manualTextColor = manualTextColor,
            textAlign = textAlign,
            normalizedTableHeightRatio = normalizedTableHeightRatio,
            drawGrid = drawGrid
        )
    }

    DisposableEffect(tableBitmap) {
        onDispose { tableBitmap?.recycle() }
    }

    LaunchedEffect(offsetXRatio, offsetYRatio, dragOffsetXRatio, dragOffsetYRatio, dragActive, hasOverride) {
        if (!dragActive && hasOverride) {
            if (offsetXRatio == dragOffsetXRatio && offsetYRatio == dragOffsetYRatio) {
                hasOverride = false
            }
        }
    }

    LaunchedEffect(overrideReleaseTick) {
        if (overrideReleaseTick == 0) return@LaunchedEffect
        delay(400)
        if (!dragActive && hasOverride) {
            hasOverride = false
        }
    }

    val tapModifier = Modifier.pointerInput(captureAspect, anchor, offsetXRatio, offsetYRatio, tableWidthRatio, tableHeightRatio) {
        detectTapGestures { tapOffset ->
            val contentRect = computeContentRect(size.width.toFloat(), size.height.toFloat(), captureAspect.ratioF)
            val tableRect = computeWatermarkTableRect(
                bounds = contentRect,
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
        Modifier.pointerInput(captureAspect, anchor, tableWidthRatio, tableHeightRatio) {
            detectDragGestures(
                onDragStart = { start ->
                    val contentRect = computeContentRect(size.width.toFloat(), size.height.toFloat(), captureAspect.ratioF)
                    val tableRect = computeWatermarkTableRect(
                        bounds = contentRect,
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
                        dragOffsetXRatio = ratioX
                        dragOffsetYRatio = ratioY
                        onDragCommit(ratioX, ratioY)
                        overrideReleaseTick += 1
                    }
                    dragActive = false
                },
                onDrag = { change, dragAmount ->
                    if (!dragActive) return@detectDragGestures
                    change.consume()
                    dragLeftPx = (dragLeftPx + dragAmount.x).coerceIn(0f, dragMaxXPx)
                    dragTopPx = (dragTopPx + dragAmount.y).coerceIn(0f, dragMaxYPx)
                    val ratioX = if (dragMaxXPx <= 0f) 0 else ((dragLeftPx / dragMaxXPx) * 100f).roundToInt().coerceIn(0, 100)
                    val ratioY = if (dragMaxYPx <= 0f) 0 else ((dragTopPx / dragMaxYPx) * 100f).roundToInt().coerceIn(0, 100)
                    dragOffsetXRatio = ratioX
                    dragOffsetYRatio = ratioY
                    // 드래그 중에는 로컬 상태만 갱신(상위 상태/SSOT 저장 금지)
                }
            )
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .then(tapModifier)
            .then(dragModifier)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val boxW = size.width
            val boxH = size.height
            val contentRect = computeContentRect(boxW, boxH, captureAspect.ratioF)
            val baseTableRect = computeWatermarkTableRect(
                bounds = contentRect,
                anchor = anchor,
                offsetXRatio = dragOffsetXRatio,
                offsetYRatio = dragOffsetYRatio,
                tableWidthRatio = tableWidthRatio,
                tableHeightRatio = tableHeightRatio
            )
            val base = contentRect.width()
            val tableW = base * (tableWidthRatio.coerceIn(40, 100) / 100f)
            val tableH = base * (tableHeightRatio.coerceIn(10, 200) / 100f)
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

            drawIntoCanvas { canvas ->
                val bitmap = tableBitmap
                if (bitmap != null && !bitmap.isRecycled) {
                    canvas.nativeCanvas.drawBitmap(bitmap, drawTableRect.left, drawTableRect.top, null)
                } else {
                    val displayAnchor = if (hasOverride || anchor == WatermarkTableAnchor.CUSTOM || dragActive) {
                        WatermarkTableAnchor.CUSTOM
                    } else {
                        anchor
                    }
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
                        drawGrid = drawGrid,
                        rowWeights = rowWeights,
                        colWeights = colWeights,
                        overrideOffsetLeftPx = if (hasOverride) dragLeftPx else null,
                        overrideOffsetTopPx = if (hasOverride) dragTopPx else null
                    )
                }
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

private fun createTableBitmap(
    width: Int,
    height: Int,
    cells: List<WatermarkBuilder.WatermarkCell>,
    rows: Int,
    cols: Int,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    bgStyle: Int,
    bgAlpha: Int,
    valueScale: Int,
    textColorMode: Int,
    manualTextColor: Int,
    textAlign: Int,
    normalizedTableHeightRatio: Int,
    drawGrid: Boolean
): Bitmap? {
    if (width <= 0 || height <= 0) return null

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    drawWatermarkTableOnCanvas(
        canvas = canvas,
        bounds = RectF(0f, 0f, width.toFloat(), height.toFloat()),
        cells = cells,
        rows = rows.coerceAtLeast(1),
        cols = cols.coerceAtLeast(1),
        anchor = WatermarkTableAnchor.TOP_LEFT,
        offsetXRatio = 0,
        offsetYRatio = 0,
        tableHeightRatio = normalizedTableHeightRatio,
        tableWidthRatio = 100,
        bgAlpha = bgAlpha.coerceIn(0, 255),
        bgStyle = bgStyle,
        valueScale = valueScale.coerceIn(60, 160),
        textColorMode = textColorMode,
        manualTextColor = manualTextColor,
        textAlign = textAlign,
        drawGrid = drawGrid,
        rowWeights = rowWeights,
        colWeights = colWeights
    )
    return bitmap
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
