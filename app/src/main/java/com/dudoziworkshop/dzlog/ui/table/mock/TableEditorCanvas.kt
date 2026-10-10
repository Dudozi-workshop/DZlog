package com.dudoziworkshop.dzlog.ui.table.mock

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
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
    rowWeights: List<Float>?,
    colWeights: List<Float>?,
    onBoundaryDragStart: () -> Unit,
    onRowBoundaryDrag: (Int, Float) -> Unit,
    onColBoundaryDrag: (Int, Float) -> Unit,
    onBoundaryDragEnd: () -> Unit,
    onCellClick: (String) -> Unit,
    onCellRangeDrag: (String, String) -> Unit,
    onClearLayoutSelection: () -> Unit,
) {
    // Pointer handlers live across recompositions; never retain an old draft/callback.
    val latestRowDrag by rememberUpdatedState(onRowBoundaryDrag)
    val latestColDrag by rememberUpdatedState(onColBoundaryDrag)
    val latestDragStart by rememberUpdatedState(onBoundaryDragStart)
    val latestDragEnd by rememberUpdatedState(onBoundaryDragEnd)
    var activeColumn by remember { mutableStateOf<Int?>(null) }
    var activeRow by remember { mutableStateOf<Int?>(null) }
    val background = Color.Transparent
    val resolvedAlpha = (bgAlpha.coerceIn(0, 255) / 255f)
    val cellBackground = when {
        transparentTable -> Color.Transparent
        darkTable -> Color(0xFF202522).copy(alpha = resolvedAlpha)
        else -> Color.White.copy(alpha = resolvedAlpha)
    }
    val textColor = when {
        textColorMode == 1 && manualTextColor == 0 -> Color.White
        textColorMode == 1 && manualTextColor == 1 -> Color.Black
        darkTable -> Color.White
        else -> Color(0xFF202124)
    }
    val cellAlignment = when (textAlignIndex.coerceIn(0, 2)) {
        0 -> Alignment.CenterStart
        2 -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    val resolvedRowWeights = TableLayoutCalculator.resolveWeights(rowWeights, rows)
    val resolvedColWeights = TableLayoutCalculator.resolveWeights(colWeights, cols)
    val baseTableHeight = (74.dp * rows.toFloat()).coerceIn(120.dp, 420.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        // Reserve identical gutters in every tab. Handles are siblings of the grid,
        // inside the frame's hit bounds, rather than negative-offset grid children.
        val gutter = 36.dp
        val previousWorkingWidth = (maxWidth - 52.dp).coerceAtLeast(1.dp)
        val expandedWorkingWidth = (maxWidth - gutter * 2).coerceAtLeast(1.dp)
        val editorScale = (expandedWorkingWidth / previousWorkingWidth).coerceAtLeast(0.01f)
        val naturalTableHeight = baseTableHeight * editorScale
        val fitScale = ((maxHeight - gutter * 2).coerceAtLeast(1.dp) / naturalTableHeight)
            .coerceIn(0f, 1f)
        val stableTableHeight = naturalTableHeight * fitScale
        val stableTableWidth = expandedWorkingWidth * fitScale

        BoxWithConstraints(
            modifier = Modifier
                .width(stableTableWidth + gutter * 2)
                .height(stableTableHeight + gutter * 2)
        ) {
            val density = LocalDensity.current
            val totalWidth = stableTableWidth
            val totalHeight = stableTableHeight
            val colWeightSum = resolvedColWeights.sum().coerceAtLeast(0.0001f)
            val rowWeightSum = resolvedRowWeights.sum().coerceAtLeast(0.0001f)
            val colSizes = resolvedColWeights.map { totalWidth * (it / colWeightSum) }
            val rowSizes = resolvedRowWeights.map { totalHeight * (it / rowWeightSum) }
            val totalWidthPx = with(density) { totalWidth.toPx().coerceAtLeast(1f) }
            val totalHeightPx = with(density) { totalHeight.toPx().coerceAtLeast(1f) }
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
                                if (isSelected) DDZColor.Primary.copy(alpha = 0.16f) else cellBackground
                            )
                            .then(
                                if (gridEnabled) Modifier.border(
                                    0.5.dp,
                                    if (darkTable) Color.White.copy(alpha = 0.28f) else Color(0xFFB8B8BE)
                                ) else Modifier
                            )
                            .clickable(enabled = !layoutMode && cell.domainCellId != null) {
                                cell.domainCellId?.let(onCellClick)
                            },
                        contentAlignment = cellAlignment,
                    ) {
                        Text(
                            cell.previewValue ?: cell.value,
                            color = if (isSelected) DDZColor.PrimaryDark else textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = (14f * fontScale.coerceIn(0.6f, 1.6f)).sp,
                            modifier = Modifier.padding(8.dp),
                        )
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
                for (boundaryIndex in 0 until (cols - 1).coerceAtLeast(0)) {
                    val x = colSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                    Box(
                        modifier = Modifier
                            .offset(x = gutter + x - 16.dp, y = 2.dp)
                            .size(32.dp)
                            .pointerInput(boundaryIndex, totalWidthPx) {
                                detectDragGestures(
                                    onDragStart = { activeColumn = boundaryIndex; latestDragStart() },
                                    onDragEnd = { activeColumn = null; latestDragEnd() },
                                    onDragCancel = { activeColumn = null; latestDragEnd() },
                                ) { change, dragAmount ->
                                    change.consume()
                                    latestColDrag(boundaryIndex, dragAmount.x / totalWidthPx)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.SwapHoriz,
                            contentDescription = "${boundaryIndex + 1}열과 ${boundaryIndex + 2}열 너비 조절",
                            modifier = Modifier.size(18.dp),
                            tint = if (activeColumn == boundaryIndex) DDZColor.PrimaryDark else DDZColor.Primary,
                        )
                    }
                }

                for (boundaryIndex in 0 until (rows - 1).coerceAtLeast(0)) {
                    val y = rowSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = gutter + y - 16.dp)
                            .size(32.dp)
                            .pointerInput(boundaryIndex, totalHeightPx) {
                                detectDragGestures(
                                    onDragStart = { activeRow = boundaryIndex; latestDragStart() },
                                    onDragEnd = { activeRow = null; latestDragEnd() },
                                    onDragCancel = { activeRow = null; latestDragEnd() },
                                ) { change, dragAmount ->
                                    change.consume()
                                    latestRowDrag(boundaryIndex, dragAmount.y / totalHeightPx)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.SwapVert,
                            contentDescription = "${boundaryIndex + 1}행과 ${boundaryIndex + 2}행 높이 조절",
                            modifier = Modifier.size(18.dp),
                            tint = if (activeRow == boundaryIndex) DDZColor.PrimaryDark else DDZColor.Primary,
                        )
                    }
                }
            }
        }
    }
}

