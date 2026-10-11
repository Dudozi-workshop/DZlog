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
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
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
    onLocked: () -> Unit,
    templates: List<SavedTableTemplate>,
    activeTemplateId: String?,
    switching: Boolean,
    onTemplateSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var showPicker by remember { mutableStateOf(false) }
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
        val hintHeight = DDZLayout.Control.Compact
        val availableWidth = (maxWidth - padding * 2).coerceAtLeast(1.dp)
        val availableHeight = (maxHeight - padding * 2 - headingHeight - hintHeight).coerceAtLeast(1.dp)
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
        Surface(Modifier.size(width + padding * 2, height + padding * 2 + headingHeight + hintHeight),
            shape = RoundedCornerShape(DDZLayout.Radius.Medium), color = DDZColor.SurfaceSoft) {
            Column(Modifier.padding(padding)) {
                if (templateName.isNotBlank()) Box(Modifier.fillMaxWidth().height(headingHeight)) {
                    Row(Modifier.fillMaxSize().clickable(enabled = templates.isNotEmpty() && !switching) { showPicker = true },
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("템플릿  $templateName", style = DDZTypography.Caption,
                            color = DDZColor.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        if (templates.isNotEmpty()) Icon(Icons.Filled.ExpandMore, "템플릿 변경",
                            Modifier.size(DDZLayout.QuickInput.Icon), tint = DDZColor.TextMuted)
                    }
                    DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false },
                        modifier = Modifier.heightIn(max = DDZLayout.QuickInput.MaxListHeight)) {
                        templates.forEach { item ->
                            DropdownMenuItem(text = { Text(item.name, style = DDZTypography.Body,
                                color = if (item.id == activeTemplateId) DDZColor.SageDarkStrong else DDZColor.TextPrimary) },
                                onClick = { showPicker = false; if (item.id != activeTemplateId) onTemplateSelect(item.id) })
                        }
                    }
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
                            .clipToBounds()
                            .then(if (!editable) Modifier.background(DDZColor.Primary.copy(alpha = 0.12f)) else Modifier)
                            .then(if (active) Modifier.background(DDZColor.SageLight.copy(alpha = 0.24f)).border(2.dp, DDZColor.SageDarkStrong) else Modifier)
                            .clickable(enabled = !switching) { if (editable) onSelect(rect.cellId) else onLocked() }
                            .semantics {
                                selected = active
                                contentDescription = "${quickCellPosition(cell)}${if (editable) " 값 수정" else " 자동 값"}"
                            }) {
                            if (!editable) Icon(Icons.Filled.Lock, null, tint = DDZColor.IconMuted,
                                modifier = Modifier.align(Alignment.TopEnd).padding(DDZSpacing.xs).size(DDZLayout.QuickInput.CellMarker))
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().height(hintHeight), contentAlignment = Alignment.CenterStart) {
                    Text("텍스트·숫자 셀만 빠르게 수정할 수 있어요.", style = DDZTypography.Caption,
                        color = DDZColor.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
