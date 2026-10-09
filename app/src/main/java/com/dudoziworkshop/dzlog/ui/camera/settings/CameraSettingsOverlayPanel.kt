package com.dudoziworkshop.dzlog.ui.camera.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.VolumeKeyAction
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControl
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControlOption
import com.dudoziworkshop.dzlog.ui.common.DDZSegmentedControlStyles
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private const val PANEL_WIDTH_FRACTION = 0.72f
private const val PANEL_DIM_ALPHA = 0.2f
private val PANEL_MAX_WIDTH = 320.dp
// 1단계 라운딩 토큰: 패널 외곽은 Medium 기준선을 사용한다.
private val PANEL_CORNER_RADIUS = DDZLayout.Radius.Medium
// 토큰 정책: 패널 헤더 터치 영역은 compact control 규격을 사용한다.
private val PANEL_HEADER_ICON_TOUCH = DDZLayout.Control.Compact
private val PANEL_HEADER_ICON_SIZE = 12.dp

@Composable
internal fun CameraSettingsOverlayPanel(
    captureAspect: CaptureAspect,
    onCaptureAspectChange: (CaptureAspect) -> Unit,
    saveMode: SaveMode,
    onSaveModeChange: (SaveMode) -> Unit,
    continuousPreviewMode: ContinuousPreviewMode,
    onContinuousPreviewModeChange: (ContinuousPreviewMode) -> Unit,
    captureSoundEnabled: Boolean,
    onCaptureSoundChange: (Boolean) -> Unit,
    captureHapticEnabled: Boolean,
    onCaptureHapticChange: (Boolean) -> Unit,
    volumeKeyAction: VolumeKeyAction,
    onVolumeKeyActionChange: (VolumeKeyAction) -> Unit,
    onDismiss: () -> Unit
) {
    val dismissInteraction = remember { MutableInteractionSource() }
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
    val panelTopPadding = (statusBarPadding.calculateTopPadding() - 1.dp).coerceAtLeast(0.dp)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DDZColor.PrimaryDark.copy(alpha = PANEL_DIM_ALPHA))
                .clickable(
                    interactionSource = dismissInteraction,
                    indication = null,
                    onClick = onDismiss
                )
        )

        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    // 정책 보정: safe inset은 유지하되 시각적 떠보임을 줄이기 위해 상단 간격을 1dp만 당긴다.
                    top = panelTopPadding,
                    end = 0.dp
                )
                .fillMaxWidth(PANEL_WIDTH_FRACTION)
                .widthIn(max = PANEL_MAX_WIDTH)
                .heightIn(max = 440.dp)
                .zIndex(30f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(PANEL_CORNER_RADIUS),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            color = DDZColor.Card
        ) {
            Column(
                // UX 3차 보정: 내부 상단 여백을 제거해 패널 시작점을 safe 영역 바로 아래로 더 밀착시킨다.
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp, top = 4.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "촬영 설정",
                        style = DDZTypography.OverlayTitleCompact,
                        color = DDZColor.Primary
                    )
                    IconButton(
                        modifier = Modifier
                            .width(PANEL_HEADER_ICON_TOUCH)
                            .height(PANEL_HEADER_ICON_TOUCH),
                        onClick = onDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = DDZColor.Primary,
                            modifier = Modifier.width(PANEL_HEADER_ICON_SIZE)
                        )
                    }
                }

                HorizontalDivider(color = DDZColor.Border)

                SettingSectionTitle("촬영 비율")
                CompactSegments(
                    options = listOf(
                        DDZSegmentedControlOption("1:1", captureAspect == CaptureAspect.R1_1) { onCaptureAspectChange(CaptureAspect.R1_1) },
                        DDZSegmentedControlOption("3:4", captureAspect == CaptureAspect.R3_4) { onCaptureAspectChange(CaptureAspect.R3_4) },
                        DDZSegmentedControlOption("9:16", captureAspect == CaptureAspect.R9_16) { onCaptureAspectChange(CaptureAspect.R9_16) }
                    )
                )

                SettingSectionTitle("사진 저장")
                CompactSegments(
                    options = listOf(
                        DDZSegmentedControlOption("원본", saveMode == SaveMode.ORIGINAL_ONLY) { onSaveModeChange(SaveMode.ORIGINAL_ONLY) },
                        DDZSegmentedControlOption("표 합성", saveMode == SaveMode.WATERMARK_ONLY) { onSaveModeChange(SaveMode.WATERMARK_ONLY) },
                        DDZSegmentedControlOption("둘 다", saveMode == SaveMode.BOTH) { onSaveModeChange(SaveMode.BOTH) }
                    )
                )

                SettingSectionTitle("촬영 후 확인")
                CompactSegments(
                    options = listOf(
                        DDZSegmentedControlOption("안 함", continuousPreviewMode == ContinuousPreviewMode.OFF) {
                            onContinuousPreviewModeChange(ContinuousPreviewMode.OFF)
                        },
                        DDZSegmentedControlOption("잠깐", continuousPreviewMode == ContinuousPreviewMode.SHORT) {
                            onContinuousPreviewModeChange(ContinuousPreviewMode.SHORT)
                        },
                        DDZSegmentedControlOption("유지", continuousPreviewMode == ContinuousPreviewMode.HOLD) {
                            onContinuousPreviewModeChange(ContinuousPreviewMode.HOLD)
                        }
                    )
                )

                SettingSectionTitle("촬영 피드백")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CameraFeedbackChip(
                        label = "소리",
                        selected = captureSoundEnabled,
                        onClick = { onCaptureSoundChange(!captureSoundEnabled) },
                        modifier = Modifier.weight(1f),
                    )
                    CameraFeedbackChip(
                        label = "진동",
                        selected = captureHapticEnabled,
                        onClick = { onCaptureHapticChange(!captureHapticEnabled) },
                        modifier = Modifier.weight(1f),
                    )
                }

                SettingSectionTitle("음량키")
                CompactSegments(
                    options = listOf(
                        DDZSegmentedControlOption("사용 안 함", volumeKeyAction == VolumeKeyAction.NONE) {
                            onVolumeKeyActionChange(VolumeKeyAction.NONE)
                        },
                        DDZSegmentedControlOption("배율", volumeKeyAction == VolumeKeyAction.ZOOM) {
                            onVolumeKeyActionChange(VolumeKeyAction.ZOOM)
                        },
                        DDZSegmentedControlOption("촬영", volumeKeyAction == VolumeKeyAction.CAPTURE) {
                            onVolumeKeyActionChange(VolumeKeyAction.CAPTURE)
                        },
                    )
                )
            }
        }
    }
}

@Composable
private fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        style = DDZTypography.SectionLabelCompact,
        color = DDZColor.Primary
    )
}

@Composable
private fun CompactSegments(options: List<DDZSegmentedControlOption>) {
    // 3단계 정책: 촬영설정 패널도 공통 SegmentedControl 렌더러를 사용해 중복 UI를 제거한다.
    DDZSegmentedControl(
        options = options,
        style = DDZSegmentedControlStyles.CameraPanel
    )
}

@Composable
private fun CameraFeedbackChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(DDZLayout.Radius.Small),
        color = if (selected) DDZColor.SageLight else DDZColor.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) DDZColor.SageBorder else DDZColor.Border,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = DDZTypography.Caption,
                color = if (selected) DDZColor.SageDarkStrong else DDZColor.TextMuted,
            )
        }
    }
}
