package com.dudoziworkshop.dzlog.ui.table.section

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.model.derivePathSlotIndexByCellId
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
import com.dudoziworkshop.dzlog.feature.table.render.ContentDrivenLayoutCell
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderAdapter
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPayload
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPlacement
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.feature.table.render.computeContentDrivenLayout
import com.dudoziworkshop.dzlog.feature.table.render.spanSize
import com.dudoziworkshop.dzlog.feature.table.render.toRelativeWeights
import com.dudoziworkshop.dzlog.ui.table.CellHeaderBadgesOverlay
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private val STRUCTURE_PREVIEW_WORKING_INSET_DP = 10.dp

private data class CellRenderEntry(
    val cell: TableCellState,
    val displayText: String,
    val isPlaceholder: Boolean,
)

/**
 * Layout 탭 전용 실제 표 편집 뷰.
 *
 * 정책 메모:
 * - 임시 grid 높이 분배가 아니라, 워터마크 실표 렌더 코어(drawWatermarkTableOnCanvas) 기반으로 표를 그린다.
 * - 현재 표 영역 박스는 유지하고, 박스 안에서 실제 표 비율을 contain-fit으로 최대 표시한다.
 * - 편집 UX는 "셀 탭/더블탭 -> CELL_EDIT 패널 편집"으로 단일화하고, 표 내부 인라인 입력창은 사용하지 않는다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RealTableGridSection(
    templateState: TableTemplateState,
    displayTextProvider: (String) -> String,
    selectedCellId: String?,
    editingCellId: String?,
    wmWidthRatio: Int,
    wmBgStyle: Int,
    wmBgAlpha: Int,
    wmValueScale: Int,
    wmTextColorMode: Int,
    wmManualTextColor: Int,
    wmTextAlign: Int,
    wmGridEnabled: Boolean,
    isStructureMode: Boolean,
    structureSelectedCellIds: Set<String>,
    onSelectCell: (String) -> Unit,
    onDoubleClickCell: (TableCellState) -> Unit,
    onSelectRange: (String, String) -> Unit,
) {
    val orderedCells = remember(templateState.cells) {
        templateState.cells.sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))
    }
    val cellRenderEntries = remember(orderedCells, displayTextProvider) {
        orderedCells.map { cell ->
            val rawValue = displayTextProvider(cell.cellId)
            val isPlaceholder = rawValue.isBlank()
            val displayText = if (isPlaceholder) dataTypeLabelKo(cell.dataType) else rawValue
            CellRenderEntry(cell = cell, displayText = displayText, isPlaceholder = isPlaceholder)
        }
    }
    val rootCells = remember(templateState.cells) { TableStructureRangeActions.rootCells(templateState.cells) }
    val placeholderCellIndexes = remember(cellRenderEntries) {
        buildSet {
            cellRenderEntries.forEachIndexed { index, entry ->
                if (entry.isPlaceholder) add(index)
            }
        }
    }
    val resolvedCells = remember(cellRenderEntries) {
        cellRenderEntries.map { entry ->
            WatermarkBuilder.WatermarkCell(
                valueText = entry.displayText,
            )
        }
    }
    // 정책 보강: 표 배경 테마(검정/흰색/투명)에 맞춰 overlay 색을 동적으로 분기한다.
    // - BG_STYLE_BLACK(0): dark table
    // - BG_STYLE_WHITE(1), BG_STYLE_TRANSPARENT(2): light table 취급
    val isDarkTableTheme = wmBgStyle == 0
    val selectedFillColor = if (isDarkTableTheme) {
        DDZColor.Primary.copy(alpha = 0.09f)
    } else {
        DDZColor.Primary.copy(alpha = 0.06f)
    }
    val selectedBorderColor = if (isDarkTableTheme) Color.White.copy(alpha = 0.90f) else DDZColor.Primary
    val placeholderTextColorArgb = if (isDarkTableTheme) {
        android.graphics.Color.argb(184, 255, 255, 255)
    } else {
        android.graphics.Color.argb(150, 0, 0, 0)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val areaWidthPx = with(density) { this@BoxWithConstraints.maxWidth.toPx() }
        val areaHeightPx = with(density) { this@BoxWithConstraints.maxHeight.toPx() }
        val workingInsetPx = with(density) { STRUCTURE_PREVIEW_WORKING_INSET_DP.toPx() }
        val workingLeftPx = workingInsetPx.coerceAtMost(areaWidthPx / 2f)
        val workingTopPx = workingInsetPx.coerceAtMost(areaHeightPx / 2f)
        val workingWidthPx = (areaWidthPx - (workingLeftPx * 2f)).coerceAtLeast(0f)
        val workingHeightPx = (areaHeightPx - (workingTopPx * 2f)).coerceAtLeast(0f)
        val rows = templateState.rows.coerceAtLeast(1)
        val cols = templateState.cols.coerceAtLeast(1)
        val resolvedStructureCellsById = remember(templateState.cells) {
            TableStructureRangeActions.resolveStructureCells(templateState.cells)
                .associateBy { it.cellId }
        }
        val layoutCells = remember(cellRenderEntries, resolvedStructureCellsById) {
            cellRenderEntries.map { entry ->
                val resolved = resolvedStructureCellsById[entry.cell.cellId]
                ContentDrivenLayoutCell(
                    rowIndex = resolved?.rootRowIndex ?: entry.cell.rowIndex,
                    colIndex = resolved?.rootColIndex ?: entry.cell.colIndex,
                    rowSpan = resolved?.rootRowSpan ?: entry.cell.rowSpan,
                    colSpan = resolved?.rootColSpan ?: entry.cell.colSpan,
                    displayText = entry.displayText,
                    isCovered = resolved?.isCovered == true,
                )
            }
        }
        val layout = remember(layoutCells, wmWidthRatio, wmValueScale, rows, cols) {
            computeContentDrivenLayout(
                cells = layoutCells,
                rows = rows,
                cols = cols,
                baseScaleRatio = wmWidthRatio.coerceIn(10, 100),
                valueScale = wmValueScale,
            )
        }

        val tableWidthPx = (layout.contentWidthPx * layout.scale).coerceAtMost(workingWidthPx)
        val tableHeightPx = (layout.contentHeightPx * layout.scale).coerceAtMost(workingHeightPx)
        val tableLeftPx = (workingLeftPx + (workingWidthPx - tableWidthPx) / 2f).coerceAtLeast(0f)
        val tableTopPx = (workingTopPx + (workingHeightPx - tableHeightPx) / 2f).coerceAtLeast(0f)
        val rowSizes = remember(layout, tableHeightPx) { layout.rowHeightsPx.map { it * layout.scale } }
        val colSizes = remember(layout, tableWidthPx) { layout.colWidthsPx.map { it * layout.scale } }
        val rowOffsets = remember(rowSizes) { cumulativeOffsets(rowSizes) }
        val colOffsets = remember(colSizes) { cumulativeOffsets(colSizes) }
        val rowWeights = remember(rowSizes) { toRelativeWeights(rowSizes) }
        val colWeights = remember(colSizes) { toRelativeWeights(colSizes) }

        // 실제 프리뷰와 동일한 표 렌더 코어를 Layout 편집영역에도 재사용.
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (tableWidthPx <= 0f || tableHeightPx <= 0f) return@Canvas

            // drawWatermarkTableOnCanvas는 bounds.width를 base로 table 크기를 계산한다.
            // 따라서 width 기준 base만 역산하고, 목표 높이는 실제 fit 높이(tableHeightPx)를 직접 사용한다.
            val bounds = RectF(
                tableLeftPx,
                tableTopPx,
                tableLeftPx + tableWidthPx,
                tableTopPx + tableHeightPx
            )

            drawIntoCanvas { canvas ->
                TableRenderAdapter.draw(
                    canvas = canvas.nativeCanvas,
                    bounds = bounds,
                    payload = TableRenderPayload(
                        rows = rows,
                        cols = cols,
                        rowWeights = rowWeights,
                        colWeights = colWeights,
                        cells = resolvedCells,
                        placeholderCellIndexes = placeholderCellIndexes,
                    ),
                    style = TableRenderStyle(
                        bgAlpha = wmBgAlpha.coerceIn(0, 255),
                        bgStyle = wmBgStyle,
                        valueScale = wmValueScale.coerceIn(60, 160),
                        textColorMode = wmTextColorMode,
                        manualTextColor = wmManualTextColor,
                        textAlign = wmTextAlign,
                        drawGrid = wmGridEnabled,
                        placeholderTextColorArgb = placeholderTextColorArgb,
                    ),
                    placement = TableRenderPlacement(
                        anchor = WatermarkTableAnchor.TOP_LEFT,
                        offsetXRatio = 0,
                        offsetYRatio = 0,
                        tableHeightRatio = 100,
                        tableWidthRatio = 100,
                    ),
                )
            }

            // 편집 화면 선택 강조:
            // - 테이블 테마(밝음/어두움)에 맞춰 fill/border 색을 분기해 가독성을 유지한다.
            val effectiveSelectionIds = if (isStructureMode && structureSelectedCellIds.isNotEmpty()) {
                structureSelectedCellIds
            } else {
                selectedCellId?.let { setOf(it) } ?: emptySet()
            }
            effectiveSelectionIds.forEach { selectedId ->
                val selected = templateState.cells.firstOrNull { it.cellId == selectedId } ?: return@forEach
                val root = TableStructureRangeActions.resolveRootCell(templateState.cells, selected)
                val l = tableLeftPx + colOffsets[root.colIndex]
                val t = tableTopPx + rowOffsets[root.rowIndex]
                val spanW = spanSize(root.colIndex, root.colSpan, colSizes)
                val spanH = spanSize(root.rowIndex, root.rowSpan, rowSizes)
                drawRect(
                    color = if (isStructureMode) selectedFillColor.copy(alpha = 0.14f) else selectedFillColor,
                    topLeft = Offset(l, t),
                    size = androidx.compose.ui.geometry.Size(spanW, spanH)
                )
                val selectionStrokeWidth = if (selected.cellId == editingCellId) 4f else 3f
                drawRect(
                    color = selectedBorderColor,
                    topLeft = Offset(l, t),
                    size = androidx.compose.ui.geometry.Size(spanW, spanH),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = selectionStrokeWidth)
                )
            }
        }

        rootCells.forEach { cell ->
            if (cell.rowIndex !in 0 until rows || cell.colIndex !in 0 until cols) return@forEach

            val cellX = tableLeftPx + colOffsets[cell.colIndex]
            val cellY = tableTopPx + rowOffsets[cell.rowIndex]
            val cellW = spanSize(cell.colIndex, cell.colSpan, colSizes)
            val cellH = spanSize(cell.rowIndex, cell.rowSpan, rowSizes)
            val nameIdx = deriveFileNameCellSlotsFromDrafts(templateState.fileNameSlotDrafts).indexOf(cell.cellId).takeIf { it >= 0 }
            val pathIdx = derivePathSlotIndexByCellId(templateState.pathSlotDrafts, cell.cellId)

            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { cellX.toDp() },
                        y = with(density) { cellY.toDp() }
                    )
                    .width(with(density) { cellW.toDp() })
                    .height(with(density) { cellH.toDp() })
                    .then(
                        if (isStructureMode) {
                            Modifier
                        } else {
                            // 정책 보강: 선택/편집 경계는 Canvas 하이라이트(3f/4f)로 일원화해
                            // overlay border와 이중으로 겹쳐 보이는 문제를 방지한다.
                            Modifier
                                .combinedClickable(
                                    onClick = { onSelectCell(cell.cellId) },
                                    onDoubleClick = { onDoubleClickCell(cell) }
                                )
                        }
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                CellHeaderBadgesOverlay(
                    fileNameSlotIndex = nameIdx,
                    pathSlotIndex = pathIdx,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }

        if (isStructureMode) {
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { tableLeftPx.toDp() },
                        y = with(density) { tableTopPx.toDp() }
                    )
                    .width(with(density) { tableWidthPx.toDp() })
                    .height(with(density) { tableHeightPx.toDp() })
                    .zIndex(2f)
                    .pointerInput(rows, cols, rowOffsets, colOffsets) {
                        detectTapGestures { offset ->
                            cellIdFromOffset(
                                cells = templateState.cells,
                                colOffsets = colOffsets,
                                rowOffsets = rowOffsets,
                                localX = offset.x,
                                localY = offset.y,
                                )?.let(onSelectCell)
                        }
                    }
                    .pointerInput(rows, cols, rowOffsets, colOffsets) {
                        var startCellId: String? = null
                        detectDragGestures(
                            onDragStart = { offset ->
                                startCellId = cellIdFromOffset(
                                    cells = templateState.cells,
                                    colOffsets = colOffsets,
                                    rowOffsets = rowOffsets,
                                    localX = offset.x,
                                    localY = offset.y,
                                )
                                startCellId?.let { onSelectRange(it, it) }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val start = startCellId ?: return@detectDragGestures
                                val end = cellIdFromOffset(
                                    cells = templateState.cells,
                                    colOffsets = colOffsets,
                                    rowOffsets = rowOffsets,
                                    localX = change.position.x,
                                    localY = change.position.y,
                                    clampToBounds = true,
                                ) ?: return@detectDragGestures
                                onSelectRange(start, end)
                            },
                        )
                    }
            )
        }
    }
}

private fun cumulativeOffsets(sizes: List<Float>): List<Float> {
    val offsets = MutableList(sizes.size + 1) { 0f }
    for (idx in sizes.indices) {
        offsets[idx + 1] = offsets[idx] + sizes[idx]
    }
    return offsets
}

private fun dataTypeLabelKo(dataType: TableCellDataType): String =
    when (dataType) {
        TableCellDataType.TEXT -> "텍스트"
        TableCellDataType.NUMBER -> "숫자"
        TableCellDataType.COUNTER -> "카운터"
        TableCellDataType.DATE -> "날짜"
        TableCellDataType.TIME -> "시간"
        TableCellDataType.ROTATING_TEXT -> "순환텍스트"
    }

private fun cellIdFromOffset(
    cells: List<TableCellState>,
    colOffsets: List<Float>,
    rowOffsets: List<Float>,
    localX: Float,
    localY: Float,
    clampToBounds: Boolean = false,
): String? {
    if (colOffsets.size < 2 || rowOffsets.size < 2) return null
    val maxX = colOffsets.last()
    val maxY = rowOffsets.last()
    val adjustedX = when {
        clampToBounds -> localX.coerceIn(0f, maxX)
        localX !in 0f..maxX -> return null
        else -> localX
    }
    val adjustedY = when {
        clampToBounds -> localY.coerceIn(0f, maxY)
        localY !in 0f..maxY -> return null
        else -> localY
    }

    val colIndex = findIndexByOffsets(adjustedX, colOffsets) ?: return null
    val rowIndex = findIndexByOffsets(adjustedY, rowOffsets) ?: return null
    val tapped = cells.firstOrNull { it.rowIndex == rowIndex && it.colIndex == colIndex } ?: return null
    return TableStructureRangeActions.resolveRootCell(cells, tapped).cellId
}

private fun findIndexByOffsets(value: Float, offsets: List<Float>): Int? {
    if (offsets.size < 2) return null
    val lastBoundary = offsets.last()
    if (value !in 0f..lastBoundary) return null
    if (value == lastBoundary) return offsets.lastIndex - 1
    return (0 until offsets.lastIndex).firstOrNull { idx ->
        value >= offsets[idx] && value < offsets[idx + 1]
    }
}
