package com.dudoziworkshop.dzlog.ui.common

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

data class DDZSegmentedControlOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

data class DDZSegmentedControlStyle(
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
    val selectedTextColor: Color,
    val unselectedTextColor: Color,
    val selectedContainerColor: Color,
    val unselectedContainerColor: Color,
    val borderColor: Color,
)

private const val SegmentGroupBorderAlpha = 0.45f
private const val SegmentSelectedContainerAlpha = 0.46f

private val DefaultSegmentedControlStyle = DDZSegmentedControlStyle(
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
)

private val QuickSettingsSegmentedControlStyle = DDZSegmentedControlStyle(
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
)

private val CameraPanelSegmentedControlStyle = DDZSegmentedControlStyle(
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
)

object DDZSegmentedControlStyles {
    val Default: DDZSegmentedControlStyle = DefaultSegmentedControlStyle
    val QuickSettings: DDZSegmentedControlStyle = QuickSettingsSegmentedControlStyle
    val CameraPanel: DDZSegmentedControlStyle = CameraPanelSegmentedControlStyle
}

@Composable
fun DDZSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    style: DDZSegmentedControlStyle,
) {
    DDZSegmentedControl(
        options = options.mapIndexed { index, label ->
            DDZSegmentedControlOption(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
            )
        },
        modifier = modifier,
        style = style,
    )
}

@Composable
fun DDZSegmentedControl(
    options: List<DDZSegmentedControlOption>,
    modifier: Modifier = Modifier,
    style: DDZSegmentedControlStyle = DDZSegmentedControlStyles.Default,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(style.widthFraction)
            .padding(horizontal = style.outerHorizontalPadding),
        shape = style.groupShape,
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, style.borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (style.fixedHeight != null) Modifier.height(style.fixedHeight) else Modifier)
                .padding(
                    horizontal = style.innerHorizontalPadding,
                    vertical = style.innerVerticalPadding,
                ),
            horizontalArrangement = Arrangement.spacedBy(style.itemSpacing),
        ) {
            options.forEach { option ->
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
                            if (option.selected) {
                                style.selectedContainerColor
                            } else {
                                style.unselectedContainerColor
                            }
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = option.onClick,
                        )
                        .padding(
                            horizontal = style.itemHorizontalPadding,
                            vertical = style.itemVerticalPadding,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = option.label,
                        style = style.textStyle,
                        color = if (option.selected) {
                            style.selectedTextColor
                        } else {
                            style.unselectedTextColor
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Compatibility overload for the existing placement preview.
 * Remove after that screen is migrated to a named DDZSegmentedControlStyle.
 */
@Composable
fun DDZSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 30.dp,
    horizontalPadding: Dp = 0.dp,
    verticalPadding: Dp = 0.dp,
    textStyle: TextStyle = DDZTypography.SegmentSmall,
) {
    val legacyStyle = DDZSegmentedControlStyle(
        groupShape = RoundedCornerShape(8.dp),
        itemShape = RoundedCornerShape(0.dp),
        widthFraction = 1f,
        outerHorizontalPadding = 0.dp,
        innerHorizontalPadding = 0.dp,
        innerVerticalPadding = 0.dp,
        itemSpacing = 0.dp,
        fixedHeight = height,
        minItemHeight = height,
        itemHorizontalPadding = horizontalPadding,
        itemVerticalPadding = verticalPadding,
        textStyle = textStyle,
        selectedTextColor = DDZColor.PrimaryDark,
        unselectedTextColor = DDZColor.Primary,
        selectedContainerColor = DDZColor.Surface,
        unselectedContainerColor = DDZColor.Card,
        borderColor = DDZColor.Border,
    )

    DDZSegmentedControl(
        options = options,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        modifier = modifier,
        style = legacyStyle,
    )
}
