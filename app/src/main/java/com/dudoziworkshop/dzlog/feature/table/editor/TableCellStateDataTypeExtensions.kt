package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions

/**
 * Switching a cell type only changes the active representation.
 * TEXT remains in rawText, while NUMBER, COUNTER, DATE and rotating-phrase
 * configuration are preserved as inactive drafts for later restoration.
 */
fun TableCellState.withDataType(newType: TableCellDataType): TableCellState {
    if (dataType == newType) return this

    val preserved = copy(
        numberDraft = if (dataType == TableCellDataType.NUMBER) {
            numberDraft ?: rawText
        } else numberDraft,
        counterSeedDraft = if (dataType == TableCellDataType.COUNTER) {
            (typedValue as? CellValue.CounterSeed)?.start ?: counterSeedDraft ?: 1
        } else counterSeedDraft,
        dateFormatDraft = if (dataType == TableCellDataType.DATE) {
            formatPattern.ifBlank { "yyyyMMdd" }
        } else dateFormatDraft,
    )
    return when (newType) {
        TableCellDataType.TEXT -> preserved.copy(
            dataType = newType,
            typedValue = CellValue.Text(preserved.rawText),
            counterScopeMode = null,
        )
        TableCellDataType.NUMBER -> {
            val number = preserved.numberDraft.orEmpty()
            preserved.copy(
                dataType = newType,
                numberDraft = number,
                typedValue = CellValue.Number(number),
                counterScopeMode = null,
            )
        }
        TableCellDataType.DATE -> preserved.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            formatPattern = preserved.dateFormatDraft ?: "yyyyMMdd",
            counterScopeMode = CounterScopeMode.EXCLUDE,
        )
        TableCellDataType.TIME -> preserved.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            formatPattern = "HHmm",
            timeFormatOptions = preserved.timeFormatOptions ?: TimeFormatOptions(),
            counterScopeMode = CounterScopeMode.EXCLUDE,
        )
        TableCellDataType.COUNTER -> {
            val seed = preserved.counterSeedDraft ?: 1
            preserved.copy(
                dataType = newType,
                typedValue = CellValue.CounterSeed(seed),
                counterSeedDraft = seed,
                counterScopeMode = null,
            )
        }
        TableCellDataType.ROTATING_TEXT -> preserved.copy(
            dataType = newType,
            typedValue = CellValue.Auto,
            counterScopeMode = null,
        )
    }
}
