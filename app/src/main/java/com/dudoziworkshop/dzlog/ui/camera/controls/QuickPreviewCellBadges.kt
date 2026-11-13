package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.feature.table.policy.QuickPreviewCellBadgePlan
import com.dudoziworkshop.dzlog.ui.common.DDZCellTypeIcons
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout

/** Edit-only status icons. No color panel, cell measurement, or saved-photo content. */
@Composable
internal fun QuickPreviewCellBadges(
    type: TableCellDataType,
    plan: QuickPreviewCellBadgePlan,
    active: Boolean,
    modifier: Modifier = Modifier,
    darkTable: Boolean = false,
    transparentTable: Boolean = false,
) {
    val size = with(LocalDensity.current) { plan.sizePx.toDp() }
    // Use icon-only contrast: light on dark, dark on light, outlined white on photos.
    val tint = when {
        transparentTable -> Color.White
        darkTable -> Color(0xFFF2F5F0)
        active -> DDZColor.SelectionEmphasis
        else -> DDZColor.TextPrimary
    }
    val cellTypeIcon = when (type) {
        TableCellDataType.TEXT -> DDZCellTypeIcons.Text
        TableCellDataType.NUMBER -> DDZCellTypeIcons.Number
        TableCellDataType.DATE -> DDZCellTypeIcons.Date
        TableCellDataType.TIME -> DDZCellTypeIcons.Time
        TableCellDataType.COUNTER -> DDZCellTypeIcons.AutoNumber
        TableCellDataType.ROTATING_TEXT -> DDZCellTypeIcons.RotatingText
    }

    Box(modifier) {
        MarkerIcon(
            image = cellTypeIcon, size = size, tint = tint, outline = transparentTable,
            modifier = Modifier.align(Alignment.TopStart).padding(DDZLayout.QuickInput.BadgeInset),
        )
        if (plan.fileName || plan.path) {
            // Keep both links even on narrow cells; only their layout changes.
            if (plan.stackLinks) {
                Column(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                        .padding(DDZLayout.QuickInput.BadgeInset),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    if (plan.fileName) MarkerIcon(
                        image = Icons.Filled.Description, size = size, tint = tint, outline = transparentTable,
                    )
                    if (plan.path) MarkerIcon(
                        image = Icons.Filled.Folder, size = size, tint = tint, outline = transparentTable,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.align(Alignment.TopEnd)
                        .wrapContentSize(align = Alignment.TopEnd, unbounded = true)
                        .padding(DDZLayout.QuickInput.BadgeInset),
                    horizontalArrangement = Arrangement.spacedBy(DDZLayout.QuickInput.BadgeInset),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (plan.fileName) MarkerIcon(
                        image = Icons.Filled.Description, size = size, tint = tint, outline = transparentTable,
                    )
                    if (plan.path) MarkerIcon(
                        image = Icons.Filled.Folder, size = size, tint = tint, outline = transparentTable,
                    )
                }
            }
        }
    }
}

/** An icon-shaped hairline, not a rectangle, keeps markers visible over changing photos. */
@Composable
private fun MarkerIcon(
    image: ImageVector,
    size: Dp,
    tint: Color,
    outline: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier.size(size)) {
        if (outline) {
            val edge = Color.Black.copy(alpha = 0.9f)
            for ((dx, dy) in listOf(
                -0.6.dp to 0.dp, 0.6.dp to 0.dp, 0.dp to -0.6.dp, 0.dp to 0.6.dp,
            )) {
                Icon(
                    imageVector = image,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().offset(x = dx, y = dy),
                    tint = edge,
                )
            }
        }
        Icon(
            imageVector = image,
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            tint = tint,
        )
    }
}
