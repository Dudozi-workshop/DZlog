package com.dudoziworkshop.dzlog.ui.table.section

import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * 워터마크 표의 크기(가로/세로 비율) UI를 담당하는 stateless 섹션.
 * 위치는 카메라 프리뷰에서 드래그로만 조정한다.
 */
@Composable
fun WatermarkPlacementSection(
    wmWidthRatio: Int,
    wmHeightRatio: Int,
    onWidthRatioChange: (Int) -> Unit,
    onHeightRatioChange: (Int) -> Unit
) {
    Text("표 크기", style = DDZTypography.CardTitle, color = DDZColor.TextMuted)

    Text("가로 크기: ${wmWidthRatio}%", color = DDZColor.TextPrimary)
    Slider(
        value = wmWidthRatio.toFloat(),
        onValueChange = { onWidthRatioChange(it.toInt().coerceIn(10, 100)) },
        valueRange = 10f..100f
    )

    Text("세로 크기: ${wmHeightRatio}%", color = DDZColor.TextPrimary)
    Slider(
        value = wmHeightRatio.toFloat(),
        onValueChange = { onHeightRatioChange(it.toInt().coerceIn(10, 100)) },
        valueRange = 10f..100f
    )
}
