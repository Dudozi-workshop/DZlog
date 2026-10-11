package com.dudoziworkshop.dzlog.ui.camera.controls

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellPosition
import com.dudoziworkshop.dzlog.feature.table.policy.quickRootCells
import com.dudoziworkshop.dzlog.feature.table.render.*
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Reuse the capture renderer; freeze initial geometry so typing never resizes this view. */
@Composable
internal fun CameraQuickTablePreview(
    template: TableTemplateState,
    templateName: String,
    cells: List<WatermarkBuilder.WatermarkCell>,
    style: TableRenderStyle,
    captureAspect: CaptureAspect,
    widthRatio: Int,
    heightRatio: Int,
    selectedId: String?,
    editableIds: Set<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val referenceWidth = with(density) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)
    val initialTemplate = remember { template }
    val roots = remember { quickRootCells(initialTemplate) }
    val baseline = remember {
        buildCameraTableSceneFromPlacement(
            bounds = RectF(0f, 0f, referenceWidth, referenceWidth / captureAspect.ratioF),
            placement = buildDesignPreviewPlacement(widthRatio, heightRatio),
            cells = cells, templateCells = initialTemplate.cells,
            rows = initialTemplate.rows, cols = initialTemplate.cols,
            valueScale = style.valueScale, baseScaleRatio = widthRatio,
            rowWeights = initialTemplate.rowWeights, colWeights = initialTemplate.colWeights,
        )
    }
    val aspect = baseline.scene.tableRect.width() / baseline.scene.tableRect.height().coerceAtLeast(1f)
    val rootCells = remember { roots.map { RenderRootCell(it.cellId, it.rowIndex, it.colIndex, it.rowSpan, it.colSpan) } }
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val padding = DDZSpacing.controlGap
        val headingHeight = if (templateName.isBlank()) 0.dp else DDZLayout.Control.Compact
        val availableWidth = (maxWidth - padding * 2).coerceAtLeast(1.dp)
        val availableHeight = (maxHeight - padding * 2 - headingHeight).coerceAtLeast(1.dp)
        val width = minOf(availableWidth, availableHeight * aspect).coerceAtLeast(1.dp)
        val height = (width / aspect).coerceAtLeast(1.dp)
        val widthPx = with(density) { width.toPx() }
        val heightPx = with(density) { height.toPx() }
        val scene = remember(widthPx, heightPx) {
            buildRenderedTableScene(RectF(0f, 0f, widthPx, heightPx), initialTemplate.rows, initialTemplate.cols,
                baseline.resolvedLayout.finalRowWeights, baseline.resolvedLayout.finalColWeights, rootCells)
        }
        val payload = remember(cells) { TableRenderPayload(initialTemplate.rows, initialTemplate.cols,
            baseline.resolvedLayout.finalRowWeights, baseline.resolvedLayout.finalColWeights, cells) }
        Surface(Modifier.size(width + padding * 2, height + padding * 2 + headingHeight),
            shape = RoundedCornerShape(DDZLayout.Radius.Medium), color = DDZColor.SurfaceSoft) {
            Column(Modifier.padding(padding)) {
                if (templateName.isNotBlank()) Box(Modifier.fillMaxWidth().height(headingHeight)) {
                    Text("템플릿  $templateName", style = DDZTypography.Caption,
                        color = DDZColor.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(Modifier.size(width, height)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas -> TableRenderAdapter.drawScene(canvas.nativeCanvas, scene, payload,
                            style.copy(drawGrid = true)) }
                    }
                    scene.rootRects.forEach { rect ->
                        val cell = template.cells.first { it.cellId == rect.cellId }
                        val editable = rect.cellId in editableIds
                        val active = selectedId == rect.cellId
                        Box(Modifier.offset(x = with(density) { rect.left.toDp() }, y = with(density) { rect.top.toDp() })
                            .size(with(density) { (rect.right - rect.left).toDp() }, with(density) { (rect.bottom - rect.top).toDp() })
                            .then(if (active) Modifier.background(DDZColor.SageLight.copy(alpha = 0.24f)).border(2.dp, DDZColor.SageDarkStrong) else Modifier)
                            .clickable(enabled = editable) { onSelect(rect.cellId) }
                            .semantics {
                                selected = active
                                contentDescription = "${quickCellPosition(cell)}${if (editable) " 값 수정" else " 자동 값"}"
                            })
                    }
                }
            }
        }
    }
}
