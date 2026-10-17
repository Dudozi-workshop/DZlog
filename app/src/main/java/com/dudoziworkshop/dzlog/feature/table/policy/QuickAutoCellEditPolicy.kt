package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * Quick editing is opt-in per cell. Counters are deliberately excluded:
 * changing a counter seed would bypass the existing counter synchronisation rules.
 */
internal fun quickSelectableCells(template: TableTemplateState): List<TableCellState> =
    quickRootCells(template)
        .filter { cell ->
            cell.quickEditable && cell.dataType != TableCellDataType.COUNTER
        }
        .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))

internal val quickDateFormats: List<String> = listOf("yyyyMMdd", "yyMMdd", "MMdd")

internal fun updateQuickDateFormat(
    template: TableTemplateState,
    cellId: String,
    pattern: String,
): TableTemplateState {
    if (pattern !in quickDateFormats) return template
    val target = template.cells.firstOrNull {
        it.cellId == cellId &&
            it.quickEditable &&
            it.dataType == TableCellDataType.DATE
    } ?: return template
    if (target.formatPattern == pattern) return template
    return template.copy(cells = template.cells.map { cell ->
        if (cell.cellId == cellId) cell.copy(formatPattern = pattern) else cell
    })
}

/** Selecting a *different* saved phrase set resets only that cell's phrase progress.
 * Simply opening quick edit, or reselecting the same set, does not advance the cursor.
 */
internal fun selectQuickPhraseSet(
    template: TableTemplateState,
    cellId: String,
    phraseSetId: String,
): TableTemplateState {
    if (template.phraseSets.none { it.id == phraseSetId && it.items.isNotEmpty() }) return template
    val target = template.cells.firstOrNull {
        it.cellId == cellId &&
            it.quickEditable &&
            it.dataType == TableCellDataType.ROTATING_TEXT
    } ?: return template
    if (target.phraseSetId == phraseSetId) return template
    return template.copy(cells = template.cells.map { cell ->
        if (cell.cellId == cellId) cell.copy(
            phraseSetId = phraseSetId,
            everyOverride = null,
            phraseIndex = 0,
            phraseCompletedShots = 0,
        ) else cell
    })
}
