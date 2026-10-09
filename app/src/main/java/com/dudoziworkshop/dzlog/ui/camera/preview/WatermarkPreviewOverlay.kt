package com.dudoziworkshop.dzlog.ui.camera.preview

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderAdapter
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPayload
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.feature.table.render.buildCameraPreviewPlacement
import com.dudoziworkshop.dzlog.feature.table.render.buildCameraTableSceneFromPlacement
import com.dudoziworkshop.dzlog.feature.table.render.moveCameraTableSceneToVisibleBounds
import com.dudoziworkshop.dzlog.feature.table.render.computeRatioOnlyTableShape
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.watermark.boundsRectFromOffset
import com.dudoziworkshop.dzlog.watermark.computeBoundsSize
import com.dudoziworkshop.dzlog.watermark.computeWatermarkBoundsRect
import com.dudoziworkshop.dzlog.watermark.computeWatermarkTableLayout
import com.dudoziworkshop.dzlog.watermark.rawRectFromBounds

/**
 * [WatermarkPreviewOverlay]
 * - 목적: 촬영 화면에서 워터마크 미리보기 렌더
 */
@Composable
fun WatermarkPreviewOverlay(
    enabled: Boolean,
    request: CaptureRequest,
    previewContentRect: RectF?,
    overrideOffsetPx: Offset?,
    dragVisibleOffsetPx: Offset?,
    activeHandleCorner: ResizeHandleCorner?,
    isArmed: Boolean,
    onBoundsRectChange: (RectF?) -> Unit,
    onRawRectChange: (RectF?) -> Unit
) {
    if (!enabled || previewContentRect == null) {
        LaunchedEffect(enabled, previewContentRect) {
            onBoundsRectChange(null)
            onRawRectChange(null)
        }
        return
    }

    // Camera Preview 정책: 외곽 비율은 표 자체 속성(SSOT),
    // 위치/회전은 촬영 배치 속성으로 별도 주입한다.
    val cameraPreviewShape = computeRatioOnlyTableShape(
        tableWidthRatio = request.watermark.tableWidthRatio,
        tableHeightRatio = request.watermark.tableHeightRatio,
        maxWidthRatio = request.watermark.tableWidthRatio,
        maxHeightRatio = request.watermark.tableHeightRatio,
    )

    val layout = computeWatermarkTableLayout(
        bounds = previewContentRect,
        anchor = request.watermark.anchor,
        offsetXRatio = request.watermark.offsetXRatio,
        offsetYRatio = request.watermark.offsetYRatio,
        tableHeightRatio = cameraPreviewShape.tableHeightRatio,
        tableWidthRatio = cameraPreviewShape.tableWidthRatio
    )

    val baseRawRect = layout.rect
    val rawW = baseRawRect.width()
    val rawH = baseRawRect.height()
    val (boundsW, boundsH) = computeBoundsSize(rawW, rawH, request.watermark.rotationCwDeg)
    val boundsRect = if (request.watermark.anchor == WatermarkTableAnchor.CUSTOM && overrideOffsetPx != null) {
        boundsRectFromOffset(
            captureRect = previewContentRect,
            boundsW = boundsW,
            boundsH = boundsH,
            boundsLeftPx = overrideOffsetPx.x,
            boundsTopPx = overrideOffsetPx.y
        )
    } else {
        computeWatermarkBoundsRect(baseRawRect, request.watermark.rotationCwDeg)
    }
    val rawRect = rawRectFromBounds(boundsRect, rawW, rawH)
    val overrideRawLeftPx = rawRect.left - previewContentRect.left
    val overrideRawTopPx = rawRect.top - previewContentRect.top

    val cells = request.watermarkCells
    val placement = buildCameraPreviewPlacement(
            anchor = request.watermark.anchor,
            offsetXRatio = request.watermark.offsetXRatio,
            offsetYRatio = request.watermark.offsetYRatio,
            tableWidthRatio = cameraPreviewShape.tableWidthRatio,
            tableHeightRatio = cameraPreviewShape.tableHeightRatio,
            overrideOffsetLeftPx = if (request.watermark.anchor == WatermarkTableAnchor.CUSTOM) overrideRawLeftPx else null,
            overrideOffsetTopPx = if (request.watermark.anchor == WatermarkTableAnchor.CUSTOM) overrideRawTopPx else null,
            rotationCwDeg = request.watermark.rotationCwDeg,
        )
        val baseline = buildCameraTableSceneFromPlacement(
            bounds = previewContentRect,
            placement = placement,
            cells = cells,
            templateCells = request.tableTemplate.cells,
            rows = request.tableTemplate.rows,
            cols = request.tableTemplate.cols,
            valueScale = request.watermark.valueScale,
            baseScaleRatio = cameraPreviewShape.tableWidthRatio,
            rowWeights = request.tableTemplate.rowWeights,
            colWeights = request.tableTemplate.colWeights,
        )
        val rendered = dragVisibleOffsetPx?.let { offset ->
            moveCameraTableSceneToVisibleBounds(
                rendered = baseline,
                photoBounds = previewContentRect,
                rotationCwDeg = request.watermark.rotationCwDeg,
                leftPx = offset.x,
                topPx = offset.y,
                templateCells = request.tableTemplate.cells,
                rows = request.tableTemplate.rows,
                cols = request.tableTemplate.cols,
            )
        } ?: baseline
    val visibleBounds = computeWatermarkBoundsRect(rendered.scene.tableRect, request.watermark.rotationCwDeg)
    LaunchedEffect(visibleBounds.left, visibleBounds.top, visibleBounds.right, visibleBounds.bottom) {
        onBoundsRectChange(RectF(visibleBounds))
        onRawRectChange(RectF(rendered.scene.tableRect))
    }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        drawIntoCanvas { canvas ->
            TableRenderAdapter.drawScene(
                canvas = canvas.nativeCanvas,
                scene = rendered.scene,
                payload = TableRenderPayload(
                    rows = request.tableTemplate.rows,
                    cols = request.tableTemplate.cols,
                    rowWeights = rendered.resolvedLayout.finalRowWeights,
                    colWeights = rendered.resolvedLayout.finalColWeights,
                    cells = cells,
                ),
                style = TableRenderStyle(
                    bgAlpha = request.watermark.tableBgAlpha,
                    bgStyle = request.watermark.bgStyle,
                    valueScale = request.watermark.valueScale,
                    textColorMode = request.watermark.textColorMode,
                    manualTextColor = request.watermark.manualTextColor,
                    textAlign = request.watermark.textAlign,
                    drawGrid = request.watermark.gridEnabled,
                ),
                rotationCwDeg = request.watermark.rotationCwDeg,
            )
            if (isArmed) {
                drawRect(
                    color = DDZColor.Surface.copy(alpha = 0.85f),
                    topLeft = Offset(visibleBounds.left, visibleBounds.top),
                    size = androidx.compose.ui.geometry.Size(visibleBounds.width(), visibleBounds.height()),
                    style = Stroke(width = 2.dp.toPx()),
                )
                val corner = activeHandleCorner ?: chooseResizeHandleCorner(visibleBounds, previewContentRect)
                val center = handleCornerPoint(visibleBounds, corner)
                drawCircle(
                    color = DDZColor.Surface,
                    radius = 12.dp.toPx(),
                    center = center,
                )
                drawCircle(
                    color = DDZColor.SageDarkStrong,
                    radius = 11.dp.toPx(),
                    center = center,
                )
                val reach = 6.dp.toPx()
                val inner = 2.5.dp.toPx()
                drawLine(DDZColor.Surface, Offset(center.x - reach, center.y), Offset(center.x + reach, center.y), 1.3.dp.toPx())
                drawLine(DDZColor.Surface, Offset(center.x, center.y - reach), Offset(center.x, center.y + reach), 1.3.dp.toPx())
                listOf(
                    Offset(-reach, 0f) to Offset(-inner, -inner),
                    Offset(-reach, 0f) to Offset(-inner, inner),
                    Offset(reach, 0f) to Offset(inner, -inner),
                    Offset(reach, 0f) to Offset(inner, inner),
                    Offset(0f, -reach) to Offset(-inner, -inner),
                    Offset(0f, -reach) to Offset(inner, -inner),
                    Offset(0f, reach) to Offset(-inner, inner),
                    Offset(0f, reach) to Offset(inner, inner),
                ).forEach { (tip, wing) ->
                    drawLine(DDZColor.Surface, center + tip, center + wing, 1.2.dp.toPx())
                }
            }
        }
    }
}

