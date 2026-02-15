package com.example.dzlog.ui.camera.settings

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
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography

private const val PANEL_WIDTH_FRACTION = 0.6f
private val PANEL_MAX_WIDTH = 420.dp
private const val PANEL_DIM_ALPHA = 0.2f
private val SEGMENT_HEIGHT = 38.dp

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
                    top = statusBarPadding.calculateTopPadding() + DDZSpacing.sectionGap,
                    end = DDZSpacing.screenPadding
                )
                .fillMaxWidth(PANEL_WIDTH_FRACTION)
                .widthIn(max = PANEL_MAX_WIDTH),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            color = DDZColor.Card.copy(alpha = 0.97f)
        ) {
            Column(
                modifier = Modifier.padding(DDZSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "촬영 설정",
                        style = DDZTypography.SectionTitle,
                        color = DDZColor.Primary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = DDZColor.Primary
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

                SettingSectionTitle("촬영 모드")
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
        style = DDZTypography.Body,
        color = DDZColor.Primary
    )
}

@Composable
private fun ConnectedSegments(options: List<SegmentOption>) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
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

@Composable
private fun SegmentItem(
    option: SegmentOption,
    modifier: Modifier = Modifier
) {
    val background = if (option.selected) {
        DDZColor.Success.copy(alpha = 0.28f)
    } else {
        DDZColor.Surface
    }

    val textColor = if (option.selected) DDZColor.Success else DDZColor.Primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .clickable(onClick = option.onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = option.label,
            style = DDZTypography.SegmentSmall,
            color = textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
