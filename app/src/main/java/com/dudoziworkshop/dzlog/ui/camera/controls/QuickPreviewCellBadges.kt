package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.feature.table.policy.QuickPreviewCellBadgePlan
import com.dudoziworkshop.dzlog.ui.common.DDZCellTypeIcons
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing

/** Cell-local status overlays; never written into the camera preview/photo scene. */
@Composable
internal fun QuickPreviewCellBadges(
    type: TableCellDataType,
    plan: QuickPreviewCellBadgePlan,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!plan.type) return
    val size = with(LocalDensity.current) { plan.sizePx.toDp() }
    val tint = if (active) DDZColor.SelectionEmphasis else DDZColor.TextSecondary
    val badgeBackground = DDZColor.Surface.copy(alpha = 0.90f)
    val cellTypeIcon = when (type) {
        TableCellDataType.TEXT -> DDZCellTypeIcons.Text
        TableCellDataType.NUMBER -> DDZCellTypeIcons.Number
        TableCellDataType.DATE -> DDZCellTypeIcons.Date
        TableCellDataType.TIME -> DDZCellTypeIcons.Time
        TableCellDataType.COUNTER -> DDZCellTypeIcons.AutoNumber
        TableCellDataType.ROTATING_TEXT -> DDZCellTypeIcons.RotatingText
    }
    Box(modifier) {
        if (plan.compact) {
            // Two lines reduce overlap with neighboring cells. Keep all metadata
            // even when the cell is smaller than the combined badge footprint.
            Column(
                modifier = Modifier.align(Alignment.TopStart)
                    .padding(DDZSpacing.xs)
                    .wrapContentSize(unbounded = true)
                    .background(badgeBackground, RoundedCornerShape(2.dp)),
                verticalArrangement = Arrangement.spacedBy(0.dp),
                horizontalAlignment = Alignment.Start,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(cellTypeIcon, contentDescription = null, modifier = Modifier.size(size), tint = tint)
                    if (plan.locked) Icon(
                        Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(size), tint = tint,
                    )
                }
                if (plan.fileName || plan.path) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (plan.fileName) Icon(
                            Icons.Filled.Description, contentDescription = null,
                            modifier = Modifier.size(size), tint = tint,
                        )
                        if (plan.path) Icon(
                            Icons.Filled.Folder, contentDescription = null,
                            modifier = Modifier.size(size), tint = tint,
                        )
                    }
                }
            }
        } else {
        Row(
            modifier = Modifier.align(Alignment.TopStart)
                .padding(DDZSpacing.xs)
                .background(badgeBackground, RoundedCornerShape(2.dp)),
            horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(cellTypeIcon, contentDescription = null, modifier = Modifier.size(size), tint = tint)
            if (plan.locked) Icon(
                Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(size), tint = tint,
            )
        }
        if (plan.fileName || plan.path) {
            Row(
                modifier = Modifier.align(Alignment.TopEnd)
                    .padding(DDZSpacing.xs)
                    .background(badgeBackground, RoundedCornerShape(2.dp)),
                horizontalArrangement = Arrangement.spacedBy(DDZSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (plan.fileName) Icon(
                    Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(size), tint = tint,
                )
                if (plan.path) Icon(
                    Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(size), tint = tint,
                )
            }
        }
        }
    }
}
