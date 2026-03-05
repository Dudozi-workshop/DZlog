package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState

/**
 * 표시용 now 갱신 주기만 결정한다.
 * 카운터 스코프(counterScopeMode)와는 무관하다.
 */
fun decideTickUnitFromTemplate(cells: List<TableCellState>): TickUnit {
    val hasSecond = cells.any {
        it.dataType == TableCellDataType.TIME &&
            it.timeFormatOptions?.includeSeconds == true
    }
    if (hasSecond) return TickUnit.SECOND

    val hasTime = cells.any {
        it.dataType == TableCellDataType.TIME
    }
    if (hasTime) return TickUnit.MINUTE

    val hasDate = cells.any {
        it.dataType == TableCellDataType.DATE
    }
    if (hasDate) return TickUnit.DAY

    return TickUnit.MINUTE
}
