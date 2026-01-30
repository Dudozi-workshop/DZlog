package com.example.dzlog.ui.table

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.table.ResolvePlan

/**
 * 표 상세설정 전용 "편집형" 미리보기
 * - DisplayTablePreview(표+값만)와 달리, CellHeaderBadges(스티커/배지) 포함
 * - 편집/클릭/더블클릭 없음 (미리보기)
 * - 실제 편집 그리드와 같은 계산(행/열 기반)으로 그려서 "다른 표"처럼 보이지 않게 함
 */
@Composable
fun EditorTableMiniPreview(
    templateState: TableTemplateState,
    plan: ResolvePlan,
    modifier: Modifier = Modifier,
    containerBg: Color = Color(0xFFF2F2F2),
    cellBg: Color = Color(0xFFF7F4EE),
    borderColor: Color = Color(0xFFBDBDBD),
    cellHeight: Dp = 44.dp,
) {
    Box(
        modifier = modifier
            .background(containerBg)
            .padding(6.dp)
    ) {
        @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val cols = templateState.cols.coerceAtLeast(1)
            val rows = templateState.rows.coerceAtLeast(1)
            val cellW = maxWidth / cols
            // 높이는 고정 cellHeight를 우선으로 하되, 화면이 더 작으면 fit
            val desiredH = cellHeight * rows
            val cellH = if (desiredH > maxHeight) (maxHeight / rows) else cellHeight

            Column(Modifier.fillMaxSize()) {
                repeat(rows) { r ->
                    Row(Modifier.fillMaxWidth()) {
                        repeat(cols) { c ->
                            val cell = templateState.cells.firstOrNull { it.rowIndex == r && it.colIndex == c }
                            val display = cell?.let { st ->
                                plan.resolvedCells.firstOrNull { it.id == st.cellId }?.resolvedText
                            }.orEmpty()

                            Box(
                                modifier = Modifier
                                    .width(cellW)
                                    .height(cellH)
                                    .padding(2.dp)
                                    .background(cellBg)
                                    .border(1.dp, borderColor),
                            ) {
                                if (cell != null) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            CellHeaderBadges(cell)
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = display,
                                                fontSize = 12.sp,
                                                color = Color.Black,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
