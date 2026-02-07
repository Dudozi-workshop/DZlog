package com.example.dzlog.ui.table.section

import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * 워터마크 표 "값" 글씨크기(60~160) 조절 UI를 담당하는 stateless 섹션.
 * - 라벨은 기획 폐기 전제(값만 표시)
 */
@Composable
fun TableValueTextSizeSection(
    wmValueScale: Int,
    onValueScaleChange: (Int) -> Unit
) {
    Text("글씨 크기", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Text(
        text = wmValueScale.toString(),
        style = DDZTypography.Body,
        color = DDZColor.TextPrimary
    )

    Slider(
        value = wmValueScale.coerceIn(60, 160).toFloat(),
        onValueChange = { onValueScaleChange(it.toInt().coerceIn(60, 160)) },
        valueRange = 60f..160f
    )
}
