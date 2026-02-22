package com.example.dzlog.ui.table.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * 워터마크 표 배경 스타일 선택 UI를 담당하는 stateless 섹션.
 */
@Composable
fun TableStyleSection(
    wmBgStyle: Int,
    wmGridEnabled: Boolean,
    onBgStyleChange: (Int) -> Unit,
    onGridEnabledChange: (Boolean) -> Unit
) {
    Text("배경", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = wmBgStyle == 0,
            onClick = { onBgStyleChange(0) }
        )
        Text("검정", color = DDZColor.TextPrimary)
        Spacer(Modifier.width(10.dp))
        RadioButton(
            selected = wmBgStyle == 1,
            onClick = { onBgStyleChange(1) }
        )
        Text("하양", color = DDZColor.TextPrimary)
        Spacer(Modifier.width(10.dp))
        RadioButton(
            selected = wmBgStyle == 2,
            onClick = { onBgStyleChange(2) }
        )
        Text("투명", color = DDZColor.TextPrimary)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("격자선", style = DDZTypography.Caption, color = DDZColor.TextMuted)
        Switch(checked = wmGridEnabled, onCheckedChange = onGridEnabledChange)
    }
}
