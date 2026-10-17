package com.dudoziworkshop.dzlog.ui.table.template

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Only the stored template is read. Opening this dialog never changes the active template. */
@Composable
internal fun TableTemplateReadOnlyPreviewDialog(
    template: SavedTableTemplate,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(template.name, style = DDZTypography.CardTitle) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("표 내용 미리보기 · 읽기 전용", style = DDZTypography.Caption, color = DDZColor.TextMuted)
                val state = template.templateState
                val gridWidth = 108.dp * state.cols.coerceAtLeast(1)
                val gridHeight = 64.dp * state.rows.coerceAtLeast(1)
                val gridCells = remember(state) { templatePreviewCellBounds(state) }
                Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    Box(
                        modifier = Modifier
                            .size(width = gridWidth, height = gridHeight)
                            .background(DDZColor.SurfaceSoft),
                    ) {
                        gridCells.forEach { entry ->
                            Surface(
                                modifier = Modifier
                                    .offset(x = gridWidth * entry.left, y = gridHeight * entry.top)
                                    .size(
                                        width = gridWidth * entry.width,
                                        height = gridHeight * entry.height,
                                    ),
                                shape = RoundedCornerShape(0.dp),
                                color = DDZColor.Surface,
                                border = BorderStroke(0.5.dp, DDZColor.SageBorder),
                            ) {
                                Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.CenterStart) {
                                    Text(
                                        previewCellText(entry.cell, state).ifBlank { " " },
                                        style = DDZTypography.Body,
                                        color = DDZColor.TextPrimary,
                                        maxLines = 5,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                    }
                }
                Text("병합 크기와 행·열 비율을 반영합니다. 날짜·시간·자동번호는 실제 촬영 시 계산됩니다.",
                    style = DDZTypography.Caption, color = DDZColor.TextMuted)
            }
        },
        confirmButton = { TextButton(onClick = onSelect) { Text("이 템플릿 선택") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        containerColor = DDZColor.Card,
    )
}

private fun previewCellText(cell: TableCellState, template: TableTemplateState): String = when (cell.dataType) {
    TableCellDataType.ROTATING_TEXT -> template.phraseSets
        .firstOrNull { it.id == cell.phraseSetId }
        ?.items?.getOrNull(cell.phraseIndex)?.takeIf { it.isNotBlank() } ?: "순환문구"
    TableCellDataType.DATE -> "날짜"
    TableCellDataType.TIME -> "시간"
    TableCellDataType.COUNTER -> "자동번호"
    TableCellDataType.TEXT, TableCellDataType.NUMBER -> cell.rawText
}
