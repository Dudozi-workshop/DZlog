package com.dudoziworkshop.dzlog.ui.camera.controls

import com.dudoziworkshop.dzlog.feature.help.ui.*
import android.graphics.RectF
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import kotlinx.coroutines.delay
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.feature.table.policy.quickPreviewFit
import com.dudoziworkshop.dzlog.feature.table.policy.quickScrollThumb
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
import androidx.compose.ui.text.font.FontWeight
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellPosition
import com.dudoziworkshop.dzlog.feature.table.policy.quickRootCells
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellLinks
import com.dudoziworkshop.dzlog.feature.table.policy.quickMarkersFit
import com.dudoziworkshop.dzlog.watermark.computeBaseTextSizeFromRenderedTable
import com.dudoziworkshop.dzlog.watermark.applyValueScaleFactor
import com.dudoziworkshop.dzlog.feature.table.render.*
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Exact capture rendering; fit all by default, pan only after explicit magnification. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CameraQuickTablePreview(
    template: TableTemplateState,
    templateName: String,
    cells: List<ResolvedCell>,
    style: TableRenderStyle,
    captureAspect: CaptureAspect,
    widthRatio: Int,
    heightRatio: Int,
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
    val guideTemplateMenu = LocalTutorialReadOnly.current && LocalTutorialLocation.current?.pageId == "template" && LocalTutorialDemoProgress.current < 0.5f
    val density = LocalDensity.current
    var showPicker by remember { mutableStateOf(false) }
    var zoomFactor by remember { mutableStateOf(1f) }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val renderCells = remember(cells, template.cells) {
        val values = cells.associate { it.id to it.resolvedText }
        template.cells.sortedWith(compareBy({ it.rowIndex }, { it.colIndex })).map {
            WatermarkBuilder.WatermarkCell(values[it.cellId].orEmpty())
        }
    }
    // Values repaint immediately; resize after a short typing pause to avoid repeated refits.
    var layoutCells by remember { mutableStateOf(renderCells) }
    LaunchedEffect(renderCells) { delay(250); layoutCells = renderCells }
    LaunchedEffect(zoomFactor) {
        if (zoomFactor == 1f) { horizontal.scrollTo(0); vertical.scrollTo(0) }
    }
    val configuration = LocalConfiguration.current
    val referenceWidth = with(density) { configuration.screenWidthDp.dp.toPx() }.coerceAtLeast(1f)
    val roots = remember(template.cells) { quickRootCells(template) }
    val baseline = remember(layoutCells, template.rows, template.cols, template.rowWeights, template.colWeights, roots, referenceWidth, captureAspect, style.valueScale, widthRatio, heightRatio) {
        buildCameraTableSceneFromPlacement(
            bounds = RectF(0f, 0f, referenceWidth, referenceWidth / captureAspect.ratioF),
            placement = buildDesignPreviewPlacement(widthRatio, heightRatio),
            cells = layoutCells, templateCells = template.cells,
            rows = template.rows, cols = template.cols,
            valueScale = style.valueScale, baseScaleRatio = widthRatio,
            rowWeights = template.rowWeights, colWeights = template.colWeights,
        )
    }
    val aspect = baseline.scene.tableRect.width() / baseline.scene.tableRect.height().coerceAtLeast(1f)
    val rootCells = remember(roots) { roots.map { RenderRootCell(it.cellId, it.rowIndex, it.colIndex, it.rowSpan, it.colSpan) } }
    BoxWithConstraints(modifier, contentAlignment = Alignment.TopCenter) {
        val viewportMaxHeight = maxHeight
        val viewportMaxWidth = maxWidth
        val padding = DDZSpacing.itemGap
        val headingHeight = DDZLayout.Control.Compact
        val hintHeight = 24.dp
        val availableWidth = (maxWidth - padding * 2).coerceAtLeast(1.dp)
        val availableHeight = (maxHeight - padding * 2 - headingHeight - hintHeight).coerceAtLeast(1.dp)
        val fit = quickPreviewFit(with(density) { availableWidth.toPx() }, with(density) { availableHeight.toPx() }, aspect)
        val fitWidth = with(density) { fit.first.toDp() }
        val fitHeight = with(density) { fit.second.toDp() }
        val width = fitWidth * zoomFactor
        val height = fitHeight * zoomFactor
        val viewportHeight = if (zoomFactor > 1f) availableHeight else fitHeight
        val widthPx = with(density) { width.toPx() }
        val heightPx = with(density) { height.toPx() }
        val scene = remember(widthPx, heightPx, baseline, rootCells) {
            buildRenderedTableScene(RectF(0f, 0f, widthPx, heightPx), template.rows, template.cols,
                baseline.resolvedLayout.finalRowWeights, baseline.resolvedLayout.finalColWeights, rootCells)
        }
        val payload = remember(renderCells, baseline) { TableRenderPayload(template.rows, template.cols,
            baseline.resolvedLayout.finalRowWeights, baseline.resolvedLayout.finalColWeights, renderCells) }
        val lineHeight = remember(widthPx, heightPx, template.rows, template.cols, style.valueScale) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = Typeface.DEFAULT_BOLD
                textSize = applyValueScaleFactor(computeBaseTextSizeFromRenderedTable(
                    widthPx, heightPx, template.rows, template.cols), style.valueScale)
            }.fontMetrics.let { it.descent - it.ascent }
        }
        val markerPx = with(density) { DDZLayout.QuickInput.CellMarker.toPx() }
        val gapPx = with(density) { DDZSpacing.xs.toPx() }
        Surface(Modifier.size(availableWidth + padding * 2, viewportHeight + padding * 2 + headingHeight + hintHeight),
            shape = RoundedCornerShape(DDZLayout.Radius.Medium), color = DDZColor.SurfaceSoft) {
            Column(Modifier.padding(padding)) {
                // DZQ-002 device feedback: keep this a flat, compact toolbar.
                // Emphasize the template name instead of drawing a nested card.
                Box(Modifier.fillMaxWidth().height(headingHeight)) {
                    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        Row(
                            Modifier.weight(1f).fillMaxHeight()
                                .clickable(enabled = templates.isNotEmpty() && !switching) { showPicker = true }
                                .padding(start = DDZSpacing.xs, end = DDZSpacing.xs),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs),
                        ) {
                            Text("템플릿", style = DDZTypography.Caption,
                                color = DDZColor.TextSecondary, maxLines = 1)
                            Text(templateName.ifBlank { "기본" },
                                style = DDZTypography.Body.copy(fontWeight = FontWeight.SemiBold),
                                color = DDZColor.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f))
                            if (templates.isNotEmpty()) Icon(Icons.Filled.ExpandMore, "템플릿 변경",
                                Modifier.size(DDZLayout.QuickInput.Icon), tint = DDZColor.PrimaryDark)
                        }
                        Box(Modifier.width(1.dp).height(headingHeight - 10.dp)
                            .background(DDZColor.Border))
                        IconButton(enabled = !switching, onClick = {
                            zoomFactor = if (zoomFactor > 1f) 1f else {
                                val base = applyValueScaleFactor(computeBaseTextSizeFromRenderedTable(
                                    fit.first, fit.second, template.rows, template.cols), style.valueScale)
                                (with(density) { DDZTypography.Body.fontSize.toPx() } / base.coerceAtLeast(1f)).coerceIn(2f, 8f)
                            }
                        }, modifier = Modifier.size(headingHeight)) {
                            Icon(if (zoomFactor > 1f) Icons.Filled.ZoomOut else Icons.Filled.ZoomIn,
                                if (zoomFactor > 1f) "표 전체 보기" else "표 확대",
                                Modifier.size(DDZLayout.QuickInput.Icon), tint = DDZColor.TextSecondary)
                        }
                    }
                    QuickTemplateMenu(expanded = showPicker, guideExpanded = guideTemplateMenu,
                        onDismiss = { showPicker = false }) {
                        if (guideTemplateMenu) {
                            DropdownMenuItem(text = { Text("기능 안내 예시") }, onClick = {})
                            DropdownMenuItem(text = { Text("다른 표 양식 · 예시") }, onClick = {})
                        }
                        templates.forEach { item ->
                            DropdownMenuItem(text = { Text(item.name, style = DDZTypography.Body,
                                color = if (item.id == activeTemplateId) DDZColor.SageDarkStrong else DDZColor.TextPrimary) },
                                onClick = { showPicker = false; if (item.id != activeTemplateId) onTemplateSelect(item.id) })
                        }
                    }
                }
                Box(Modifier.size(availableWidth, viewportHeight).clipToBounds()) {
                Box(Modifier.fillMaxSize().then(if (zoomFactor > 1f)
                    Modifier.verticalScroll(vertical).horizontalScroll(horizontal) else Modifier),
                    contentAlignment = Alignment.TopCenter) {
                Box(Modifier.size(width, height)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawIntoCanvas { canvas -> TableRenderAdapter.drawScene(canvas.nativeCanvas, scene, payload,
                            style.copy(drawGrid = true)) }
                    }
                    scene.rootRects.forEach { rect ->
                        val cell = template.cells.first { it.cellId == rect.cellId }
                        val editable = rect.cellId in editableIds
                        val active = selectedId == rect.cellId
                        val links = quickCellLinks(template, rect.cellId)
                        val requester = remember(rect.cellId) { BringIntoViewRequester() }
                        LaunchedEffect(active, zoomFactor, viewportMaxHeight, viewportMaxWidth) {
                            if (active && zoomFactor > 1f) { withFrameNanos { }; requester.bringIntoView() }
                        }
                        val markerCount = (links.count + if (editable) 0 else 1).coerceAtLeast(1)
                        val valueTop = ((rect.bottom - rect.top) - lineHeight) / 2f
                        val scaledMarker = minOf(markerPx, valueTop - gapPx * 2,
                            ((rect.right - rect.left) - gapPx * (markerCount + 1)) / markerCount).coerceAtLeast(0f)
                        val markerSize = with(density) { scaledMarker.toDp() }
                        val readableMarker = markerSize >= DDZLayout.QuickInput.MinCellMarker
                        val showLock = !editable && readableMarker && quickMarkersFit(rect.right - rect.left,
                            valueTop, true, 0, scaledMarker, gapPx)
                        val showMarkers = readableMarker && quickMarkersFit(rect.right - rect.left,
                            valueTop, !editable, links.count, scaledMarker, gapPx)
                        Box(Modifier.offset(x = with(density) { rect.left.toDp() }, y = with(density) { rect.top.toDp() })
                            .size(with(density) { (rect.right - rect.left).toDp() }, with(density) { (rect.bottom - rect.top).toDp() })
                            .bringIntoViewRequester(requester).clipToBounds()
                            .then(if (!editable) Modifier.background(DDZColor.Primary.copy(alpha = 0.12f)) else Modifier)
                            .then(if (active) Modifier.background(DDZColor.SelectionOverlay).border(DDZLayout.QuickInput.SelectionBorder, DDZColor.SelectionEmphasis) else Modifier)
                            .clickable(enabled = !switching) { if (editable) onSelect(rect.cellId) else onLocked(rect.cellId) }
                            .semantics {
                                selected = active
                                contentDescription = "${quickCellPosition(cell)}${if (editable) " 값 수정" else " 자동 값"} ${links.description}"
                            }) {
                            if (showLock || showMarkers) {
                                if (showLock) Icon(Icons.Filled.Lock, null, tint = DDZColor.IconMuted,
                                    modifier = Modifier.align(Alignment.TopStart).padding(DDZSpacing.xs).size(markerSize))
                                if (showMarkers) Row(Modifier.align(Alignment.TopEnd).padding(DDZSpacing.xs),
                                    horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs)) {
                                    val tint = if (active) DDZColor.SelectionEmphasis else DDZColor.IconMuted
                                    if (links.fileName) Icon(Icons.Filled.Description, null,
                                        Modifier.size(markerSize), tint = tint)
                                    if (links.path) Icon(Icons.Filled.Folder, null,
                                        Modifier.size(markerSize), tint = tint)
                                }
                            }
                        }
                    }
                }
                }
                if (zoomFactor > 1f) QuickPreviewScrollIndicators(horizontal, vertical, Modifier.matchParentSize())
                }
                Box(Modifier.fillMaxWidth().height(hintHeight), contentAlignment = Alignment.CenterStart) {
                    Text("셀을 눌러 값이나 표시 형식을 바꿀 수 있어요.", style = DDZTypography.Caption,
                        color = DDZColor.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

/** Persistent overflow cues; drawing observes scroll state without intercepting table touches. */
@Composable
private fun QuickPreviewScrollIndicators(horizontal: ScrollState, vertical: ScrollState, modifier: Modifier) {
    val density = LocalDensity.current
    val inset = with(density) { DDZSpacing.itemGap.toPx() }
    val thickness = with(density) { DDZLayout.QuickInput.SelectionBorder.toPx() }
    val minimum = with(density) { DDZLayout.Control.Compact.toPx() }
    Canvas(modifier) {
        val color = DDZColor.SelectionEmphasis
        val track = color.copy(alpha = 0.18f)
        fun line(a: Offset, b: Offset, tint: androidx.compose.ui.graphics.Color = color) =
            drawLine(tint, a, b, thickness, StrokeCap.Round)
        val verticalTrack = (size.height - inset * 4).coerceAtLeast(1f)
        quickScrollThumb(verticalTrack, vertical.maxValue, vertical.value, minimum)?.let { thumb ->
            val x = size.width - inset
            val top = inset * 2
            line(Offset(x, top), Offset(x, top + verticalTrack), track)
            line(Offset(x, top + thumb.first), Offset(x, top + thumb.first + thumb.second))
            if (vertical.value > 0) {
                line(Offset(x - inset / 2, inset), Offset(x, inset / 2))
                line(Offset(x, inset / 2), Offset(x + inset / 2, inset))
            }
            if (vertical.value < vertical.maxValue) {
                line(Offset(x - inset / 2, size.height - inset), Offset(x, size.height - inset / 2))
                line(Offset(x, size.height - inset / 2), Offset(x + inset / 2, size.height - inset))
            }
        }
        val horizontalTrack = (size.width - inset * 4).coerceAtLeast(1f)
        quickScrollThumb(horizontalTrack, horizontal.maxValue, horizontal.value, minimum)?.let { thumb ->
            val y = size.height - inset
            val left = inset * 2
            line(Offset(left, y), Offset(left + horizontalTrack, y), track)
            line(Offset(left + thumb.first, y), Offset(left + thumb.first + thumb.second, y))
            if (horizontal.value > 0) {
                line(Offset(inset, y - inset / 2), Offset(inset / 2, y))
                line(Offset(inset / 2, y), Offset(inset, y + inset / 2))
            }
            if (horizontal.value < horizontal.maxValue) {
                line(Offset(size.width - inset, y - inset / 2), Offset(size.width - inset / 2, y))
                line(Offset(size.width - inset / 2, y), Offset(size.width - inset, y + inset / 2))
            }
        }
    }
}

@Composable
private fun QuickTemplateMenu(expanded: Boolean, guideExpanded: Boolean, onDismiss: () -> Unit,
    content: @Composable () -> Unit) {
    if (guideExpanded) Surface(Modifier.tutorialTarget("camera.quick-template"), color = DDZColor.Surface, shadowElevation = 4.dp) {
        Column { content() }
    } else DropdownMenu(expanded = expanded, onDismissRequest = onDismiss,
        modifier = Modifier.heightIn(max = DDZLayout.QuickInput.MaxListHeight)) { content() }
}
