package com.dudoziworkshop.dzlog.ui.table.section

import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
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
    onSelectCell: (String) -> Unit,
    onDoubleClickCell: (TableCellState) -> Unit
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
        val rowSizes = remember(templateState.rowWeights, rows, tableHeightPx) {
            computeSizes(total = tableHeightPx, weights = resolveWeightsOrOnes(templateState.rowWeights, rows))
        }
        val colSizes = remember(templateState.colWeights, cols, tableWidthPx) {
            computeSizes(total = tableWidthPx, weights = resolveWeightsOrOnes(templateState.colWeights, cols))
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
                        rowWeights = templateState.rowWeights,
                        colWeights = templateState.colWeights,
                        cells = resolvedCells,
                    ),
                    style = TableRenderStyle(
                        bgAlpha = 210,
                        bgStyle = wmBgStyle,
                        valueScale = 100,
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

            // 편집 화면 선택 강조:
            // - 테이블 테마(밝음/어두움)에 맞춰 fill/border 색을 분기해 가독성을 유지한다.
            selectedCellId?.let { selectedId ->
                val selected = templateState.cells.firstOrNull { it.cellId == selectedId }
                if (selected != null) {
                    val l = tableLeftPx + colOffsets[selected.colIndex]
                    val t = tableTopPx + rowOffsets[selected.rowIndex]
                    val w = colSizes[selected.colIndex]
                    val h = rowSizes[selected.rowIndex]
                    drawRect(
                        color = selectedFillColor,
                        topLeft = Offset(l, t),
                        size = androidx.compose.ui.geometry.Size(w, h)
                    )
                    // 선택 border는 3f, 편집 중인 선택 셀은 4f로 강화해 인지성을 높인다.
                    val selectionStrokeWidth = if (selected.cellId == editingCellId) 4f else 3f
                    drawRect(
                        color = selectedBorderColor,
                        topLeft = Offset(l, t),
                        size = androidx.compose.ui.geometry.Size(w, h),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = selectionStrokeWidth)
                    )
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
                        // 정책 보강: 선택/편집 경계는 Canvas 하이라이트(3f/4f)로 일원화해
                        // overlay border와 이중으로 겹쳐 보이는 문제를 방지한다.
                        Modifier
                    )
                    .combinedClickable(
                        onClick = { onSelectCell(cell.cellId) },
                        onDoubleClick = { onDoubleClickCell(cell) }
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
