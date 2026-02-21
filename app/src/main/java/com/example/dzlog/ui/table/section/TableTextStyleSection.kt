package com.example.dzlog.ui.table.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.WatermarkManualTextColor
import com.example.dzlog.domain.model.WatermarkTextAlign
import com.example.dzlog.domain.model.WatermarkTextColorMode
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

@Composable
fun TableTextStyleSection(
    wmTextColorMode: Int,
    wmManualTextColor: Int,
    wmTextAlign: Int,
    wmValueScale: Int,
    onTextColorModeChange: (Int) -> Unit,
    onManualTextColorChange: (Int) -> Unit,
    onTextAlignChange: (Int) -> Unit,
    onValueScaleChange: (Int) -> Unit
) {
    Text("글씨색", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onTextColorModeChange(WatermarkTextColorMode.AUTO) }) {
            Text(if (wmTextColorMode == WatermarkTextColorMode.AUTO) "자동 ✓" else "자동", style = DDZTypography.ButtonText)
        }
        Button(onClick = { onTextColorModeChange(WatermarkTextColorMode.MANUAL) }) {
            Text(if (wmTextColorMode == WatermarkTextColorMode.MANUAL) "수동 ✓" else "수동", style = DDZTypography.ButtonText)
        }
    }

    if (wmTextColorMode == WatermarkTextColorMode.MANUAL) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onManualTextColorChange(WatermarkManualTextColor.WHITE) }) {
                Text(if (wmManualTextColor == WatermarkManualTextColor.WHITE) "흰 ✓" else "흰", style = DDZTypography.ButtonText)
            }
            Button(onClick = { onManualTextColorChange(WatermarkManualTextColor.BLACK) }) {
                Text(if (wmManualTextColor == WatermarkManualTextColor.BLACK) "검 ✓" else "검", style = DDZTypography.ButtonText)
            }
        }
    }

    Text("정렬", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onTextAlignChange(WatermarkTextAlign.LEFT) }) {
            Text(if (wmTextAlign == WatermarkTextAlign.LEFT) "좌 ✓" else "좌", style = DDZTypography.ButtonText)
        }
        Button(onClick = { onTextAlignChange(WatermarkTextAlign.CENTER) }) {
            Text(if (wmTextAlign == WatermarkTextAlign.CENTER) "중 ✓" else "중", style = DDZTypography.ButtonText)
        }
        Button(onClick = { onTextAlignChange(WatermarkTextAlign.RIGHT) }) {
            Text(if (wmTextAlign == WatermarkTextAlign.RIGHT) "우 ✓" else "우", style = DDZTypography.ButtonText)
        }
    }

    Text("글자 크기", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(onClick = { onValueScaleChange(85) }) {
            Text(if (wmValueScale <= 92) "작게 ✓" else "작게", style = DDZTypography.ButtonText)
        }
        Button(onClick = { onValueScaleChange(100) }) {
            Text(if (wmValueScale in 93..107) "기본 ✓" else "기본", style = DDZTypography.ButtonText)
        }
        Button(onClick = { onValueScaleChange(115) }) {
            Text(if (wmValueScale >= 108) "크게 ✓" else "크게", style = DDZTypography.ButtonText)
        }
    }
}
