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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private const val PANEL_WIDTH_FRACTION = 0.6f
private const val PANEL_DIM_ALPHA = 0.2f
private val PANEL_MAX_WIDTH = 420.dp
private val PANEL_CORNER_RADIUS = 14.dp
private val SEGMENT_HEIGHT = 30.dp
private val SEGMENT_CORNER_RADIUS = 10.dp
private const val SEGMENT_WIDTH_FRACTION = 0.95f
private val PANEL_HEADER_ICON_TOUCH = 30.dp
private val PANEL_HEADER_ICON_SIZE = 12.dp

@Composable
internal fun CameraSettingsOverlayPanel(
    captureAspect: CaptureAspect,
    onCaptureAspectChange: (CaptureAspect) -> Unit,
    saveMode: SaveMode,
    onSaveModeChange: (SaveMode) -> Unit,
    showGrid: Boolean,
    onShowGridChange: (Boolean) -> Unit,
    showTable: Boolean,
    onShowTableChange: (Boolean) -> Unit,
    continuousPreviewMode: ContinuousPreviewMode,
    onContinuousPreviewModeChange: (ContinuousPreviewMode) -> Unit,
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
                .zIndex(30f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(PANEL_CORNER_RADIUS),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            color = DDZColor.Card.copy(alpha = 0.97f)
        ) {
            Column(
                // UX 3차 보정: 내부 상단 여백을 제거해 패널 시작점을 safe 영역 바로 아래로 더 밀착시킨다.
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 0.dp, bottom = 6.dp),
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
                ConnectedSegments(
                    options = listOf(
                        SegmentOption("1:1", captureAspect == CaptureAspect.R1_1) { onCaptureAspectChange(CaptureAspect.R1_1) },
                        SegmentOption("3:4", captureAspect == CaptureAspect.R3_4) { onCaptureAspectChange(CaptureAspect.R3_4) },
                        SegmentOption("9:16", captureAspect == CaptureAspect.R9_16) { onCaptureAspectChange(CaptureAspect.R9_16) }
                    )
                )

                SettingSectionTitle("저장 방식")
                ConnectedSegments(
                    options = listOf(
                        SegmentOption("원본", saveMode == SaveMode.ORIGINAL_ONLY) { onSaveModeChange(SaveMode.ORIGINAL_ONLY) },
                        SegmentOption("워터마크", saveMode == SaveMode.WATERMARK_ONLY) { onSaveModeChange(SaveMode.WATERMARK_ONLY) },
                        SegmentOption("둘 다", saveMode == SaveMode.BOTH) { onSaveModeChange(SaveMode.BOTH) }
                    )
                )

                SettingSectionTitle("미리보기")
                ConnectedSegments(
                    options = listOf(
                        SegmentOption("없음", continuousPreviewMode == ContinuousPreviewMode.OFF) {
                            onContinuousPreviewModeChange(ContinuousPreviewMode.OFF)
                        },
                        SegmentOption("짧게", continuousPreviewMode == ContinuousPreviewMode.SHORT) {
                            onContinuousPreviewModeChange(ContinuousPreviewMode.SHORT)
                        },
                        SegmentOption("고정", continuousPreviewMode == ContinuousPreviewMode.HOLD) {
                            onContinuousPreviewModeChange(ContinuousPreviewMode.HOLD)
                        }
                    )
                )

                SettingSectionTitle("화면 표기")
                ConnectedSegments(
                    options = listOf(
                        SegmentOption("그리드", showGrid) { onShowGridChange(!showGrid) },
                        SegmentOption("표", showTable) { onShowTableChange(!showTable) }
                    )
                )
            }
        }
    }
}

private data class SegmentOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
private fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        style = DDZTypography.SectionLabelCompact,
        color = DDZColor.Primary
    )
}

@Composable
private fun ConnectedSegments(options: List<SegmentOption>) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(SEGMENT_CORNER_RADIUS)
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(SEGMENT_WIDTH_FRACTION)
                .clip(shape),
            shape = shape,
            color = DDZColor.Surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DDZColor.Border)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SEGMENT_HEIGHT)
            ) {
                options.forEachIndexed { index, option ->
                    SegmentItem(
                        option = option,
                        modifier = Modifier.weight(1f)
                    )
                    if (index != options.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(DDZColor.Border)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SegmentItem(
    option: SegmentOption,
    modifier: Modifier = Modifier
) {
    val background = if (option.selected) DDZColor.SageLight.copy(alpha = 0.45f) else DDZColor.Surface
    val textColor = if (option.selected) DDZColor.SageDark else DDZColor.Primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .clickable(onClick = option.onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.label,
            style = DDZTypography.SegmentCompact,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
