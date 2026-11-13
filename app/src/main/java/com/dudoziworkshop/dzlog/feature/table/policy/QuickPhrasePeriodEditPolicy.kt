package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress

/** Set mode edits the shared default; cell mode stores a local fixed interval. */
internal data class QuickPhrasePeriodEdit(
    val cellId: String,
    val useSetDefault: Boolean,
    val every: Int,
)

internal data class QuickPhrasePeriodConflict(
    val cellId: String,
    val row: Int,
    val column: Int,
    val phraseIndex: Int,
    val completed: Int,
    val newEvery: Int,
)

private fun currentEvery(cell: TableCellState, set: RotatingPhraseSet): Int =
    (cell.everyOverride ?: set.everyForItem(cell.phraseIndex)).coerceAtLeast(1)

/** Build the proposed state without resetting progress; canceling never touches the draft. */
internal fun previewQuickPhrasePeriodEdit(
    template: TableTemplateState,
    edit: QuickPhrasePeriodEdit,
): TableTemplateState? {
    if (edit.every !in 1..RotatingPhraseSet.MAX_EVERY) return null
    val cell = template.cells.firstOrNull {
        it.cellId == edit.cellId && it.dataType == TableCellDataType.ROTATING_TEXT
    } ?: return null
    val set = template.phraseSets.firstOrNull { it.id == cell.phraseSetId && it.items.isNotEmpty() }
        ?: return null
    val changedSets = if (edit.useSetDefault) template.phraseSets.map {
        if (it.id == set.id) it.copy(defaultEvery = edit.every) else it
    } else template.phraseSets
    val changedCells = template.cells.map {
        if (it.cellId == edit.cellId) {
            it.copy(everyOverride = if (edit.useSetDefault) null else edit.every)
        } else it
    }
    return template.copy(phraseSets = changedSets, cells = changedCells)
}

/** Only newly-invalid cells require the explicit user-controlled resolution dialog. */
internal fun quickPhrasePeriodConflicts(
    original: TableTemplateState,
    proposed: TableTemplateState,
): List<QuickPhrasePeriodConflict> {
    val oldSets = original.phraseSets.associateBy { it.id }
    val newSets = proposed.phraseSets.associateBy { it.id }
    val oldCells = original.cells.associateBy { it.cellId }
    return proposed.cells.mapNotNull { cell ->
        if (cell.dataType != TableCellDataType.ROTATING_TEXT) return@mapNotNull null
        val newSet = newSets[cell.phraseSetId]?.takeIf { it.items.isNotEmpty() } ?: return@mapNotNull null
        val old = oldCells[cell.cellId] ?: return@mapNotNull null
        val oldSet = oldSets[old.phraseSetId]?.takeIf { it.items.isNotEmpty() } ?: return@mapNotNull null
        val newEvery = currentEvery(cell, newSet)
        if (newEvery == currentEvery(old, oldSet) || cell.phraseCompletedShots < newEvery) return@mapNotNull null
        QuickPhrasePeriodConflict(
            cellId = cell.cellId,
            row = cell.rowIndex + 1,
            column = cell.colIndex + 1,
            phraseIndex = cell.phraseIndex.coerceIn(0, newSet.items.lastIndex),
            completed = cell.phraseCompletedShots,
            newEvery = newEvery,
        )
    }
}

/** All conflicts must be explicitly resolved before any period change is committed. */
internal fun applyQuickPhrasePeriodEdit(
    original: TableTemplateState,
    edit: QuickPhrasePeriodEdit,
    resolutions: Map<String, CellPhraseProgress> = emptyMap(),
): TableTemplateState? {
    val proposed = previewQuickPhrasePeriodEdit(original, edit) ?: return null
    return applyPhrasePeriodResolutions(proposed, quickPhrasePeriodConflicts(original, proposed), resolutions)
}

/** Applies explicit per-cell choices to either a set edit or a cell-local edit. */
internal fun applyPhrasePeriodResolutions(
    proposed: TableTemplateState,
    conflicts: List<QuickPhrasePeriodConflict>,
    resolutions: Map<String, CellPhraseProgress>,
): TableTemplateState? {
    if (conflicts.any { it.cellId !in resolutions }) return null
    val sets = proposed.phraseSets.associateBy { it.id }
    val conflictIds = conflicts.map { it.cellId }.toSet()
    val updated = proposed.cells.map { cell ->
        val choice = resolutions[cell.cellId] ?: return@map cell
        if (cell.cellId !in conflictIds) return null
        val set = sets[cell.phraseSetId] ?: return null
        if (choice.index !in set.items.indices) return null
        val cycle = (cell.everyOverride ?: set.everyForItem(choice.index)).coerceAtLeast(1)
        if (choice.completedShots !in 0 until cycle) return null
        cell.copy(phraseIndex = choice.index, phraseCompletedShots = choice.completedShots)
    }
    return proposed.copy(cells = updated)
}

/** A phrase selection and its completed count must be saved as one state change. */
internal fun updateQuickPhraseProgress(
    template: TableTemplateState,
    cellId: String,
    index: Int,
    completed: Int,
): TableTemplateState {
    val cell = template.cells.firstOrNull {
        it.cellId == cellId && it.dataType == TableCellDataType.ROTATING_TEXT
    } ?: return template
    val set = template.phraseSets.firstOrNull { it.id == cell.phraseSetId } ?: return template
    if (index !in set.items.indices) return template
    val every = (cell.everyOverride ?: set.everyForItem(index)).coerceAtLeast(1)
    if (completed !in 0 until every) return template
    if (cell.phraseIndex == index && cell.phraseCompletedShots == completed) return template
    return template.copy(cells = template.cells.map {
        if (it.cellId == cellId) it.copy(phraseIndex = index, phraseCompletedShots = completed) else it
    })
}
