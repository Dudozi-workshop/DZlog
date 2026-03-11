package com.dudoziworkshop.dzlog.ui.table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.ui.common.DzIcon
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * Cell top badges (표시 전용)
 * - 📁n : 경로 단계 (GroupLevel -> index)
 * - 🏷n : 파일명 구성 요소 (fileName slot draft index)
 */
@Composable
fun CellHeaderBadgesOverlay(
    cell: TableCellState,
    fileNameSlotIndex: Int?,
    modifier: Modifier = Modifier
) {
    val dirIndex: Int? = when (cell.groupLevel) {
        GroupLevel.G1 -> 1
        GroupLevel.G2 -> 2
        else -> null
    }

    val hasDir = (dirIndex != null)
    val hasName = (fileNameSlotIndex != null)

    if (!hasDir && !hasName) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        if (hasDir) {
            BadgeChip { DzIcon.Directory(dirIndex!!).Render() }
        } else {
            Spacer(Modifier.width(1.dp))
        }

        if (hasName) {
            BadgeChip { DzIcon.NameTag(fileNameSlotIndex!! + 1).Render() }
        } else {
            Spacer(Modifier.width(1.dp))
        }
    }
}

@Composable
private fun BadgeChip(content: @Composable () -> Unit) {
    Surface(
        color = DDZColor.Background.copy(alpha = 0.75f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, DDZColor.TextMuted),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 5.dp, vertical = 1.dp)
                .graphicsLayer(scaleX = 0.85f, scaleY = 0.85f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}
