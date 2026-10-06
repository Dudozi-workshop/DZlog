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
import com.dudoziworkshop.dzlog.domain.model.deriveFileNameCellSlotsFromDrafts
import com.dudoziworkshop.dzlog.domain.model.derivePathSlotIndexByCellId
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
import com.dudoziworkshop.dzlog.feature.table.render.RenderRootRect
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderAdapter
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderPayload
import com.dudoziworkshop.dzlog.feature.table.render.TableRenderStyle
import com.dudoziworkshop.dzlog.feature.table.render.buildContentDrivenRenderedSceneFromPlacement
import com.dudoziworkshop.dzlog.feature.table.render.buildDesignPreviewPlacement
import com.dudoziworkshop.dzlog.feature.table.render.computeDesignPreviewFitShape
import com.dudoziworkshop.dzlog.ui.table.CellHeaderBadgesOverlay
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

private val STRUCTURE_PREVIEW_WORKING_INSET_DP = 10.dp
private const val DEBUG_ROOT_HIT_RECTS = false

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
    val rootCellById = remember(rootCells) { rootCells.associateBy { it.cellId } }
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
        val renderBounds = remember(workingLeftPx, workingTopPx, workingWidthPx, workingHeightPx) {
            RectF(
                workingLeftPx,
                workingTopPx,
                workingLeftPx + workingWidthPx,
                workingTopPx + workingHeightPx,
            )
        }
        val previewRatio = remember(renderBounds, rows, cols, wmWidthRatio) {
            computeDesignPreviewFitShape(
                boundsWidth = renderBounds.width(),
                boundsHeight = renderBounds.height(),
                tableWidthRatio = wmWidthRatio,
                tableHeightRatio = wmWidthRatio,
                rows = rows,
                cols = cols,
            )
        }
        val placement = remember(previewRatio) {
            buildDesignPreviewPlacement(
                tableWidthRatio = previewRatio.tableWidthRatio,
                tableHeightRatio = previewRatio.tableHeightRatio,
            )
        }
        val rendered = remember(renderBounds, placement, resolvedCells, rows, cols, wmValueScale, previewRatio.tableWidthRatio, templateState.cells) {
            buildContentDrivenRenderedSceneFromPlacement(
                bounds = renderBounds,
                placement = placement,
                cells = resolvedCells,
                templateCells = templateState.cells,
                rows = rows,
                cols = cols,
                valueScale = wmValueScale,
                baseScaleRatio = previewRatio.tableWidthRatio,
            )
        }
        val renderedScene = rendered.scene
        val rootHitRects = renderedScene.rootRects

        // 실제 프리뷰와 동일한 표 렌더 코어를 Layout 편집영역에도 재사용.
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (renderBounds.width() <= 0f || renderBounds.height() <= 0f) return@Canvas

            drawIntoCanvas { canvas ->
                TableRenderAdapter.drawScene(
                    canvas = canvas.nativeCanvas,
                    scene = renderedScene,
                    payload = TableRenderPayload(
                        rows = rows,
                        cols = cols,
                        rowWeights = rendered.resolvedLayout.finalRowWeights,
                        colWeights = rendered.resolvedLayout.finalColWeights,
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
                )
            }

            // 편집 화면 선택 강조:
            // - 테이블 테마(밝음/어두움)에 맞춰 fill/border 색을 분기해 가독성을 유지한다.
                val effectiveSelectionIds = if (isStructureMode && structureSelectedCellIds.isNotEmpty()) {
                    structureSelectedCellIds
                } else {
                    selectedCellId?.let { setOf(it) } ?: emptySet()
                }
                val selectedRects = effectiveSelectionIds.mapNotNull { selectedId ->
                    rootHitRects.firstOrNull { it.cellId == selectedId }
                }

                selectedRects.forEach { rect ->
                    val l = renderedScene.tableRect.left + rect.left
                    val t = renderedScene.tableRect.top + rect.top
                    val spanW = rect.right - rect.left
                    val spanH = rect.bottom - rect.top
                    drawRect(
                        color = if (isStructureMode) selectedFillColor.copy(alpha = 0.14f) else selectedFillColor,
                        topLeft = Offset(l, t),
                        size = androidx.compose.ui.geometry.Size(spanW, spanH)
                    )
                    if (!isStructureMode) {
                        val selectionStrokeWidth = if (rect.cellId == editingCellId) 4f else 3f
                        drawRect(
                            color = selectedBorderColor,
                            topLeft = Offset(l, t),
                            size = androidx.compose.ui.geometry.Size(spanW, spanH),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = selectionStrokeWidth)
                        )
                    }
                }

                if (isStructureMode && selectedRects.isNotEmpty()) {
                    val left = selectedRects.minOf { it.left }
                    val top = selectedRects.minOf { it.top }
                    val right = selectedRects.maxOf { it.right }
                    val bottom = selectedRects.maxOf { it.bottom }
                    drawRect(
                        color = selectedBorderColor,
                        topLeft = Offset(
                            renderedScene.tableRect.left + left,
                            renderedScene.tableRect.top + top,
                        ),
                        size = androidx.compose.ui.geometry.Size(
                            right - left,
                            bottom - top,
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f),
                    )
                }

                if (isStructureMode && DEBUG_ROOT_HIT_RECTS) {
                    rootHitRects.forEach { rect ->
                        drawRect(
                            color = Color.Red.copy(alpha = 0.75f),
                            topLeft = Offset(renderedScene.tableRect.left + rect.left, renderedScene.tableRect.top + rect.top),
                        size = androidx.compose.ui.geometry.Size(
                            rect.right - rect.left,
                            rect.bottom - rect.top,
                        ),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
            }
        }

        rootHitRects.forEach { rect ->
            val cell = rootCellById[rect.cellId] ?: return@forEach
            val cellX = renderedScene.tableRect.left + rect.left
            val cellY = renderedScene.tableRect.top + rect.top
            val cellW = rect.right - rect.left
            val cellH = rect.bottom - rect.top
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
                        x = with(density) { renderedScene.tableRect.left.toDp() },
                        y = with(density) { renderedScene.tableRect.top.toDp() }
                    )
                    .width(with(density) { renderedScene.tableRect.width().toDp() })
                    .height(with(density) { renderedScene.tableRect.height().toDp() })
                    .zIndex(2f)
                    .pointerInput(rows, cols, rootHitRects, renderedScene.tableRect) {
                        detectTapGestures { offset ->
                            hitTestRootCellId(
                                rootRects = rootHitRects,
                                localX = offset.x,
                                localY = offset.y,
                            )?.let(onSelectCell)
                        }
                    }
                    .pointerInput(rows, cols, rootHitRects) {
                        var startCellId: String? = null
                        detectDragGestures(
                            onDragStart = { offset ->
                                startCellId = hitTestRootCellId(
                                    rootRects = rootHitRects,
                                    localX = offset.x,
                                    localY = offset.y,
                                )
                                startCellId?.let { onSelectRange(it, it) }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val start = startCellId ?: return@detectDragGestures
                                val end = hitTestRootCellId(
                                    rootRects = rootHitRects,
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

private fun hitTestRootCellId(
    rootRects: List<RenderRootRect>,
    localX: Float,
    localY: Float,
    clampToBounds: Boolean = false,
): String? {
    if (rootRects.isEmpty()) return null
    val maxRight = rootRects.maxOf { it.right }
    val maxBottom = rootRects.maxOf { it.bottom }
    val x = when {
        clampToBounds -> localX.coerceIn(0f, maxRight)
        localX !in 0f..maxRight -> return null
        else -> localX
    }
    val y = when {
        clampToBounds -> localY.coerceIn(0f, maxBottom)
        localY !in 0f..maxBottom -> return null
        else -> localY
    }
    return rootRects.firstOrNull { rect ->
        val containsX = if (x == maxRight) x >= rect.left && x <= rect.right else x >= rect.left && x < rect.right
        val containsY = if (y == maxBottom) y >= rect.top && y <= rect.bottom else y >= rect.top && y < rect.bottom
        containsX && containsY
    }?.cellId
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
