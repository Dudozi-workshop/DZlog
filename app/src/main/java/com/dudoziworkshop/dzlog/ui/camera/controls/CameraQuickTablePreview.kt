package com.dudoziworkshop.dzlog.ui.camera.controls

import android.graphics.RectF
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import com.dudoziworkshop.dzlog.domain.model.WatermarkTextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellPosition
import com.dudoziworkshop.dzlog.feature.table.policy.quickRootCells
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellLinks
import com.dudoziworkshop.dzlog.feature.table.policy.quickEditingSizes
import com.dudoziworkshop.dzlog.feature.table.policy.quickEditingRows
import com.dudoziworkshop.dzlog.feature.table.render.*
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Reading-first editing canvas. Its wrapping and markers never alter capture geometry. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CameraQuickTablePreview(
    template: TableTemplateState,
    templateName: String,
    cells: List<WatermarkBuilder.WatermarkCell>,
    style: TableRenderStyle,
    selectedId: String?,
    editableIds: Set<String>,
    onSelect: (String) -> Unit,
    onLocked: (String) -> Unit,
    templates: List<SavedTableTemplate>,
    activeTemplateId: String?,
    switching: Boolean,
    onTemplateSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    var showPicker by remember { mutableStateOf(false) }
    val roots = remember(template.cells) { quickRootCells(template) }
    val rootCells = remember(roots) { roots.map { RenderRootCell(it.cellId, it.rowIndex, it.colIndex, it.rowSpan, it.colSpan) } }
    val values = remember(template.cells, cells) { template.cells.mapIndexed { index, cell ->
        cell.cellId to cells.getOrNull(index)?.valueText.orEmpty()
    }.toMap() }
    val vertical = rememberScrollState()
    val horizontal = rememberScrollState()
    val measurer = rememberTextMeasurer(cacheSize = template.rows * template.cols)
    val textStyle = DDZTypography.Body.copy(fontWeight = FontWeight.Bold, textAlign = when (style.textAlign) {
        WatermarkTextAlign.CENTER -> TextAlign.Center
        WatermarkTextAlign.RIGHT -> TextAlign.End
        else -> TextAlign.Start
    })
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        val padding = DDZSpacing.controlGap
        val headingHeight = if (templateName.isBlank()) 0.dp else DDZLayout.Control.Compact
        val hintHeight = DDZLayout.Control.Compact
        val availableWidth = (maxWidth - padding * 2).coerceAtLeast(1.dp)
        val colSizes = remember(availableWidth, template.colWeights, template.cols, density) {
            quickEditingSizes(template.cols, template.colWeights, with(density) { availableWidth.toPx() },
                with(density) { DDZLayout.QuickInput.MinCellWidth.toPx() })
        }
        val markerBand = DDZLayout.QuickInput.CellMarker + DDZSpacing.xs * 2
        val cellPadding = DDZSpacing.itemGap
        val rowSizes = remember(roots, values, colSizes, template.rowWeights, textStyle, density, measurer) {
            val base = quickEditingSizes(template.rows, template.rowWeights, 0f,
                with(density) { DDZLayout.QuickInput.RowHeight.toPx() })
            val required = roots.map { cell ->
                val width = colSizes.drop(cell.colIndex).take(cell.colSpan).sum() - with(density) { cellPadding.toPx() * 2 }
                val layout = measurer.measure(values[cell.cellId].orEmpty(), style = textStyle,
                    constraints = Constraints(maxWidth = width.toInt().coerceAtLeast(1)))
                layout.size.height + with(density) { (markerBand + cellPadding * 2).toPx() }
            }
            quickEditingRows(base, roots, required)
        }
        val widthPx = colSizes.sum()
        val heightPx = rowSizes.sum()
        val scene = remember(colSizes, rowSizes, rootCells) {
            buildRenderedTableScene(RectF(0f, 0f, widthPx, heightPx), template.rows, template.cols,
                rowSizes, colSizes, rootCells)
        }
        val width = with(density) { widthPx.toDp() }
        val height = with(density) { heightPx.toDp() }
        Surface(Modifier.fillMaxWidth().heightIn(max = maxHeight),
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
                Box(Modifier.fillMaxWidth().weight(1f, fill = false).clipToBounds()
                    .verticalScroll(vertical).horizontalScroll(horizontal)) {
                    Box(Modifier.size(width, height)) {
                        scene.rootRects.forEach { rect ->
                            val cell = roots.first { it.cellId == rect.cellId }
                            val editable = rect.cellId in editableIds
                            val active = selectedId == rect.cellId
                            val links = quickCellLinks(template, rect.cellId)
                            val requester = remember(rect.cellId) { BringIntoViewRequester() }
                            // Follow cell navigation / keyboard resizing, never zoom on each keystroke.
                            LaunchedEffect(active, maxHeight, maxWidth) {
                                if (active) {
                                    withFrameNanos { }
                                    requester.bringIntoView()
                                }
                            }
                            Box(Modifier.offset(x = with(density) { rect.left.toDp() }, y = with(density) { rect.top.toDp() })
                                .size(with(density) { (rect.right - rect.left).toDp() }, with(density) { (rect.bottom - rect.top).toDp() })
                                .bringIntoViewRequester(requester).clipToBounds()
                                .background(if (active) DDZColor.SelectedSoft else DDZColor.Surface)
                                .then(if (!editable) Modifier.background(DDZColor.Primary.copy(alpha = 0.12f)) else Modifier)
                                .border(if (active) DDZLayout.QuickInput.SelectionBorder else 1.dp,
                                    if (active) DDZColor.SelectionEmphasis else DDZColor.Border)
                                .clickable(enabled = !switching) { if (editable) onSelect(rect.cellId) else onLocked(rect.cellId) }
                                .semantics {
                                    selected = active
                                    contentDescription = "${quickCellPosition(cell)}${if (editable) " 값 수정" else " 자동 값"} ${links.description}"
                                }) {
                                if (!editable) Icon(Icons.Filled.Lock, null, tint = DDZColor.IconMuted,
                                    modifier = Modifier.align(Alignment.TopStart).padding(DDZSpacing.xs).size(DDZLayout.QuickInput.CellMarker))
                                Row(Modifier.align(Alignment.TopEnd).padding(DDZSpacing.xs),
                                    horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs)) {
                                    val tint = if (active) DDZColor.SelectionEmphasis else DDZColor.IconMuted
                                    if (links.fileName) Icon(Icons.Filled.Description, null,
                                        Modifier.size(DDZLayout.QuickInput.CellMarker), tint = tint)
                                    if (links.path) Icon(Icons.Filled.Folder, null,
                                        Modifier.size(DDZLayout.QuickInput.CellMarker), tint = tint)
                                }
                                Box(Modifier.fillMaxSize().padding(top = markerBand)
                                    .padding(horizontal = cellPadding, vertical = cellPadding), contentAlignment = Alignment.Center) {
                                    Text(values[rect.cellId].orEmpty(), style = textStyle, color = DDZColor.TextPrimary,
                                        modifier = Modifier.fillMaxWidth())
                                }
                            }
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
