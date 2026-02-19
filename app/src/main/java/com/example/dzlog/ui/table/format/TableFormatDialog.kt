package com.example.dzlog.ui.table.format

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.HourSystem
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.domain.model.TimeFormatOptions
import com.example.dzlog.domain.model.TimeSeparator
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import com.example.dzlog.ui.table.template.updateCell

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
                    isDate -> "DATE 형식"
                    isTime -> "TIME 형식"
                    else -> "형식"
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
                        val initial = targetCell.timeFormatOptions ?: TimeFormatOptions()
                        var hourSystem by remember(targetId) { mutableStateOf(initial.hourSystem) }
                        var includeSeconds by remember(targetId) { mutableStateOf(initial.includeSeconds) }
                        var separator by remember(targetId) { mutableStateOf(initial.separator) }

                        fun apply() {
                            val updated = updateCell(templateState, targetId) { c ->
                                c.copy(
                                    timeFormatOptions = TimeFormatOptions(
                                        hourSystem = hourSystem,
                                        includeSeconds = includeSeconds,
                                        separator = separator
                                    ),
                                    formatPattern = ""
                                )
                            }
                            onTemplateChange(updated)
                        }

                        Text("시간 표시 설정", style = DDZTypography.SectionTitle)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("시간제", modifier = Modifier.width(72.dp))
                            RadioButton(
                                selected = hourSystem == HourSystem.H24,
                                onClick = { hourSystem = HourSystem.H24; apply() }
                            )
                            Text("24h")
                            Spacer(Modifier.width(12.dp))
                            RadioButton(
                                selected = hourSystem == HourSystem.H12,
                                onClick = { hourSystem = HourSystem.H12; apply() }
                            )
                            Text("12h")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("초 포함", modifier = Modifier.width(72.dp))
                            Switch(
                                checked = includeSeconds,
                                onCheckedChange = { includeSeconds = it; apply() }
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("구분자", modifier = Modifier.width(72.dp))
                            listOf(TimeSeparator.COLON, TimeSeparator.NONE).forEach { s ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = separator == s,
                                        onClick = { separator = s; apply() }
                                    )
                                    Text(if (s == TimeSeparator.NONE) "붙이기" else s.token)
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                        }

                        val preview = buildString {
                            append(if (hourSystem == HourSystem.H24) "HH" else "hh")
                            append(separator.token)
                            append("mm")
                            if (includeSeconds) {
                                append(separator.token)
                                append("ss")
                            }
                            if (hourSystem == HourSystem.H12) append(" a")
                        }
                        Text("미리보기: $preview", style = DDZTypography.Caption, color = DDZColor.TextMuted)
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
