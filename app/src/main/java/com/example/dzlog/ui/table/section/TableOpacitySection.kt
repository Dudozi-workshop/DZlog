package com.example.dzlog.ui.table.section

import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * 워터마크 표 배경 투명도(0~255) 조절 UI를 담당하는 stateless 섹션.
 */
@Composable
fun TableOpacitySection(
    wmBgAlpha: Int,
    onBgAlphaChange: (Int) -> Unit
) {
    Text("투명도", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Text(
        text = wmBgAlpha.toString(),
        style = DDZTypography.Body,
        color = DDZColor.TextPrimary
    )

    Slider(
        value = wmBgAlpha.coerceIn(0, 255).toFloat(),
        onValueChange = { onBgAlphaChange(it.toInt().coerceIn(0, 255)) },
        valueRange = 0f..255f
    )
}
