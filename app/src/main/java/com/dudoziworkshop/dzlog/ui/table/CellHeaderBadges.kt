package com.dudoziworkshop.dzlog.ui.table

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.ui.common.DzIcon
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * Cell top badges (표시 전용)
 * - 📁n : 경로 단계 (GroupLevel -> index)
 * - 🏷n : 파일명 구성 요소 (fileNameSlots index)
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
            .padding(horizontal = 2.dp),
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
        color = DDZColor.Card,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.padding(top = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}
