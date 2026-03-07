package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun ZoomControlSection(
    zoomRatioTenths: Int,
    maxZoomTenths: Int,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onZoomTenthsChange: (Int) -> Unit
) {
    val normalizedMaxTenths = maxZoomTenths.coerceAtLeast(10)
    val normalizedTenths = zoomRatioTenths.coerceIn(10, normalizedMaxTenths)
    val zoomLabel = String.format(Locale.US, "%.1fx", normalizedTenths / 10f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            // UX 마감: expanded 상태에서는 바깥 박스 강조를 줄이고 내부 컨트롤 중심으로 보이게 한다.
            .background(
                color = DDZColor.Card.copy(alpha = if (expanded) 0f else 0.35f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 34.dp, minHeight = 34.dp)
                .background(DDZColor.Surface.copy(alpha = 0.95f), RoundedCornerShape(999.dp))
                .border(1.dp, DDZColor.SageBorder, RoundedCornerShape(999.dp))
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = zoomLabel, color = DDZColor.TextStrong, style = DDZTypography.Caption)
        }

        if (expanded) {
            Slider(
                modifier = Modifier.width(200.dp),
                value = normalizedTenths / 10f,
                onValueChange = {
                    val stepped = (it * 10f).roundToInt().coerceIn(10, normalizedMaxTenths)
                    onZoomTenthsChange(stepped)
                },
                valueRange = 1f..(normalizedMaxTenths / 10f),
                steps = (normalizedMaxTenths - 10).coerceAtLeast(1) - 1
            )

            // 정책: 프리셋은 빠른 이동용이며, 지원 최대 줌을 넘는 경우 가능한 범위로 자동 보정한다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(10, 20, 40, 100).forEach { presetTenths ->
                    val actualPreset = presetTenths.coerceIn(10, normalizedMaxTenths)
                    val selected = normalizedTenths == actualPreset
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selected) DDZColor.SageLight.copy(alpha = 0.65f) else DDZColor.Surface.copy(alpha = 0.96f),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .border(1.dp, if (selected) DDZColor.SageDark else DDZColor.Border, RoundedCornerShape(999.dp))
                            .clickable { onZoomTenthsChange(actualPreset) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (presetTenths == 10) "1x" else if (presetTenths == 20) "2x" else if (presetTenths == 40) "4x" else "10x",
                            style = DDZTypography.Caption,
                            color = if (selected) DDZColor.SageDark else DDZColor.TextPrimary
                        )
                    }
                }
            }
        }
    }
}
