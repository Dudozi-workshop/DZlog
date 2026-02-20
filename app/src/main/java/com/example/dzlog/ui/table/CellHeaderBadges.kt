package com.example.dzlog.ui.table

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.GroupLevel
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.ui.common.DzIcon

/**
 * Cell top badges (표시 전용)
 * - 📁n : 경로 단계 (GroupLevel -> index)
 * - 🏷n : 파일명 구성 요소 (fileNameSlots index)
 */
@Composable
fun CellHeaderBadges(cell: TableCellState, fileNameSlotIndex: Int?) {
    val dirIndex: Int? = when (cell.groupLevel) {
        GroupLevel.G1 -> 1
        GroupLevel.G2 -> 2
        else -> null
    }

    val hasDir = (dirIndex != null)
    val hasName = (fileNameSlotIndex != null)

    if (!hasDir && !hasName) return

    Row {
        dirIndex?.let { DzIcon.Directory(it).Render() }
        if (hasDir && hasName) Spacer(Modifier.width(6.dp))
        fileNameSlotIndex?.let { DzIcon.NameTag(it + 1).Render() }
    }
}

