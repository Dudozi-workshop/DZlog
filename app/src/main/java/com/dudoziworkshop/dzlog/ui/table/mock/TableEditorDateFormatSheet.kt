package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.preview.TickUnit
import com.dudoziworkshop.dzlog.domain.preview.computeNextDelayMillis
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val dateOptions = listOf(
    "yyyyMMdd" to "연월일",
    "yyMMdd" to "짧은 연월일",
    "MMdd" to "월일",
)

internal fun tableEditorDateFormatLabel(pattern: String): String =
    dateOptions.firstOrNull { it.first == pattern.ifBlank { "yyyyMMdd" } }?.second ?: "기존 형식"

@Composable
internal fun TableEditorDateFormatSheet(
    selectedPattern: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val now by produceState(initialValue = Date()) {
        while (isActive) {
            delay(computeNextDelayMillis(TickUnit.DAY))
            value = Date()
        }
    }
    DDZBottomSheet(title = "날짜 형식", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selectedPattern.isNotBlank() && dateOptions.none { it.first == selectedPattern }) {
                Text(
                    "기존 형식을 사용 중이에요. 선택하면 새 형식으로 바뀌어요.",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
            }
            dateOptions.forEach { (pattern, label) ->
                val isSelected = selectedPattern.ifBlank { "yyyyMMdd" } == pattern
                Surface(
                    onClick = { onSelect(pattern) },
                    modifier = Modifier.fillMaxWidth().semantics { selected = isSelected },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) DDZColor.SelectedSoft else DDZColor.Surface,
                    border = BorderStroke(1.dp, if (isSelected) DDZColor.SageBorder else DDZColor.Border),
                ) {
                    Row(
                        modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                SimpleDateFormat(pattern, Locale.getDefault()).format(now),
                                style = DDZTypography.SettingLabel,
                                color = DDZColor.TextPrimary,
                            )
                            Text(label, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                        }
                        if (isSelected) Icon(Icons.Default.Check, "현재 선택", tint = DDZColor.SelectedDark)
                    }
                }
            }
        }
    }
}
