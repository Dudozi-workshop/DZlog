package com.dudoziworkshop.dzlog.domain.counter

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellState

data class CounterNumberPlan(val current: Int, val display: String, val next: Int)

fun planCounterNumber(cell: TableCellState?, override: Int?, digits: Int): CounterNumberPlan {
    val seed = (cell?.typedValue as? CellValue.CounterSeed)?.start?.takeIf { it >= 0 }
    val current = override?.coerceAtLeast(1) ?: seed ?: 1
    val width = digits.coerceIn(0, 6)
    val display = if (width == 0) current.toString() else current.toString().padStart(width, '0')
    return CounterNumberPlan(current, display, current + 1)
}
