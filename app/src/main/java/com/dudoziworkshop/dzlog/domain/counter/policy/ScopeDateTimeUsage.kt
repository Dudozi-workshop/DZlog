package com.dudoziworkshop.dzlog.domain.counter.policy

import com.dudoziworkshop.dzlog.domain.model.GroupLevel
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState

data class ScopeDateTimeUsage(
    val usesDateInScope: Boolean,
    val usesTimeInScope: Boolean
)

fun resolveScopeDateTimeUsage(
    cells: List<TableCellState>,
    fileNameSlots: List<String?>
): ScopeDateTimeUsage {
    val cellById = cells.associateBy { it.cellId }

    val usesDateInFileName = fileNameSlots.any { id ->
        id != null && cellById[id]?.dataType == TableCellDataType.DATE
    }
    val usesTimeInFileName = fileNameSlots.any { id ->
        id != null && cellById[id]?.dataType == TableCellDataType.TIME
    }

    val usesDateInPath = cells.any {
        (it.groupLevel == GroupLevel.G1 || it.groupLevel == GroupLevel.G2) && it.dataType == TableCellDataType.DATE
    }
    val usesTimeInPath = cells.any {
        (it.groupLevel == GroupLevel.G1 || it.groupLevel == GroupLevel.G2) && it.dataType == TableCellDataType.TIME
    }

    return ScopeDateTimeUsage(
        usesDateInScope = usesDateInFileName || usesDateInPath,
        usesTimeInScope = usesTimeInFileName || usesTimeInPath
    )
}
