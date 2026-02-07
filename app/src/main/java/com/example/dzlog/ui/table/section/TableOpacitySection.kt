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
    // 내부값(0~255) → UI 퍼센트(0~100)
    val percent = (wmBgAlpha.coerceIn(0, 255) * 100) / 255

    // ⬆️ 텍스트 먼저
    Text("투명도", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Text(
        text = "$percent%",
        style = DDZTypography.Body,
        color = DDZColor.TextPrimary
    )

    // ⬇️ 슬라이더
    Slider(
        value = percent.toFloat(),
        onValueChange = { uiValue ->
            val p = uiValue.toInt().coerceIn(0, 100)
            // UI 퍼센트(0~100) → 내부값(0~255)
            val alpha255 = (p * 255) / 100
            onBgAlphaChange(alpha255.coerceIn(0, 255))
        },
        valueRange = 0f..100f
    )

    Slider(
        value = wmBgAlpha.coerceIn(0, 255).toFloat(),
        onValueChange = { onBgAlphaChange(it.toInt().coerceIn(0, 255)) },
        valueRange = 0f..255f
    )
}
