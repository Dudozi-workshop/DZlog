package com.dudoziworkshop.dzlog.feature.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private val SegmentGroupShape = RoundedCornerShape(18.dp)
private val SegmentItemShape = RoundedCornerShape(15.dp)
private val SegmentOuterHorizontalPadding = 7.dp
private val SegmentInnerPadding = 4.dp
private val SegmentItemMinHeight = 36.dp
private val SegmentItemHorizontalPadding = 12.dp
private val SegmentItemVerticalPadding = 8.dp
private val SegmentItemSpacing = 4.dp

private const val SegmentGroupBorderAlpha = 0.45f
private const val SegmentSelectedContainerAlpha = 0.46f

private val segmentGroupBorderColor = DDZColor.Border.copy(alpha = SegmentGroupBorderAlpha)
private val selectedContainerColor = DDZColor.SageLight.copy(alpha = SegmentSelectedContainerAlpha)
private val unselectedContainerColor = DDZColor.Surface.copy(alpha = 0f)
private val selectedTextColor = DDZColor.SageDark
private val unselectedTextColor = DDZColor.TextMuted

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    // 주요 정책: 설정값 선택/저장(onSelect) 로직은 유지하고, UI 스타일만 soft segmented 톤으로 개선한다.
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SegmentOuterHorizontalPadding),
        shape = SegmentGroupShape,
        color = DDZColor.Surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, segmentGroupBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SegmentInnerPadding, vertical = SegmentInnerPadding),
            horizontalArrangement = Arrangement.spacedBy(SegmentItemSpacing)
        ) {
            options.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = SegmentItemMinHeight)
                        .clip(SegmentItemShape)
                        .background(
                            if (selected) selectedContainerColor else unselectedContainerColor
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onSelect(idx) }
                        )
                        .padding(
                            horizontal = SegmentItemHorizontalPadding,
                            vertical = SegmentItemVerticalPadding
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = DDZTypography.Body,
                        color = if (selected) selectedTextColor else unselectedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
