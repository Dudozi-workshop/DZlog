package com.example.dzlog.ui.table

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dzlog.domain.model.WatermarkTableAnchor

@Composable
fun WatermarkPlacementControls(
    wmAnchor: WatermarkTableAnchor,
    wmWidthRatio: Int,
    wmHeightRatio: Int,
    onAnchorChange: (WatermarkTableAnchor) -> Unit,
    onWidthRatioChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("표 위치/크기", fontSize = 13.sp, color = Color.DarkGray)

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = wmAnchor == WatermarkTableAnchor.TOP_LEFT,
                onClick = { onAnchorChange(WatermarkTableAnchor.TOP_LEFT) }
            )
            Text("좌상", color = Color.Black)
            Spacer(Modifier.width(8.dp))
            RadioButton(
                selected = wmAnchor == WatermarkTableAnchor.TOP_RIGHT,
                onClick = { onAnchorChange(WatermarkTableAnchor.TOP_RIGHT) }
            )
            Text("우상", color = Color.Black)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = wmAnchor == WatermarkTableAnchor.BOTTOM_LEFT,
                onClick = { onAnchorChange(WatermarkTableAnchor.BOTTOM_LEFT) }
            )
            Text("좌하", color = Color.Black)
            Spacer(Modifier.width(8.dp))
            RadioButton(
                selected = wmAnchor == WatermarkTableAnchor.BOTTOM_RIGHT,
                onClick = { onAnchorChange(WatermarkTableAnchor.BOTTOM_RIGHT) }
            )
            Text("우하", color = Color.Black)
        }

        Text(
            text = "표 크기 (가로 ${wmWidthRatio}%, 세로 ${wmHeightRatio}%)",
            color = Color.Black
        )
        Slider(
            value = wmWidthRatio.toFloat(),
            onValueChange = { onWidthRatioChange(it.toInt()) },
            valueRange = 40f..100f
        )

        Text(
            text = "※ 촬영 화면/홈/설정 미리보기에도 동일하게 반영됨",
            fontSize = 11.sp,
            color = Color.DarkGray
        )
    }
}
