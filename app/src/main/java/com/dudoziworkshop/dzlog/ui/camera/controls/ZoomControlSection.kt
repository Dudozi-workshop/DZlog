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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

// 2단계 라운딩 토큰: 줌 칩/프리셋은 pill 계열로 Full 고정한다.
private val CHIP_SHAPE = RoundedCornerShape(DDZLayout.Radius.Full)
private val PRESET_VALUES_TENTHS = listOf(10, 20, 40, 100)

@Composable
internal fun ZoomControlSection(
    zoomRatioTenths: Int,
    maxZoomTenths: Int,
    expanded: Boolean,
    hapticEnabled: Boolean,
    onToggleExpanded: () -> Unit,
    onZoomTenthsChange: (Int) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val normalizedMaxTenths = maxZoomTenths.coerceIn(10, 100)
    val normalizedTenths = zoomRatioTenths.coerceIn(10, normalizedMaxTenths)
    val zoomLabel = formatZoomActualLabel(normalizedTenths)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            // UX 정책 유지: collapsed/expanded 모두 바깥 카드 강조를 제거하고 컨트롤 자체 가시성에 집중한다.
            // 2단계 라운딩 토큰: 줌 영역 outer 컨테이너는 Medium을 사용한다.
            .background(DDZColor.Card.copy(alpha = 0f), RoundedCornerShape(DDZLayout.Radius.Medium))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = DDZLayout.Control.Standard, minHeight = DDZLayout.Control.Standard)
                .background(DDZColor.Surface.copy(alpha = 0.95f), CHIP_SHAPE)
                .border(1.dp, DDZColor.SageBorder, CHIP_SHAPE)
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = zoomLabel, color = DDZColor.TextStrong, style = DDZTypography.Caption)
        }

        if (expanded) {
            ZoomTickBar(
                zoomTenths = normalizedTenths,
                maxZoomTenths = normalizedMaxTenths,
                hapticEnabled = hapticEnabled,
                onZoomTenthsChange = onZoomTenthsChange,
                onStepHaptic = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                modifier = Modifier.fillMaxWidth(0.76f)
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
                            .clickable {
                                if (hapticEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onZoomTenthsChange(actualPreset)
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = formatZoomActualLabel(presetTenths),
                            style = DDZTypography.Caption,
                            color = presetText
                        )
                    }
                }
            }
        }
    }
}
