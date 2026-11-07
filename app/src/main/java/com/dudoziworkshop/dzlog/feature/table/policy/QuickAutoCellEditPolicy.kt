package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * Quick editing is available for every visible root cell except counters.
 * Counter seeds remain protected by the capture counter synchronisation rules.
 * The legacy quickEditable flag is ignored for existing templates.
 */
/**
 * Change only the selected rotating cell, never the shared phrase set or another cell.
 * Stored progress always describes what will be shown on the NEXT successful capture.
 */
internal fun selectQuickPhraseItem(
    template: TableTemplateState,
    cellId: String,
    index: Int,
): TableTemplateState {
    val cell = template.cells.firstOrNull {
        it.cellId == cellId && it.dataType == TableCellDataType.ROTATING_TEXT
    } ?: return template
    val set = template.phraseSets.firstOrNull { it.id == cell.phraseSetId }
        ?.takeIf { it.items.isNotEmpty() } ?: return template
    if (index !in set.items.indices) return template
    if (cell.phraseIndex == index && cell.phraseCompletedShots == 0) return template
    return template.copy(cells = template.cells.map {
        if (it.cellId == cellId) it.copy(phraseIndex = index, phraseCompletedShots = 0) else it
    })
}

/**
 * A fixed value is always stored explicitly, even when it equals the set's
 * current default. "전체 설정" is the ONLY way back to inherited per-item rules.
 */
internal fun updateQuickPhraseEvery(
    template: TableTemplateState,
    cellId: String,
    every: Int,
): TableTemplateState {
    if (every < 1) return template
    val cell = template.cells.firstOrNull {
        it.cellId == cellId && it.dataType == TableCellDataType.ROTATING_TEXT
    } ?: return template
    val set = template.phraseSets.firstOrNull { it.id == cell.phraseSetId }
        ?.takeIf { it.items.isNotEmpty() } ?: return template
    if (cell.everyOverride == every) return template
    return template.copy(cells = template.cells.map {
        if (it.cellId == cellId) it.copy(everyOverride = every, phraseCompletedShots = 0) else it
    })
}

/** Switch just this cell to the saved set's default and per-phrase intervals. */
internal fun followQuickPhraseSetPeriods(
    template: TableTemplateState,
    cellId: String,
): TableTemplateState {
    val cell = template.cells.firstOrNull {
        it.cellId == cellId && it.dataType == TableCellDataType.ROTATING_TEXT
    } ?: return template
    if (cell.everyOverride == null || template.phraseSets.none {
            it.id == cell.phraseSetId && it.items.isNotEmpty()
        }) return template
    return template.copy(cells = template.cells.map {
        if (it.cellId == cellId) it.copy(everyOverride = null, phraseCompletedShots = 0) else it
    })
}

/** Completed captures must remain below the effective cycle length. */
internal fun updateQuickPhraseCompleted(
    template: TableTemplateState,
    cellId: String,
    completed: Int,
): TableTemplateState {
    val cell = template.cells.firstOrNull {
        it.cellId == cellId && it.dataType == TableCellDataType.ROTATING_TEXT
    } ?: return template
    val set = template.phraseSets.firstOrNull { it.id == cell.phraseSetId }
        ?.takeIf { it.items.isNotEmpty() } ?: return template
    val every = (cell.everyOverride ?: set.everyForItem(cell.phraseIndex)).coerceAtLeast(1)
    if (completed !in 0 until every || cell.phraseCompletedShots == completed) return template
    return template.copy(cells = template.cells.map {
        if (it.cellId == cellId) it.copy(phraseCompletedShots = completed) else it
    })
}

internal fun quickSelectableCells(template: TableTemplateState): List<TableCellState> =
    quickRootCells(template)
        .filter { it.dataType != TableCellDataType.COUNTER }
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
