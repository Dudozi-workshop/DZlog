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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    val background = DDZColor.Background
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
        val previousWorkingWidth = (maxWidth - 52.dp).coerceAtLeast(1.dp)
        val expandedWorkingWidth = (maxWidth - 16.dp).coerceAtLeast(1.dp)
        val editorScale = (expandedWorkingWidth / previousWorkingWidth).coerceAtLeast(1f)
        val stableTableHeight = (baseTableHeight * editorScale)
            .coerceAtMost((maxHeight - 16.dp).coerceAtLeast(120.dp))

        BoxWithConstraints(
            modifier = Modifier
                .width(expandedWorkingWidth)
                .height(stableTableHeight)
                .border(2.dp, DDZColor.Primary, RoundedCornerShape(4.dp))
        ) {
            val density = LocalDensity.current
            val totalWidth = maxWidth
            val totalHeight = maxHeight
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
                        cell.value,
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

                for (boundaryIndex in 0 until (cols - 1).coerceAtLeast(0)) {
                    val x = colSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                    Box(
                        modifier = Modifier
                            .offset(x = x - 10.dp)
                            .width(20.dp)
                            .fillMaxHeight()
                            .pointerInput(boundaryIndex, totalWidthPx) {
                                detectDragGestures(
                                    onDragStart = { onBoundaryDragStart() },
                                    onDragEnd = onBoundaryDragEnd,
                                    onDragCancel = onBoundaryDragEnd,
                                ) { change, dragAmount ->
                                    change.consume()
                                    onColBoundaryDrag(boundaryIndex, dragAmount.x / totalWidthPx)
                                }
                            },
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(18.dp)
                                .background(DDZColor.Primary.copy(alpha = 0.65f))
                        )
                    }
                }

                for (boundaryIndex in 0 until (rows - 1).coerceAtLeast(0)) {
                    val y = rowSizes.take(boundaryIndex + 1).fold(0.dp) { acc, value -> acc + value }
                    Box(
                        modifier = Modifier
                            .offset(y = y - 10.dp)
                            .height(20.dp)
                            .fillMaxWidth()
                            .pointerInput(boundaryIndex, totalHeightPx) {
                                detectDragGestures(
                                    onDragStart = { onBoundaryDragStart() },
                                    onDragEnd = onBoundaryDragEnd,
                                    onDragCancel = onBoundaryDragEnd,
                                ) { change, dragAmount ->
                                    change.consume()
                                    onRowBoundaryDrag(boundaryIndex, dragAmount.y / totalHeightPx)
                                }
                            },
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Box(
                            Modifier
                                .height(3.dp)
                                .width(18.dp)
                                .background(DDZColor.Primary.copy(alpha = 0.65f))
                        )
                    }
                }
            }
        }
    }
}
