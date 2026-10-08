package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * Only editable manual values belong here. Counter/rotating/date/time policy stays
 * with the table editor and the camera naming/counter pipeline.
 */
@Composable
internal fun CameraQuickValueSheet(
    template: TableTemplateState,
    onDismiss: () -> Unit,
    onApply: (TableTemplateState) -> Unit,
) {
    val manualCells = remember(template) {
        template.cells
            .filter { it.dataType == TableCellDataType.TEXT || it.dataType == TableCellDataType.NUMBER }
            .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))
    }
    val inputCells = manualCells.filter { it.kind == TableCellKind.INPUT }
    val editableCells = inputCells.ifEmpty { manualCells }
    val drafts = remember(template) {
        editableCells.associate { it.cellId to it.rawText }.toMutableStateMap()
    }
    val changed = editableCells.any { drafts[it.cellId] != it.rawText }

    DDZBottomSheet(onDismiss = onDismiss, title = "빠른 값 변경") {
        Text(
            text = "촬영할 값만 변경합니다. 셀 종류와 자동번호 설정은 표 상세에서 변경하세요.",
            color = DDZColor.TextMuted,
        )
        if (editableCells.isEmpty()) {
            Text(
                text = "직접 변경할 수 있는 텍스트 또는 숫자 셀이 없습니다.",
                modifier = Modifier.padding(vertical = 16.dp),
                color = DDZColor.TextMuted,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 390.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                editableCells.forEach { cell ->
                    DDZTextField(
                        value = drafts[cell.cellId] ?: cell.rawText,
                        onValueChange = { drafts[cell.cellId] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = "${cell.rowIndex + 1}행 ${cell.colIndex + 1}열 · " +
                            if (cell.dataType == TableCellDataType.NUMBER) "숫자" else "텍스트",
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DDZButton(
                text = "취소",
                style = DDZButtonStyle.Secondary,
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            )
            DDZButton(
                text = "적용",
                modifier = Modifier.weight(1f),
                enabled = changed,
                onClick = {
                    val updates = drafts.toMap()
                    onApply(
                        template.copy(cells = template.cells.map { cell ->
                            val updated = updates[cell.cellId]
                            if (updated == null || updated == cell.rawText) cell
                            else when (cell.dataType) {
                                TableCellDataType.TEXT -> cell.copy(
                                    rawText = updated,
                                    typedValue = CellValue.Text(updated),
                                )
                                TableCellDataType.NUMBER -> cell.copy(
                                    rawText = updated,
                                    typedValue = CellValue.Number(updated),
                                )
                                else -> cell
                            }
                        })
                    )
                },
            )
        }
    }
}
