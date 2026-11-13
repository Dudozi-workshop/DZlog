package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.quickCellLinks
import com.dudoziworkshop.dzlog.feature.table.policy.quickPreviewCellBadgePlan
import com.dudoziworkshop.dzlog.ui.camera.controls.QuickPreviewCellBadges
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderAdapter
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPayload
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.feature.table.render.buildRenderedTableScene
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.RectF
import kotlin.math.roundToInt
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@Composable
internal fun ColumnScope.MockTableCanvas(
    cells: List<TableEditorCellUiModel>,
    rows: Int,
    cols: Int,
    selectedCellId: String?,
    selectedIds: Set<Int>,
    darkTable: Boolean,
    transparentTable: Boolean,
    gridEnabled: Boolean,
    bgAlpha: Int,
    fontScale: Float,
    textAlignIndex: Int,
    textColorMode: Int,
    manualTextColor: Int,
    layoutMode: Boolean,
    preserveInputScale: Boolean,
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    templateCells: List<TableCellState>,
    linkTemplate: TableTemplateState,
    tableWidthRatio: Int,
    tableHeightRatio: Int,
    captureAspect: CaptureAspect,
    onBoundaryDragStart: () -> Unit,
    onRowBoundaryDrag: (Int, Float) -> Unit,
    onColBoundaryDrag: (Int, Float) -> Unit,
    onBoundaryDragEnd: () -> Unit,
    onCellClick: (String) -> Unit,
    onCellRangeDrag: (String, String) -> Unit,
    onClearLayoutSelection: () -> Unit,
) {
    // Pointer handlers live across recompositions; never retain an old draft/callback.
    val latestClear by rememberUpdatedState(onClearLayoutSelection)
    val latestRowDrag by rememberUpdatedState(onRowBoundaryDrag)
    val latestColDrag by rememberUpdatedState(onColBoundaryDrag)
    val latestDragStart by rememberUpdatedState(onBoundaryDragStart)
    val latestDragEnd by rememberUpdatedState(onBoundaryDragEnd)
    var gestureScale by remember(rows, cols) { mutableStateOf<Float?>(null) }
    var activeColumn by remember { mutableStateOf<Int?>(null) }
    var activeRow by remember { mutableStateOf<Int?>(null) }
    val previewScroll = rememberScrollState()
    val background = Color.Transparent
    // A selection highlight is UI-only; it must not change the camera's bold text.
    val renderedValues = remember(cells, rows, cols) { editorRenderCells(cells, rows, cols) }
    val renderedRoots = remember(cells) { editorRenderRootCells(cells) }
    val templateCellById = remember(templateCells) { templateCells.associateBy { it.cellId } }
    // Match the camera's content-driven cell sizing before uniform editor zoom/fit.
    val referenceGeometry = remember(
        renderedValues, templateCells, rows, cols, rowWeights, colWeights,
        fontScale, tableWidthRatio, tableHeightRatio, captureAspect,
    ) {
        computeEditorCameraTableGeometry(
            cells = renderedValues,
            templateCells = templateCells,
            rows = rows,
            cols = cols,
            rowWeights = rowWeights,
            colWeights = colWeights,
            valueScale = (fontScale * 100f).roundToInt(),
            tableWidthRatio = tableWidthRatio,
            tableHeightRatio = tableHeightRatio,
            captureAspect = captureAspect,
        )
    }
    val resolvedRowWeights = referenceGeometry.rowWeights
    val resolvedColWeights = referenceGeometry.colWeights

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(background)
            .pointerInput(layoutMode) { detectTapGestures { if (layoutMode) latestClear() } },
        contentAlignment = Alignment.Center,
    ) {
        // Reserve identical gutters in every tab. Handles are siblings of the grid,
        // inside the frame's hit bounds, rather than negative-offset grid children.
        val gutter = 36.dp
        val expandedWorkingWidth = (maxWidth - gutter * 2).coerceAtLeast(1.dp)
        val naturalTableWidth = expandedWorkingWidth
        val naturalTableHeight = naturalTableWidth / referenceGeometry.widthToHeight
        val viewportFitScale = editorPreviewFitScale(naturalTableWidth.value, naturalTableHeight.value,
            expandedWorkingWidth.value, (maxHeight - gutter * 2).coerceAtLeast(1.dp).value)
        var unobscuredScale by remember(maxWidth, rows, cols) { mutableStateOf<Float?>(null) }
        val fitScale = editorPreviewInputScale(viewportFitScale, unobscuredScale, preserveInputScale)
        val animatedScale by animateFloatAsState(
            targetValue = editorPreviewDragScale(fitScale, gestureScale),
            animationSpec = if (gestureScale != null) snap() else tween(durationMillis = 180),
            label = "editorPreviewFit",
        )
        val displayScale = minOf(animatedScale, fitScale)
        val latestDisplayScale by rememberUpdatedState(displayScale)
        val stableTableHeight = naturalTableHeight * displayScale
        val stableTableWidth = naturalTableWidth * displayScale
        SideEffect {
            if (!preserveInputScale) unobscuredScale = displayScale
        }
        val density = LocalDensity.current
        val viewportHeight = maxHeight
        val frameHeight = stableTableHeight + gutter * 2
        val contentHeight = maxOf(viewportHeight, frameHeight)
        val selected = cells.firstOrNull { it.domainCellId == selectedCellId && !it.isCovered }
        // Only viewport/selection changes move the view. Typing never recentres it.
        LaunchedEffect(preserveInputScale, selectedCellId, viewportHeight, rows, cols) {
            if (!preserveInputScale || selected == null) {
                previewScroll.scrollTo(0)
            } else {
                val sum = resolvedRowWeights.sum().coerceAtLeast(0.0001f)
                val start = selected.rowIndex.coerceIn(0, rows)
                val end = (start + selected.rowSpan).coerceIn(start, rows)
                val top = (contentHeight - frameHeight) / 2 + gutter +
                    stableTableHeight * (resolvedRowWeights.take(start).sum() / sum)
                val bottom = top + stableTableHeight * (resolvedRowWeights.subList(start, end).sum() / sum)
                val target = editorPreviewSelectionScroll(top.value, bottom.value,
                    viewportHeight.value, contentHeight.value)
                withFrameNanos { }
                previewScroll.scrollTo(with(density) { target.dp.roundToPx() })
            }
        }

        Box(
            Modifier.fillMaxSize().clipToBounds().drawWithContent {
                drawContent()
                if (preserveInputScale && previewScroll.maxValue > 0) {
                    val trackHeight = size.height - 8.dp.toPx()
                    if (trackHeight > 0f) {
                        val thumbHeight = (trackHeight * size.height /
                            (size.height + previewScroll.maxValue)).coerceIn(minOf(20.dp.toPx(), trackHeight), trackHeight)
                        val start = 4.dp.toPx() + (trackHeight - thumbHeight) *
                            previewScroll.value / previewScroll.maxValue
                        val x = size.width - 2.dp.toPx()
                        drawLine(DDZColor.BorderStrong, Offset(x, 4.dp.toPx()),
                            Offset(x, size.height - 4.dp.toPx()), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                        drawLine(DDZColor.PrimaryDark, Offset(x, start), Offset(x, start + thumbHeight),
                            strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                    }
                }
            }
                .verticalScroll(previewScroll, enabled = preserveInputScale && !layoutMode),
        ) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = viewportHeight),
                contentAlignment = Alignment.Center,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .requiredSize(stableTableWidth + gutter * 2, frameHeight)
                ) {
                    val density = LocalDensity.current
                    val totalWidth = stableTableWidth
                    val totalHeight = stableTableHeight
                    val markerSizePx = with(density) { DDZLayout.QuickInput.CellMarker.toPx() }
                    val minimumMarkerSizePx = with(density) { DDZLayout.QuickInput.MinCellMarker.toPx() }
                    val markerInsetPx = with(density) { DDZLayout.QuickInput.BadgeInset.toPx() }
                    val colWeightSum = resolvedColWeights.sum().coerceAtLeast(0.0001f)
                    val rowWeightSum = resolvedRowWeights.sum().coerceAtLeast(0.0001f)
                    val colSizes = resolvedColWeights.map { totalWidth * (it / colWeightSum) }
                    val rowSizes = resolvedRowWeights.map { totalHeight * (it / rowWeightSum) }
                    val totalWidthPx = with(density) { totalWidth.toPx().coerceAtLeast(1f) }
                    val totalHeightPx = with(density) { totalHeight.toPx().coerceAtLeast(1f) }
                    val latestWidthPx by rememberUpdatedState(totalWidthPx)
                    val latestHeightPx by rememberUpdatedState(totalHeightPx)
                    val renderScene = remember(
                        totalWidthPx, totalHeightPx, rows, cols,
                        resolvedRowWeights, resolvedColWeights, renderedRoots,
                    ) {
                        buildRenderedTableScene(
                            tableRect = RectF(0f, 0f, totalWidthPx, totalHeightPx),
                            rows = rows,
                            cols = cols,
                            rowWeights = resolvedRowWeights,
                            colWeights = resolvedColWeights,
                            rootCells = renderedRoots,
                        )
                    }
                    val renderStyle = TableRenderStyle(
                        bgStyle = when {
                            transparentTable -> 2
                            darkTable -> 0
                            else -> 1
                        },
                        bgAlpha = bgAlpha,
                        valueScale = (fontScale * 100f).roundToInt(),
                        textColorMode = textColorMode,
                        manualTextColor = manualTextColor,
                        textAlign = textAlignIndex,
                        drawGrid = gridEnabled,
                    )
                    val rootHitRects = cells.filterNot { it.isCovered }.map { cell ->
                        val startRow = cell.rowIndex.coerceIn(0, (rows - 1).coerceAtLeast(0))
                        val startCol = cell.colIndex.coerceIn(0, (cols - 1).coerceAtLeast(0))
                        val endRowExclusive = (startRow + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows)
                        val endColExclusive = (startCol + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols)
                        val x = colSizes.take(startCol).fold(0.dp) { acc, value -> acc + value }
                        val y = rowSizes.take(startRow).fold(0.dp) { acc, value -> acc + value }
                        val width = colSizes.subList(startCol, endColExclusive).fold(0.dp) { acc, value -> acc + value }
                        val height = rowSizes.subList(startRow, endRowExclusive).fold(0.dp) { acc, value -> acc + value }
                        cell to Rect(
                            left = with(density) { x.toPx() },
                            top = with(density) { y.toPx() },
                            right = with(density) { (x + width).toPx() },
                            bottom = with(density) { (y + height).toPx() },
                        )
                    }

                    Box(
                        Modifier.offset(gutter, gutter).size(totalWidth, totalHeight)
                            .border(2.dp, DDZColor.Primary, RoundedCornerShape(4.dp))
                    ) {
                        // Draw the same bold font and proportional text sizing as the camera/photo.
                        Canvas(Modifier.matchParentSize()) {
                            drawIntoCanvas { canvas ->
                                TableRenderAdapter.drawScene(
                                    canvas = canvas.nativeCanvas,
                                    scene = renderScene,
                                    payload = TableRenderPayload(
                                        rows = rows,
                                        cols = cols,
                                        rowWeights = resolvedRowWeights,
                                        colWeights = resolvedColWeights,
                                        cells = renderedValues,
                                    ),
                                    style = renderStyle,
                                )
                            }
                        }
                        cells.filterNot { it.isCovered }.forEach { cell ->
                            val startRow = cell.rowIndex.coerceIn(0, (rows - 1).coerceAtLeast(0))
                            val startCol = cell.colIndex.coerceIn(0, (cols - 1).coerceAtLeast(0))
                            val endRowExclusive = (startRow + cell.rowSpan.coerceAtLeast(1)).coerceAtMost(rows)
                            val endColExclusive = (startCol + cell.colSpan.coerceAtLeast(1)).coerceAtMost(cols)
                            val x = colSizes.take(startCol).fold(0.dp) { acc, value -> acc + value }
                            val y = rowSizes.take(startRow).fold(0.dp) { acc, value -> acc + value }
                            val width = colSizes.subList(startCol, endColExclusive).fold(0.dp) { acc, value -> acc + value }
                            val height = rowSizes.subList(startRow, endRowExclusive).fold(0.dp) { acc, value -> acc + value }
                            val isSelected = if (layoutMode) {
                                cell.id in selectedIds
                            } else {
                                selectedCellId != null && cell.domainCellId == selectedCellId
                            }

                            Box(
                                modifier = Modifier
                                    .offset(x = x, y = y)
                                    .size(width = width, height = height)
                                    .background(
                                        if (isSelected) DDZColor.Primary.copy(alpha = 0.16f) else Color.Transparent
                                    )
                                    .semantics {
                                        contentDescription = cell.previewValue ?: cell.value
                                    }
                                    .clickable(enabled = !layoutMode && cell.domainCellId != null) {
                                        cell.domainCellId?.let(onCellClick)
                                    },
                            ) {
                                // Visual markers are an overlay: they never participate in
                                // cell measurement or move the camera-matched text canvas.
                                if (!layoutMode) {
                                    cell.domainCellId?.let { cellId ->
                                        templateCellById[cellId]?.let { templateCell ->
                                            QuickPreviewCellBadges(
                                                type = templateCell.dataType,
                                                plan = quickPreviewCellBadgePlan(
                                                    widthPx = with(density) { width.toPx() },
                                                    idealSizePx = markerSizePx,
                                                    minimumSizePx = minimumMarkerSizePx,
                                                    insetPx = markerInsetPx,
                                                    links = quickCellLinks(linkTemplate, cellId),
                                                ),
                                                active = isSelected,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (layoutMode) {
                            fun hitDomainCellId(position: Offset): String? =
                                rootHitRects.firstOrNull { (_, rect) -> rect.contains(position) }
                                    ?.first
                                    ?.domainCellId

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(rootHitRects) {
                                        detectTapGestures { position ->
                                            val domainId = hitDomainCellId(position)
                                            if (domainId == null) {
                                                onClearLayoutSelection()
                                            } else {
                                                onCellClick(domainId)
                                            }
                                        }
                                    }
                                    .pointerInput(rootHitRects) {
                                        var startDomainId: String? = null
                                        detectDragGestures(
                                            onDragStart = { position ->
                                                startDomainId = hitDomainCellId(position)
                                            },
                                            onDrag = { change, _ ->
                                                change.consume()
                                                val startId = startDomainId ?: return@detectDragGestures
                                                val endId = hitDomainCellId(change.position) ?: return@detectDragGestures
                                                onCellRangeDrag(startId, endId)
                                            },
                                            onDragEnd = { startDomainId = null },
                                            onDragCancel = { startDomainId = null },
                                        )
                                    }
                            )

                        }
                        activeColumn?.let { boundaryIndex ->
                            val x = colSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                            Box(Modifier.offset(x = x - 1.dp).width(2.dp).fillMaxHeight().background(DDZColor.PrimaryDark))
                        }
                        activeRow?.let { boundaryIndex ->
                            val y = rowSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                            Box(Modifier.offset(y = y - 1.dp).height(2.dp).fillMaxWidth().background(DDZColor.PrimaryDark))
                        }
                    }

                    if (layoutMode) {
                        for (boundaryIndex in 0 until cols.coerceAtLeast(0)) {
                            val x = colSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                            Box(
                                modifier = Modifier
                                    .offset(x = if (boundaryIndex == cols - 1) gutter + totalWidth + 2.dp else gutter + x - 16.dp,
                                        y = if (boundaryIndex == cols - 1) gutter + totalHeight / 2 - 16.dp else 2.dp)
                                    .size(32.dp)
                                    .background(
                                        if (activeColumn == boundaryIndex) DDZColor.SelectedSoft else DDZColor.Surface,
                                        CircleShape,
                                    )
                                    .border(1.dp, DDZColor.BorderStrong, CircleShape)
                                    .pointerInput(boundaryIndex) {
                                        detectDragGestures(
                                            onDragStart = { gestureScale = latestDisplayScale; activeColumn = boundaryIndex; latestDragStart() },
                                            onDragEnd = { activeColumn = null; gestureScale = null; latestDragEnd() },
                                            onDragCancel = { activeColumn = null; gestureScale = null; latestDragEnd() },
                                        ) { change, dragAmount ->
                                            change.consume()
                                            latestColDrag(boundaryIndex, dragAmount.x / latestWidthPx)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                BoundaryDirectionArrow(
                                    horizontal = true,
                                    contentDescription = if (boundaryIndex == cols - 1) "마지막 열 너비 조절" else "${boundaryIndex + 1}열과 ${boundaryIndex + 2}열 너비 조절",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (activeColumn == boundaryIndex) DDZColor.PrimaryDark else DDZColor.Primary,
                                )
                            }
                        }

                        for (boundaryIndex in 0 until rows.coerceAtLeast(0)) {
                            val y = rowSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                            Box(
                                modifier = Modifier
                                    .offset(x = if (boundaryIndex == rows - 1) gutter + totalWidth / 2 - 16.dp else 2.dp,
                                        y = if (boundaryIndex == rows - 1) gutter + totalHeight + 2.dp else gutter + y - 16.dp)
                                    .size(32.dp)
                                    .background(
                                        if (activeRow == boundaryIndex) DDZColor.SelectedSoft else DDZColor.Surface,
                                        CircleShape,
                                    )
                                    .border(1.dp, DDZColor.BorderStrong, CircleShape)
                                    .pointerInput(boundaryIndex) {
                                        detectDragGestures(
                                            onDragStart = { gestureScale = latestDisplayScale; activeRow = boundaryIndex; latestDragStart() },
                                            onDragEnd = { activeRow = null; gestureScale = null; latestDragEnd() },
                                            onDragCancel = { activeRow = null; gestureScale = null; latestDragEnd() },
                                        ) { change, dragAmount ->
                                            change.consume()
                                            latestRowDrag(boundaryIndex, dragAmount.y / latestHeightPx)
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                BoundaryDirectionArrow(
                                    horizontal = false,
                                    contentDescription = if (boundaryIndex == rows - 1) "마지막 행 높이 조절" else "${boundaryIndex + 1}행과 ${boundaryIndex + 2}행 높이 조절",
                                    modifier = Modifier.size(18.dp),
                                    tint = if (activeRow == boundaryIndex) DDZColor.PrimaryDark else DDZColor.Primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


/** One shaft with an arrowhead at each end; not the two-arrow swap icon. */
@Composable
private fun BoundaryDirectionArrow(
    horizontal: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier,
    tint: Color,
) {
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        rotate(if (horizontal) 0f else 90f) {
            val left = size.width * 0.18f
            val right = size.width * 0.82f
            val middle = size.height * 0.5f
            val head = size.width * 0.20f
            val stroke = 1.5.dp.toPx()
            fun segment(from: Offset, to: Offset) =
                drawLine(tint, from, to, strokeWidth = stroke, cap = StrokeCap.Round)
            segment(Offset(left, middle), Offset(right, middle))
            segment(Offset(left, middle), Offset(left + head, middle - head))
            segment(Offset(left, middle), Offset(left + head, middle + head))
            segment(Offset(right, middle), Offset(right - head, middle - head))
            segment(Offset(right, middle), Offset(right - head, middle + head))
        }
    }
}
