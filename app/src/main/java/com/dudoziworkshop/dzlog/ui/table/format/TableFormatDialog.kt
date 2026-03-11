package com.dudoziworkshop.dzlog.ui.table.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.HourSystem
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import com.dudoziworkshop.dzlog.domain.model.TimeSeparator
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import com.dudoziworkshop.dzlog.ui.table.template.updateCell

@Composable
fun TableFormatDialog(
    state: TableFormatDialogState,
    templateState: TableTemplateState,
    onTemplateChange: (TableTemplateState) -> Unit,
    onClose: () -> Unit,
    dateFormatOptions: List<String>
) {
    if (!state.isVisible) return

    val targetId = state.targetCellId
    val targetType = state.targetType
    val targetCell = templateState.cells.firstOrNull { it.cellId == targetId }

    if (targetId == null || targetType == null || targetCell == null) {
        onClose()
        return
    }

    val isDate = (targetType == TableCellDataType.DATE)
    val isTime = (targetType == TableCellDataType.TIME)

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                when {
                    isDate -> "날짜 형식"
                    isTime -> "시간 형식"
                    else -> "형식 설정"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when {
                    isDate -> {
                        val current = targetCell.formatPattern
                        dateFormatOptions.forEach { p ->
                            TextButton(
                                onClick = {
                                    val updated = updateCell(templateState, targetId) { c ->
                                        c.copy(formatPattern = p)
                                    }
                                    onTemplateChange(updated)
                                    onClose()
                                }
                            ) {
                                Text(if (current == p) "✓  $p" else p, style = DDZTypography.ButtonText)
                            }
                        }
                    }

                    isTime -> {
                        Text("시간 형식은 파일명/저장경로/카운터스코프 공용 정책으로 HHmm(분 단위)만 사용합니다.", style = DDZTypography.Caption)
                        TextButton(
                            onClick = {
                                val updated = updateCell(templateState, targetId) { c ->
                                    c.copy(
                                        timeFormatOptions = TimeFormatOptions(
                                            hourSystem = HourSystem.H24,
                                            includeSeconds = false,
                                            separator = TimeSeparator.NONE
                                        ),
                                        formatPattern = "HHmm"
                                    )
                                }
                                onTemplateChange(updated)
                                onClose()
                            }
                        ) {
                            Text("HHmm 적용", style = DDZTypography.ButtonText)
                        }
                    }

                    else -> Text("지원되지 않는 타입")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text("닫기", style = DDZTypography.ButtonText)
            }
        }
    )
}
