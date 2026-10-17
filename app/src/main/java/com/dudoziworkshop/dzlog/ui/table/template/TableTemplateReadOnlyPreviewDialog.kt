package com.dudoziworkshop.dzlog.ui.table.template

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
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
                val roots = TableStructureRangeActions.rootCells(state.cells)
                val columns = state.cols.coerceAtLeast(1)
                val width = 108.dp
                Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    (0 until state.rows).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                            var col = 0
                            while (col < columns) {
                                val root = roots.firstOrNull { it.rowIndex == row && it.colIndex == col }
                                val covering = roots.any {
                                    row in it.rowIndex until (it.rowIndex + it.rowSpan) &&
                                        col in it.colIndex until (it.colIndex + it.colSpan) &&
                                        (it.rowIndex != row || it.colIndex != col)
                                }
                                if (root != null) {
                                    Surface(
                                        modifier = Modifier.width(width * root.colSpan.coerceAtLeast(1))
                                            .heightIn(min = 64.dp),
                                        shape = RoundedCornerShape(0.dp),
                                        color = DDZColor.Surface,
                                        border = BorderStroke(0.5.dp, DDZColor.SageBorder),
                                    ) {
                                        Box(modifier = Modifier.padding(8.dp), contentAlignment = Alignment.CenterStart) {
                                            Text(
                                                previewCellText(root, state).ifBlank { " " },
                                                style = DDZTypography.Body,
                                                color = DDZColor.TextPrimary,
                                            )
                                        }
                                    }
                                } else if (!covering) {
                                    Surface(
                                        modifier = Modifier.width(width).heightIn(min = 64.dp),
                                        border = BorderStroke(0.5.dp, DDZColor.SageBorder),
                                    ) { Box(Modifier.padding(8.dp)) {} }
                                }
                                col++
                            }
                        }
                    }
                }
                Text("병합된 셀은 시작 위치에 표시됩니다. 날짜·시간·자동번호는 실제 촬영 시 계산됩니다.",
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
