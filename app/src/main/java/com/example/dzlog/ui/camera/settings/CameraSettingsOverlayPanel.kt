package com.example.dzlog.ui.camera.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.CaptureAspect
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.domain.model.SaveMode
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing

private const val PANEL_WIDTH_FRACTION = 0.48f
private val PANEL_MAX_WIDTH = 360.dp
private const val PANEL_DIM_ALPHA = 0.2f

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
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface
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
                    Text(text = "촬영 설정", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "닫기")
                    }
                }

                HorizontalDivider()

                SingleChoiceSection(
                    title = "촬영 비율",
                    options = listOf(
                        SegmentOption("1:1", CaptureAspect.R1_1),
                        SegmentOption("3:4", CaptureAspect.R3_4),
                        SegmentOption("9:16", CaptureAspect.R9_16)
                    ),
                    selected = captureAspect,
                    onSelect = onCaptureAspectChange
                )

                SingleChoiceSection(
                    title = "저장 방식",
                    options = listOf(
                        SegmentOption("원본", SaveMode.ORIGINAL_ONLY),
                        SegmentOption("워터마크", SaveMode.WATERMARK_ONLY),
                        SegmentOption("둘 다", SaveMode.BOTH)
                    ),
                    selected = saveMode,
                    onSelect = onSaveModeChange
                )

                ToggleSection(
                    title = "화면 표기",
                    toggles = listOf(
                        ToggleOption("그리드", showGrid, onShowGridChange),
                        ToggleOption("표", showTable, onShowTableChange)
                    )
                )

                SingleChoiceSection(
                    title = "촬영 모드",
                    options = listOf(
                        SegmentOption("없음", ContinuousPreviewMode.OFF),
                        SegmentOption("짧게", ContinuousPreviewMode.SHORT),
                        SegmentOption("고정", ContinuousPreviewMode.HOLD)
                    ),
                    selected = continuousPreviewMode,
                    onSelect = onContinuousPreviewModeChange
                )
            }
        }
    }
}

private data class SegmentOption<T>(
    val label: String,
    val value: T
)

private data class ToggleOption(
    val label: String,
    val checked: Boolean,
    val onChange: (Boolean) -> Unit
)

@Composable
private fun <T> SingleChoiceSection(
    title: String,
    options: List<SegmentOption<T>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        SegmentedButtons(
            labels = options.map { it.label },
            selectedIndex = options.indexOfFirst { it.value == selected }.coerceAtLeast(0),
            onSelect = { index -> onSelect(options[index].value) }
        )
    }
}

@Composable
private fun ToggleSection(
    title: String,
    toggles: List<ToggleOption>
) {
    Column(verticalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)) {
            toggles.forEach { toggle ->
                val container = if (toggle.checked) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                }
                val textColor = if (toggle.checked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { toggle.onChange(!toggle.checked) },
                    shape = MaterialTheme.shapes.small,
                    color = container
                ) {
                    Text(
                        text = toggle.label,
                        modifier = Modifier.padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SegmentedButtons(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DDZSpacing.itemGap)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            val container = if (isSelected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) },
                shape = MaterialTheme.shapes.small,
                color = container
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .padding(contentPadding)
                        .padding(horizontal = DDZSpacing.cardPadding, vertical = DDZSpacing.itemGap),
                    color = textColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
