package com.dudoziworkshop.dzlog.ui.home

import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Places the capture action around the viewport center while respecting the measured
 * top/bottom groups. Only the two inter-zone gaps grow or shrink; the footer stays intact.
 * When content does not fit, the parent scroll container can expand this layout's height.
 */
@Composable
internal fun HomeThreeZoneLayout(
    minViewportHeight: Dp,
    minTopGap: Dp,
    minBottomGap: Dp,
    targetActionCenterFraction: Float,
    modifier: Modifier = Modifier,
    top: @Composable () -> Unit,
    action: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
) {
    Layout(
        content = {
            top()
            action()
            bottom()
        },
        modifier = modifier.heightIn(min = minViewportHeight),
    ) { measurables, constraints ->
        check(measurables.size == 3) { "HomeThreeZoneLayout requires exactly three root elements." }

        val childConstraints = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val topPlaceable = measurables[0].measure(childConstraints)
        val actionPlaceable = measurables[1].measure(childConstraints)
        val bottomPlaceable = measurables[2].measure(childConstraints)

        val topGapPx = minTopGap.roundToPx()
        val bottomGapPx = minBottomGap.roundToPx()
        val neededHeight = topPlaceable.height + topGapPx + actionPlaceable.height +
            bottomGapPx + bottomPlaceable.height
        val layoutHeight = max(constraints.minHeight, neededHeight)
        val bottomY = layoutHeight - bottomPlaceable.height
        val actionMinY = topPlaceable.height + topGapPx
        val actionMaxY = bottomY - bottomGapPx - actionPlaceable.height
        val centerTargetY = (layoutHeight * targetActionCenterFraction -
            actionPlaceable.height / 2f).roundToInt()
        val actionY = centerTargetY.coerceIn(actionMinY, actionMaxY)

        layout(constraints.maxWidth, layoutHeight) {
            topPlaceable.placeRelative(0, 0)
            actionPlaceable.placeRelative(0, actionY)
            bottomPlaceable.placeRelative(0, bottomY)
        }
    }
}
