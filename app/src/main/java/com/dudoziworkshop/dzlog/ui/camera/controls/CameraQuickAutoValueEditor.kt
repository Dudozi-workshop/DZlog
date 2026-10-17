package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.quickDateFormats
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun CameraQuickAutoValueEditor(
    cell: TableCellState,
    template: TableTemplateState,
    now: Date,
    enabled: Boolean,
    onDateFormat: (String) -> Unit,
    onPhraseSet: (String) -> Unit,
) {
    when (cell.dataType) {
        TableCellDataType.DATE -> {
            Text("날짜 형식", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                quickDateFormats.forEach { pattern ->
                    val chosen = cell.formatPattern.ifBlank { "yyyyMMdd" } == pattern
                    DDZButton(
                        text = SimpleDateFormat(pattern, Locale.getDefault()).format(now),
                        modifier = Modifier.weight(1f),
                        style = DDZButtonStyle.Secondary,
                        containerColorOverride = if (chosen) DDZColor.SelectedSoft else null,
                        enabled = enabled,
                        onClick = { onDateFormat(pattern) },
                    )
                }
            }
        }
        TableCellDataType.TIME -> {
            Text("시간은 촬영 시각을 기준으로 자동 입력됩니다. (24시간 HHmm)",
                style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
            Text(SimpleDateFormat("HHmm", Locale.getDefault()).format(now),
                style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
        }
        TableCellDataType.ROTATING_TEXT -> {
            Text("순환문구 세트", style = DDZTypography.SettingLabel, color = DDZColor.TextPrimary)
            val available = template.phraseSets.filter { it.items.isNotEmpty() }
            if (available.isEmpty()) {
                Text("등록된 문구가 없습니다. 표 상세설정에서 먼저 문구를 만들어 주세요.",
                    style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    available.forEach { set ->
                        DDZButton(
                            text = set.name,
                            modifier = Modifier.fillMaxWidth(),
                            style = DDZButtonStyle.Secondary,
                            containerColorOverride = if (cell.phraseSetId == set.id) DDZColor.SelectedSoft else null,
                            enabled = enabled,
                            onClick = { onPhraseSet(set.id) },
                        )
                    }
                }
            }
        }
        else -> Unit
    }
}
