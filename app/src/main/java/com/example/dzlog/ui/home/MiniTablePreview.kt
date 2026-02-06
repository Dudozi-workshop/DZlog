package com.example.dzlog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.ui.common.TablePreviewFrame
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * 홈 화면용 "표 전체 미리보기"(A안)
 * - 실제 rows/cols 기반으로 전체 그리드를 축소 렌더링
 * - 입력/편집 없음(프리뷰 전용)
 */
@Composable
fun MiniTablePreview(
    templateState: TableTemplateState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val rows = templateState.rows.coerceIn(1, 12)
    val cols = templateState.cols.coerceIn(1, 8)
    val cellMap = templateState.cells.associateBy { "${it.rowIndex}:${it.colIndex}" }

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxSize()
    ) {
        Text(
            text = "표 미리보기",
            style = DDZTypography.CardTitle,
            color = DDZColor.TextPrimary
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .weight(1f, fill = true)
                .background(DDZColor.Surface, RoundedCornerShape(10.dp))
                .border(1.dp, DDZColor.Border, RoundedCornerShape(10.dp))
                .padding(6.dp)
        ) {
            TablePreviewFrame(
                aspectRatio = cols / rows.toFloat(),
                modifier = Modifier.fillMaxSize()
            ) { innerModifier ->
                Column(
                    modifier = innerModifier,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(rows) { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = true),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(cols) { c ->
                                val cell = cellMap["$r:$c"]
                                MiniCell(
                                    cell = cell,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCell(
    cell: TableCellState?,
    modifier: Modifier
) {
    val text = cell?.label?.takeIf { it.isNotBlank() } ?: cellTypeShort(cell?.dataType)
    Box(
        modifier = modifier
            .background(DDZColor.Background, RoundedCornerShape(4.dp))
            .border(1.dp, DDZColor.Border, RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = DDZTypography.Caption,
            color = DDZColor.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.sizeIn(maxWidth = 999.dp)
        )
    }
}

private fun cellTypeShort(type: TableCellDataType?): String {
    return when (type) {
        TableCellDataType.TEXT -> "T"
        TableCellDataType.NUMBER -> "N"
        TableCellDataType.DATE -> "D"
        TableCellDataType.TIME -> "시간"
        TableCellDataType.COUNTER -> "카운터"
        else -> ""
    }
}
