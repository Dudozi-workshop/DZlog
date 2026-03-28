package com.dudoziworkshop.dzlog.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/**
 * 공통 세그먼트 옵션 모델.
 * - 설정 화면(selectedIndex)과 촬영 설정 패널(개별 selected)을 같은 렌더러로 연결하기 위한 브릿지다.
 */
data class SegmentedControlOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

data class SegmentedControlStyle(
    val groupShape: RoundedCornerShape,
    val itemShape: RoundedCornerShape,
    val widthFraction: Float,
    val outerHorizontalPadding: Dp,
    val innerHorizontalPadding: Dp,
    val innerVerticalPadding: Dp,
    val itemSpacing: Dp,
    val fixedHeight: Dp?,
    val minItemHeight: Dp,
    val itemHorizontalPadding: Dp,
    val itemVerticalPadding: Dp,
    val textStyle: TextStyle,
    val selectedTextColor: androidx.compose.ui.graphics.Color,
    val unselectedTextColor: androidx.compose.ui.graphics.Color,
    val selectedContainerColor: androidx.compose.ui.graphics.Color,
    val unselectedContainerColor: androidx.compose.ui.graphics.Color,
    val borderColor: androidx.compose.ui.graphics.Color,
    val showDivider: Boolean,
    val dividerColor: androidx.compose.ui.graphics.Color
)

private const val SegmentGroupBorderAlpha = 0.45f
private const val SegmentSelectedContainerAlpha = 0.46f

// 기존 전체설정 톤 유지용 기본 스타일
private val DefaultSegmentedControlStyle = SegmentedControlStyle(
    groupShape = RoundedCornerShape(DDZLayout.Radius.Medium),
    itemShape = RoundedCornerShape(DDZLayout.Radius.Medium),
    widthFraction = 1f,
    outerHorizontalPadding = 7.dp,
    innerHorizontalPadding = 4.dp,
    innerVerticalPadding = 4.dp,
    itemSpacing = 4.dp,
    fixedHeight = null,
    minItemHeight = 36.dp,
    itemHorizontalPadding = 12.dp,
    itemVerticalPadding = 8.dp,
    textStyle = DDZTypography.Body,
    selectedTextColor = DDZColor.SageDark,
    unselectedTextColor = DDZColor.TextMuted,
    selectedContainerColor = DDZColor.SageLight.copy(alpha = SegmentSelectedContainerAlpha),
    unselectedContainerColor = DDZColor.Surface.copy(alpha = 0f),
    borderColor = DDZColor.Border.copy(alpha = SegmentGroupBorderAlpha),
    showDivider = false,
    dividerColor = DDZColor.Border
)

// 빠른 설정 영역 전용 미세 보정 스타일
private val QuickSettingsSegmentedControlStyle = SegmentedControlStyle(
    groupShape = RoundedCornerShape(DDZLayout.Radius.Small),
    itemShape = RoundedCornerShape(DDZLayout.Radius.Small),
    widthFraction = 1f,
    outerHorizontalPadding = 0.dp,
    innerHorizontalPadding = 3.dp,
    innerVerticalPadding = 3.dp,
    itemSpacing = 3.dp,
    fixedHeight = 40.dp,
    minItemHeight = 40.dp,
    itemHorizontalPadding = 10.dp,
    itemVerticalPadding = 5.dp,
    textStyle = DDZTypography.Caption,
    selectedTextColor = DDZColor.SageDarkStrong,
    unselectedTextColor = DDZColor.TextMuted,
    selectedContainerColor = DDZColor.SageLight.copy(alpha = 0.42f),
    unselectedContainerColor = DDZColor.Surface,
    borderColor = DDZColor.Border,
    showDivider = false,
    dividerColor = DDZColor.Border
)

// 촬영 설정 패널 전용 정돈형 compact 스타일
private val CameraPanelSegmentedControlStyle = SegmentedControlStyle(
    groupShape = RoundedCornerShape(DDZLayout.Radius.Small),
    itemShape = RoundedCornerShape(DDZLayout.Radius.Small),
    widthFraction = 0.95f,
    outerHorizontalPadding = 0.dp,
    innerHorizontalPadding = 2.dp,
    innerVerticalPadding = 2.dp,
    itemSpacing = 2.dp,
    fixedHeight = DDZLayout.Control.Compact,
    minItemHeight = DDZLayout.Control.Compact,
    itemHorizontalPadding = 6.dp,
    itemVerticalPadding = 2.dp,
    textStyle = DDZTypography.Caption,
    selectedTextColor = DDZColor.SageDarkStrong,
    unselectedTextColor = DDZColor.TextMuted,
    selectedContainerColor = DDZColor.SageLight.copy(alpha = 0.42f),
    unselectedContainerColor = DDZColor.Surface,
    borderColor = DDZColor.Border,
    showDivider = false,
    dividerColor = DDZColor.Border
)

object SegmentedControlStyles {
    val Default: SegmentedControlStyle = DefaultSegmentedControlStyle
    val QuickSettings: SegmentedControlStyle = QuickSettingsSegmentedControlStyle
    val CameraPanel: SegmentedControlStyle = CameraPanelSegmentedControlStyle
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: SegmentedControlStyle = SegmentedControlStyles.Default
) {
    val mappedOptions = options.mapIndexed { index, label ->
        SegmentedControlOption(
            label = label,
            selected = index == selectedIndex,
            onClick = { onSelect(index) }
        )
    }
    SegmentedControl(
        options = mappedOptions,
        modifier = modifier,
        style = style
    )
}

@Composable
fun SegmentedControl(
    options: List<SegmentedControlOption>,
    modifier: Modifier = Modifier,
    style: SegmentedControlStyle = SegmentedControlStyles.Default
) {
    // 주요 정책: 선택/저장 로직은 외부에 두고, 세그먼트 렌더링만 공통화한다.
    Surface(
        modifier = modifier
            .fillMaxWidth(style.widthFraction)
            .padding(horizontal = style.outerHorizontalPadding),
        shape = style.groupShape,
        color = DDZColor.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, style.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (style.fixedHeight != null) Modifier.height(style.fixedHeight) else Modifier)
                .padding(
                    horizontal = style.innerHorizontalPadding,
                    vertical = style.innerVerticalPadding
                ),
            horizontalArrangement = Arrangement.spacedBy(style.itemSpacing)
        ) {
            options.forEachIndexed { index, option ->
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            if (style.fixedHeight != null) {
                                Modifier.fillMaxHeight()
                            } else {
                                Modifier.heightIn(min = style.minItemHeight)
                            }
                        )
                        .clip(style.itemShape)
                        .background(
                            if (option.selected) style.selectedContainerColor else style.unselectedContainerColor
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = option.onClick
                        )
                        .padding(
                            horizontal = style.itemHorizontalPadding,
                            vertical = style.itemVerticalPadding
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        style = style.textStyle,
                        color = if (option.selected) style.selectedTextColor else style.unselectedTextColor,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (style.showDivider && index != options.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(style.dividerColor)
                    )
                }
            }
        }
    }
}
