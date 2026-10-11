package com.dudoziworkshop.dzlog.ui.camera.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
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
private val PANEL_SETTING_ROW_HEIGHT = 44.dp

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
                Box(
                    modifier = Modifier.fillMaxWidth().height(PANEL_SETTING_ROW_HEIGHT),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "촬영 설정",
                        style = DDZTypography.OverlayTitleCompact,
                        color = DDZColor.Primary,
                        textAlign = TextAlign.Center,
                    )
                    IconButton(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
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

                CompactSettingsRow("비율") {
                    CompactSegments(listOf(
                        DDZSegmentedControlOption("1:1", captureAspect == CaptureAspect.R1_1) { onCaptureAspectChange(CaptureAspect.R1_1) },
                        DDZSegmentedControlOption("3:4", captureAspect == CaptureAspect.R3_4) { onCaptureAspectChange(CaptureAspect.R3_4) },
                        DDZSegmentedControlOption("9:16", captureAspect == CaptureAspect.R9_16) { onCaptureAspectChange(CaptureAspect.R9_16) },
                    ))
                }
                CompactSettingsRow("저장") {
                    CompactSegments(listOf(
                        DDZSegmentedControlOption("원본", saveMode == SaveMode.ORIGINAL_ONLY) { onSaveModeChange(SaveMode.ORIGINAL_ONLY) },
                        DDZSegmentedControlOption("표 합성", saveMode == SaveMode.WATERMARK_ONLY) { onSaveModeChange(SaveMode.WATERMARK_ONLY) },
                        DDZSegmentedControlOption("둘 다", saveMode == SaveMode.BOTH) { onSaveModeChange(SaveMode.BOTH) },
                    ))
                }
                CompactSettingsRow("촬영 확인") {
                    CompactSegments(listOf(
                        DDZSegmentedControlOption("안 함", continuousPreviewMode == ContinuousPreviewMode.OFF) { onContinuousPreviewModeChange(ContinuousPreviewMode.OFF) },
                        DDZSegmentedControlOption("잠깐", continuousPreviewMode == ContinuousPreviewMode.SHORT) { onContinuousPreviewModeChange(ContinuousPreviewMode.SHORT) },
                        DDZSegmentedControlOption("유지", continuousPreviewMode == ContinuousPreviewMode.HOLD) { onContinuousPreviewModeChange(ContinuousPreviewMode.HOLD) },
                    ))
                }
                CompactSettingsRow("피드백") {
                    CameraFeedbackChip("소리", captureSoundEnabled, { onCaptureSoundChange(!captureSoundEnabled) }, Modifier.weight(1f))
                    CameraFeedbackChip("진동", captureHapticEnabled, { onCaptureHapticChange(!captureHapticEnabled) }, Modifier.weight(1f))
                }
                CompactSettingsRow("음량키") {
                    CompactSegments(listOf(
                        DDZSegmentedControlOption("안 함", volumeKeyAction == VolumeKeyAction.NONE) { onVolumeKeyActionChange(VolumeKeyAction.NONE) },
                        DDZSegmentedControlOption("배율", volumeKeyAction == VolumeKeyAction.ZOOM) { onVolumeKeyActionChange(VolumeKeyAction.ZOOM) },
                        DDZSegmentedControlOption("촬영", volumeKeyAction == VolumeKeyAction.CAPTURE) { onVolumeKeyActionChange(VolumeKeyAction.CAPTURE) },
                    ))
                }
            }
        }
    }
}

private val CompactCameraSegments = DDZSegmentedControlStyles.CameraPanel.copy(
    widthFraction = 1f,
    fixedHeight = 40.dp,
    minItemHeight = 40.dp,
    outerHorizontalPadding = 0.dp,
    innerHorizontalPadding = 1.dp,
    innerVerticalPadding = 0.dp,
    itemHorizontalPadding = 1.dp,
    itemSpacing = 1.dp,
    selectedContainerColor = DDZColor.SageLight,
    textStyle = DDZTypography.Caption.copy(fontSize = 11.sp),
)

@Composable
private fun CompactSettingsRow(label: String, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(PANEL_SETTING_ROW_HEIGHT),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.width(55.dp).height(PANEL_SETTING_ROW_HEIGHT), contentAlignment = Alignment.Center) {
            Text(label, style = DDZTypography.Caption.copy(fontSize = 11.sp), color = DDZColor.Primary, textAlign = TextAlign.Center, maxLines = 1)
        }
        content()
    }
}

@Composable
private fun RowScope.CompactSegments(options: List<DDZSegmentedControlOption>) {
    DDZSegmentedControl(options = options, modifier = Modifier.weight(1f), style = CompactCameraSegments)
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
                style = DDZTypography.Caption.copy(fontSize = 11.sp),
                color = if (selected) DDZColor.SageDarkStrong else DDZColor.TextMuted,
            )
        }
    }
}
