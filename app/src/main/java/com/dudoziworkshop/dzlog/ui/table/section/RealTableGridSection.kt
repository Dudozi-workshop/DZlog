package com.dudoziworkshop.dzlog.ui.table.section

import android.graphics.RectF
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.model.derivePathSlotIndexByCellId
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.ui.table.CellHeaderBadgesOverlay
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderAdapter
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPayload
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPlacement
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Layout 탭 전용 실제 표 편집 뷰.
 *
 * 정책 메모:
 * - 임시 grid 높이 분배가 아니라, 워터마크 실표 렌더 코어(drawWatermarkTableOnCanvas) 기반으로 표를 그린다.
 * - 현재 표 영역 박스는 유지하고, 박스 안에서 실제 표 비율을 contain-fit으로 최대 표시한다.
 * - 편집 UX는 "셀 탭/더블탭 -> CELL_EDIT 패널 편집"으로 단일화하고, 표 내부 인라인 입력창은 사용하지 않는다.
 */
@Composable
fun RealTableGridSection(
    templateState: TableTemplateState,
    displayTextProvider: (String) -> String,
    selectedCellId: String?,
    editingCellId: String?,
    wmWidthRatio: Int,
    wmHeightRatio: Int,
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
    onStartRowWeightsDrag: () -> Unit,
    onStartColumnWeightsDrag: () -> Unit,
    onFinishRowWeightsDrag: () -> Unit,
    onFinishColumnWeightsDrag: () -> Unit,
    onCommitRowWeightsDragEnd: (List<Float>) -> Unit,
    onCommitColumnWeightsDragEnd: (List<Float>) -> Unit,
    onSelectRange: (String, String) -> Unit,
) {
    val resolvedCells = remember(templateState.cells, displayTextProvider) {
        // 정책 보정:
        // - WatermarkCell은 좌표(row/col)를 받지 않고 valueText만 가지므로,
        //   drawWatermarkTableOnCanvas가 "입력 리스트 순서"를 좌표 매핑 기준으로 사용할 수 있다.
        // - 따라서 templateState.cells의 원본 순서에 의존하지 않고,
        //   rowIndex -> colIndex(row-major)로 명시 정렬해 안정적으로 전달한다.
        templateState.cells
            .sortedWith(compareBy<TableCellState>({ it.rowIndex }, { it.colIndex }))
            .map { cell ->
                WatermarkBuilder.WatermarkCell(
                    valueText = displayTextProvider(cell.cellId)
                )
            }
    }

    // 정책 보강: 표 배경 테마(검정/흰색/투명)에 맞춰 overlay 색을 동적으로 분기한다.
    // - BG_STYLE_BLACK(0): dark table
    // - BG_STYLE_WHITE(1), BG_STYLE_TRANSPARENT(2): light table 취급
    val isDarkTableTheme = wmBgStyle == 0
    val placeholderColor = if (isDarkTableTheme) {
        Color.White.copy(alpha = 0.72f)
    } else {
        Color.Black.copy(alpha = 0.56f)
    }
    val selectedFillColor = if (isDarkTableTheme) {
        DDZColor.Primary.copy(alpha = 0.09f)
    } else {
        DDZColor.Primary.copy(alpha = 0.06f)
    }
    val selectedBorderColor = if (isDarkTableTheme) Color.White.copy(alpha = 0.90f) else DDZColor.Primary

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val areaWidthPx = with(density) { maxWidth.toPx() }
        val areaHeightPx = with(density) { maxHeight.toPx() }
        val safeWidthRatio = wmWidthRatio.coerceIn(10, 100)
        val safeHeightRatio = wmHeightRatio.coerceIn(10, 100)
        val tableAspect = safeWidthRatio / safeHeightRatio.toFloat()

        val rows = templateState.rows.coerceAtLeast(1)
        val cols = templateState.cols.coerceAtLeast(1)
        val cellCount = rows * cols

        var previewRowWeights by remember(templateState.rowWeights, rows) {
            mutableStateOf(resolveWeightsOrOnes(templateState.rowWeights, rows))
        }
        var previewColWeights by remember(templateState.colWeights, cols) {
            mutableStateOf(resolveWeightsOrOnes(templateState.colWeights, cols))
        }
        var activeRowBoundary by remember { mutableStateOf<Int?>(null) }
        var activeColBoundary by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(templateState.rowWeights, rows) {
            previewRowWeights = resolveWeightsOrOnes(templateState.rowWeights, rows)
        }
        LaunchedEffect(templateState.colWeights, cols) {
            previewColWeights = resolveWeightsOrOnes(templateState.colWeights, cols)
        }

        // 1) contain-fit 기준 크기 계산
        val fitTableWidthPx: Float
        val fitTableHeightPx: Float
        if (areaWidthPx <= 0f || areaHeightPx <= 0f) {
            fitTableWidthPx = 0f
            fitTableHeightPx = 0f
        } else if (areaWidthPx / areaHeightPx > tableAspect) {
            fitTableHeightPx = areaHeightPx
            fitTableWidthPx = fitTableHeightPx * tableAspect
        } else {
            fitTableWidthPx = areaWidthPx
            fitTableHeightPx = fitTableWidthPx / tableAspect
        }

        // 2) 셀 수 기반 adaptive scale cap 적용 (작은 표 과확대 방지)
        val adaptiveScale = resolveAdaptiveTableScale(cellCount)

        // 3) 최종 표 크기(전체 스케일) 계산 + 4) 최종 기준 center 정렬
        val tableWidthPx = fitTableWidthPx * adaptiveScale
        val tableHeightPx = fitTableHeightPx * adaptiveScale
        val tableLeftPx = ((areaWidthPx - tableWidthPx) / 2f).coerceAtLeast(0f)
        val tableTopPx = ((areaHeightPx - tableHeightPx) / 2f).coerceAtLeast(0f)
        val rowSizes = remember(previewRowWeights, rows, tableHeightPx) {
            computeSizes(total = tableHeightPx, weights = resolveWeightsOrOnes(previewRowWeights, rows))
        }
        val colSizes = remember(previewColWeights, cols, tableWidthPx) {
            computeSizes(total = tableWidthPx, weights = resolveWeightsOrOnes(previewColWeights, cols))
        }
        val rowOffsets = remember(rowSizes) { cumulativeOffsets(rowSizes) }
        val colOffsets = remember(colSizes) { cumulativeOffsets(colSizes) }

        // 실제 프리뷰와 동일한 표 렌더 코어를 Layout 편집영역에도 재사용.
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            if (tableWidthPx <= 0f || tableHeightPx <= 0f) return@Canvas

            // drawWatermarkTableOnCanvas는 bounds.width를 base로 table 크기를 계산한다.
            // 따라서 "현재 영역에 fit된 목표 표 rect"가 정확히 나오도록 base bounds를 역산해 맞춘다.
            val baseWidth = tableWidthPx * 100f / safeWidthRatio
            val bounds = RectF(
                tableLeftPx,
                tableTopPx,
                tableLeftPx + baseWidth,
                tableTopPx + tableHeightPx
            )

            drawIntoCanvas { canvas ->
                TableRenderAdapter.draw(
                    canvas = canvas.nativeCanvas,
                    bounds = bounds,
                    payload = TableRenderPayload(
                        rows = rows,
                        cols = cols,
                        rowWeights = previewRowWeights,
                        colWeights = previewColWeights,
                        cells = resolvedCells,
                    ),
                    style = TableRenderStyle(
                        bgAlpha = wmBgAlpha.coerceIn(0, 255),
                        bgStyle = wmBgStyle,
                        valueScale = wmValueScale.coerceIn(60, 160),
                        textColorMode = wmTextColorMode,
                        manualTextColor = wmManualTextColor,
                        textAlign = wmTextAlign,
                        drawGrid = wmGridEnabled,
                    ),
                    placement = TableRenderPlacement(
                        anchor = WatermarkTableAnchor.TOP_LEFT,
                        offsetXRatio = 0,
                        offsetYRatio = 0,
                        tableHeightRatio = safeHeightRatio,
                        tableWidthRatio = safeWidthRatio,
                    ),
                )
            }

            if (isStructureMode) {
                activeRowBoundary?.let { boundary ->
                    if (boundary in 1 until rows) {
                        val topRowIndex = boundary - 1
                        val bottomRowIndex = boundary
                        val topY = tableTopPx + rowOffsets[topRowIndex]
                        val topH = rowSizes[topRowIndex]
                        val bottomY = tableTopPx + rowOffsets[bottomRowIndex]
                        val bottomH = rowSizes[bottomRowIndex]
                        drawRect(
                            color = DDZColor.Primary.copy(alpha = 0.10f),
                            topLeft = Offset(tableLeftPx, topY),
                            size = androidx.compose.ui.geometry.Size(tableWidthPx, topH)
                        )
                        drawRect(
                            color = DDZColor.Primary.copy(alpha = 0.10f),
                            topLeft = Offset(tableLeftPx, bottomY),
                            size = androidx.compose.ui.geometry.Size(tableWidthPx, bottomH)
                        )
                        val lineY = tableTopPx + rowOffsets[boundary]
                        drawLine(
                            color = DDZColor.Primary,
                            start = Offset(tableLeftPx, lineY),
                            end = Offset(tableLeftPx + tableWidthPx, lineY),
                            strokeWidth = 4f
                        )
                    }
                }
                activeColBoundary?.let { boundary ->
                    if (boundary in 1 until cols) {
                        val leftColIndex = boundary - 1
                        val rightColIndex = boundary
                        val leftX = tableLeftPx + colOffsets[leftColIndex]
                        val leftW = colSizes[leftColIndex]
                        val rightX = tableLeftPx + colOffsets[rightColIndex]
                        val rightW = colSizes[rightColIndex]
                        drawRect(
                            color = DDZColor.Primary.copy(alpha = 0.10f),
                            topLeft = Offset(leftX, tableTopPx),
                            size = androidx.compose.ui.geometry.Size(leftW, tableHeightPx)
                        )
                        drawRect(
                            color = DDZColor.Primary.copy(alpha = 0.10f),
                            topLeft = Offset(rightX, tableTopPx),
                            size = androidx.compose.ui.geometry.Size(rightW, tableHeightPx)
                        )
                        val lineX = tableLeftPx + colOffsets[boundary]
                        drawLine(
                            color = DDZColor.Primary,
                            start = Offset(lineX, tableTopPx),
                            end = Offset(lineX, tableTopPx + tableHeightPx),
                            strokeWidth = 4f
                        )
                    }
                }
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
                val l = tableLeftPx + colOffsets[selected.colIndex]
                val t = tableTopPx + rowOffsets[selected.rowIndex]
                val w = colSizes[selected.colIndex]
                val h = rowSizes[selected.rowIndex]
                drawRect(
                    color = if (isStructureMode) selectedFillColor.copy(alpha = 0.14f) else selectedFillColor,
                    topLeft = Offset(l, t),
                    size = androidx.compose.ui.geometry.Size(w, h)
                )
                val selectionStrokeWidth = if (selected.cellId == editingCellId) 4f else 3f
                drawRect(
                    color = selectedBorderColor,
                    topLeft = Offset(l, t),
                    size = androidx.compose.ui.geometry.Size(w, h),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = selectionStrokeWidth)
                )
            }
        }


        if (isStructureMode) {
            // 구조 모드: 표 바깥 핸들(삼각형) + 넓은 hit target으로 비율 조절 문맥을 분리한다.
            val handleGapPx = with(density) { 8.dp.toPx() }
            val handleHitPx = with(density) { 26.dp.toPx() }
            val rowTriangleSize = 12.dp
            val colTriangleSize = 12.dp
            val minSegmentPx = with(density) { 24.dp.toPx() }

            for (boundary in 1 until rows) {
                val yPx = tableTopPx + rowOffsets[boundary]
                val rowHandleActive = activeRowBoundary == boundary
                val rowTriangleDp = if (rowHandleActive) 15.dp else rowTriangleSize
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (tableLeftPx + tableWidthPx + handleGapPx).toDp() },
                            y = with(density) { (yPx - handleHitPx / 2f).toDp() }
                        )
                        .size(with(density) { (if (rowHandleActive) handleHitPx + 6.dp.toPx() else handleHitPx).toDp() })
                        .background(
                            if (rowHandleActive) DDZColor.Primary.copy(alpha = 0.18f) else Color.Transparent,
                            RoundedCornerShape(999.dp)
                        )
                        .pointerInput(boundary, rows) {
                            var startWeights: List<Float> = emptyList()
                            var startSizes: List<Float> = emptyList()
                            var accumulatedDelta = 0f
                            detectDragGestures(
                                onDragStart = {
                                    onStartRowWeightsDrag()
                                    activeRowBoundary = boundary
                                    startWeights = previewRowWeights
                                    startSizes = computeSizes(
                                        total = tableHeightPx,
                                        weights = resolveWeightsOrOnes(startWeights, rows),
                                    )
                                    accumulatedDelta = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDelta += dragAmount.y
                                    previewRowWeights = applyBoundaryDragPreviewFromStart(
                                        startWeights = startWeights,
                                        boundaryIndex = boundary,
                                        accumulatedDeltaPx = accumulatedDelta,
                                        startSegmentSizesPx = startSizes,
                                        minSegmentPx = minSegmentPx,
                                    )
                                },
                                onDragCancel = {
                                    if (startWeights.isNotEmpty()) previewRowWeights = startWeights
                                    activeRowBoundary = null
                                    onFinishRowWeightsDrag()
                                },
                                onDragEnd = {
                                    if (startWeights.isNotEmpty()) {
                                        if (weightsAlmostEqual(startWeights, previewRowWeights)) {
                                            previewRowWeights = startWeights
                                        } else {
                                            onCommitRowWeightsDragEnd(previewRowWeights)
                                        }
                                    }
                                    activeRowBoundary = null
                                    onFinishRowWeightsDrag()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(rowTriangleDp)
                            .height(rowTriangleDp)
                    ) {
                        val path = Path().apply {
                            moveTo(0f, size.height / 2f)
                            lineTo(size.width, 0f)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path = path, color = if (rowHandleActive) DDZColor.Primary else DDZColor.Primary.copy(alpha = 0.86f))
                    }
                }
            }

            for (boundary in 1 until cols) {
                val xPx = tableLeftPx + colOffsets[boundary]
                val colHandleActive = activeColBoundary == boundary
                val colTriangleDp = if (colHandleActive) 15.dp else colTriangleSize
                Box(
                    modifier = Modifier
                        .offset(
                            x = with(density) { (xPx - handleHitPx / 2f).toDp() },
                            y = with(density) { (tableTopPx + tableHeightPx + handleGapPx).toDp() }
                        )
                        .size(with(density) { (if (colHandleActive) handleHitPx + 6.dp.toPx() else handleHitPx).toDp() })
                        .background(
                            if (colHandleActive) DDZColor.Primary.copy(alpha = 0.18f) else Color.Transparent,
                            RoundedCornerShape(999.dp)
                        )
                        .pointerInput(boundary, cols) {
                            var startWeights: List<Float> = emptyList()
                            var startSizes: List<Float> = emptyList()
                            var accumulatedDelta = 0f
                            detectDragGestures(
                                onDragStart = {
                                    onStartColumnWeightsDrag()
                                    activeColBoundary = boundary
                                    startWeights = previewColWeights
                                    startSizes = computeSizes(
                                        total = tableWidthPx,
                                        weights = resolveWeightsOrOnes(startWeights, cols),
                                    )
                                    accumulatedDelta = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    accumulatedDelta += dragAmount.x
                                    previewColWeights = applyBoundaryDragPreviewFromStart(
                                        startWeights = startWeights,
                                        boundaryIndex = boundary,
                                        accumulatedDeltaPx = accumulatedDelta,
                                        startSegmentSizesPx = startSizes,
                                        minSegmentPx = minSegmentPx,
                                    )
                                },
                                onDragCancel = {
                                    if (startWeights.isNotEmpty()) previewColWeights = startWeights
                                    activeColBoundary = null
                                    onFinishColumnWeightsDrag()
                                },
                                onDragEnd = {
                                    if (startWeights.isNotEmpty()) {
                                        if (weightsAlmostEqual(startWeights, previewColWeights)) {
                                            previewColWeights = startWeights
                                        } else {
                                            onCommitColumnWeightsDragEnd(previewColWeights)
                                        }
                                    }
                                    activeColBoundary = null
                                    onFinishColumnWeightsDrag()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(
                        modifier = Modifier
                            .width(colTriangleDp)
                            .height(colTriangleDp)
                    ) {
                        val path = Path().apply {
                            moveTo(size.width / 2f, 0f)
                            lineTo(0f, size.height)
                            lineTo(size.width, size.height)
                            close()
                        }
                        drawPath(path = path, color = if (colHandleActive) DDZColor.Primary else DDZColor.Primary.copy(alpha = 0.86f))
                    }
                }
            }
        }

        templateState.cells.forEach { cell ->
            if (cell.rowIndex !in 0 until rows || cell.colIndex !in 0 until cols) return@forEach

            val cellX = tableLeftPx + colOffsets[cell.colIndex]
            val cellY = tableTopPx + rowOffsets[cell.rowIndex]
            val cellW = colSizes[cell.colIndex]
            val cellH = rowSizes[cell.rowIndex]
            val isEditingCell = cell.cellId == editingCellId
            val display = displayTextProvider(cell.cellId)
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (display.isBlank()) {
                        // 정책 변경:
                        // - 일반 상태의 값 텍스트는 Canvas(실표 렌더 코어)만 사용하고 Overlay 텍스트는 제거한다.
                        // - Overlay는 "값 없음 placeholder"일 때만 노출해 중복 렌더/잔상처럼 보이는 문제를 방지한다.
                        Text(
                            text = dataTypeLabelKo(cell.dataType),
                            style = DDZTypography.Caption,
                            // 테이블 테마 기반 placeholder muted 색상 분기
                            color = placeholderColor
                        )
                    }
                }

                CellHeaderBadgesOverlay(
                    cell = cell,
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


private fun resolveAdaptiveTableScale(cellCount: Int): Float =
    when {
        cellCount <= 4 -> 0.72f
        cellCount <= 6 -> 0.82f
        cellCount <= 8 -> 0.90f
        else -> 1.00f
    }

private fun resolveWeightsOrOnes(weights: List<Float>?, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    return TableLayoutCalculator.resolveWeights(weights, count)
}

private fun computeSizes(total: Float, weights: List<Float>): List<Float> {
    return TableLayoutCalculator.computeSizes(total, weights)
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


private fun applyBoundaryDragPreviewFromStart(
    startWeights: List<Float>,
    boundaryIndex: Int,
    accumulatedDeltaPx: Float,
    startSegmentSizesPx: List<Float>,
    minSegmentPx: Float,
): List<Float> {
    if (boundaryIndex <= 0 || boundaryIndex >= startWeights.size) return startWeights
    val leftIndex = boundaryIndex - 1
    val rightIndex = boundaryIndex
    val leftSize = startSegmentSizesPx.getOrNull(leftIndex) ?: return startWeights
    val rightSize = startSegmentSizesPx.getOrNull(rightIndex) ?: return startWeights

    val boundedDelta = accumulatedDeltaPx
        .coerceAtMost(rightSize - minSegmentPx)
        .coerceAtLeast(-(leftSize - minSegmentPx))

    val pairSize = leftSize + rightSize
    if (pairSize <= 0f) return startWeights

    val nextLeftSize = (leftSize + boundedDelta).coerceAtLeast(minSegmentPx)
    val pairWeight = (startWeights[leftIndex] + startWeights[rightIndex]).coerceAtLeast(0.0001f)
    val nextLeftWeight = pairWeight * (nextLeftSize / pairSize)
    val nextRightWeight = (pairWeight - nextLeftWeight).coerceAtLeast(0.0001f)

    return startWeights.toMutableList().apply {
        this[leftIndex] = nextLeftWeight
        this[rightIndex] = nextRightWeight
    }
}

private fun weightsAlmostEqual(a: List<Float>, b: List<Float>): Boolean {
    if (a.size != b.size) return false
    return a.indices.all { index -> kotlin.math.abs(a[index] - b[index]) < 0.0001f }
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
        localX < 0f || localX > maxX -> return null
        else -> localX
    }
    val adjustedY = when {
        clampToBounds -> localY.coerceIn(0f, maxY)
        localY < 0f || localY > maxY -> return null
        else -> localY
    }

    val colIndex = findIndexByOffsets(adjustedX, colOffsets) ?: return null
    val rowIndex = findIndexByOffsets(adjustedY, rowOffsets) ?: return null
    return cells.firstOrNull { it.rowIndex == rowIndex && it.colIndex == colIndex }?.cellId
}

private fun findIndexByOffsets(value: Float, offsets: List<Float>): Int? {
    if (offsets.size < 2) return null
    val lastBoundary = offsets.last()
    if (value < 0f || value > lastBoundary) return null
    if (value == lastBoundary) return offsets.lastIndex - 1
    return (0 until offsets.lastIndex).firstOrNull { idx ->
        value >= offsets[idx] && value < offsets[idx + 1]
    }
}
