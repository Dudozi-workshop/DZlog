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
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import java.util.Locale
import kotlin.math.roundToInt

private val CHIP_SHAPE = RoundedCornerShape(999.dp)
private val PRESET_VALUES_TENTHS = listOf(10, 20, 40, 100)

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
            // UX 정책 유지: collapsed/expanded 모두 바깥 카드 강조를 제거하고 컨트롤 자체 가시성에 집중한다.
            .background(DDZColor.Card.copy(alpha = 0f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 34.dp, minHeight = 34.dp)
                .background(DDZColor.Surface.copy(alpha = 0.95f), CHIP_SHAPE)
                .border(1.dp, DDZColor.SageBorder, CHIP_SHAPE)
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
                steps = (normalizedMaxTenths - 10).coerceAtLeast(1) - 1,
                colors = SliderDefaults.colors(
                    thumbColor = DDZColor.SageDarkStrong,
                    activeTrackColor = DDZColor.SagePrimary,
                    inactiveTrackColor = DDZColor.Card.copy(alpha = 0.95f)
                )
            )

            // 정책 유지: 프리셋은 빠른 이동용이며, 지원 최대 줌을 넘는 경우 가능한 범위로 자동 보정한다.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PRESET_VALUES_TENTHS.forEach { presetTenths ->
                    val actualPreset = presetTenths.coerceIn(10, normalizedMaxTenths)
                    val selected = normalizedTenths == actualPreset
                    val presetBackground: Color
                    val presetBorder: Color
                    val presetText: Color
                    if (selected) {
                        presetBackground = DDZColor.SageLight.copy(alpha = 0.82f)
                        presetBorder = DDZColor.SageDarkStrong
                        presetText = DDZColor.SageDarkStrong
                    } else {
                        // UX 2차 보정 유지: 밝은 프리뷰에서도 프리셋이 묻히지 않도록 웜 베이지 대비를 강화한다.
                        presetBackground = DDZColor.Primary.copy(alpha = 0.14f)
                        presetBorder = DDZColor.Primary.copy(alpha = 0.30f)
                        presetText = DDZColor.PrimaryElevated
                    }

                    Box(
                        modifier = Modifier
                            .background(color = presetBackground, shape = CHIP_SHAPE)
                            .border(1.dp, presetBorder, CHIP_SHAPE)
                            .clickable { onZoomTenthsChange(actualPreset) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = presetLabel(presetTenths),
                            style = DDZTypography.Caption,
                            color = presetText
                        )
                    }
                }
            }
        }
    }
}

private fun presetLabel(presetTenths: Int): String = when (presetTenths) {
    10 -> "1x"
    20 -> "2x"
    40 -> "4x"
    else -> "10x"
}
