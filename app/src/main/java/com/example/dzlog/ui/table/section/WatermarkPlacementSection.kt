package com.example.dzlog.ui.table.section

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.WatermarkTableAnchor
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * 워터마크 표의 배치(Anchor)와 크기(가로/세로 비율) UI를 담당하는 stateless 섹션.
 */
@Composable
fun WatermarkPlacementSection(
    wmAnchor: WatermarkTableAnchor,
    wmWidthRatio: Int,
    wmHeightRatio: Int,
    onAnchorChange: (WatermarkTableAnchor) -> Unit,
    onWidthRatioChange: (Int) -> Unit,
    onHeightRatioChange: (Int) -> Unit
) {
    Text("표 위치/크기", style = DDZTypography.CardTitle, color = DDZColor.TextMuted)

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = wmAnchor == WatermarkTableAnchor.TOP_LEFT,
            onClick = { onAnchorChange(WatermarkTableAnchor.TOP_LEFT) }
        )
        Text("좌상", color = DDZColor.TextPrimary)
        Spacer(Modifier.width(8.dp))
        RadioButton(
            selected = wmAnchor == WatermarkTableAnchor.TOP_RIGHT,
            onClick = { onAnchorChange(WatermarkTableAnchor.TOP_RIGHT) }
        )
        Text("우상", color = DDZColor.TextPrimary)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = wmAnchor == WatermarkTableAnchor.BOTTOM_LEFT,
            onClick = { onAnchorChange(WatermarkTableAnchor.BOTTOM_LEFT) }
        )
        Text("좌하", color = DDZColor.TextPrimary)
        Spacer(Modifier.width(8.dp))
        RadioButton(
            selected = wmAnchor == WatermarkTableAnchor.BOTTOM_RIGHT,
            onClick = { onAnchorChange(WatermarkTableAnchor.BOTTOM_RIGHT) }
        )
        Text("우하", color = DDZColor.TextPrimary)
    }

    Text(
        text = "표 크기 (가로 ${wmWidthRatio}%, 세로 ${wmHeightRatio}%)",
        color = DDZColor.TextPrimary
    )

    Text("가로", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Slider(
        value = wmWidthRatio.toFloat(),
        onValueChange = { onWidthRatioChange(it.toInt()) },
        valueRange = 40f..100f
    )

    Text("세로", style = DDZTypography.Caption, color = DDZColor.TextMuted)
    Slider(
        value = wmHeightRatio.toFloat(),
        onValueChange = { onHeightRatioChange(it.toInt()) },
        valueRange = 10f..35f
    )

    Text(
        text = "※ 촬영 화면/홈/설정 미리보기에는 동일하게 반영됨",
        style = DDZTypography.Caption,
        color = DDZColor.TextMuted
    )
}
