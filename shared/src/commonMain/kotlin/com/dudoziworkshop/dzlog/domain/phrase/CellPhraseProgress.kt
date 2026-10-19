package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType

/** Stored progress describes the NEXT capture, with completed shots in the current item. */
data class CellPhraseProgress(val index: Int, val completedShots: Int)

object CellPhraseProgressPolicy {
    fun captureUpdates(cells: List<TableCellState>, sets: List<RotatingPhraseSet>): Map<String, CellPhraseProgress> {
        val setsById = sets.associateBy { it.id }
        return cells.filter { cell ->
            cell.dataType == TableCellDataType.ROTATING_TEXT && cells.none { root ->
                root.cellId != cell.cellId &&
                    cell.rowIndex in root.rowIndex until (root.rowIndex + root.rowSpan) &&
                    cell.colIndex in root.colIndex until (root.colIndex + root.colSpan)
            }
        }.mapNotNull { cell ->
            setsById[cell.phraseSetId]?.takeIf { it.items.isNotEmpty() }?.let { set ->
                cell.cellId to afterSuccessfulCapture(cell, set)
            }
        }.toMap()
    }

    fun current(cell: TableCellState, set: RotatingPhraseSet): CellPhraseProgress {
        val every = (cell.everyOverride ?: set.defaultEvery).coerceAtLeast(1)
        return CellPhraseProgress(
            cell.phraseIndex.coerceIn(0, (set.items.size - 1).coerceAtLeast(0)),
            cell.phraseCompletedShots.coerceIn(0, every - 1),
        )
    }

    fun afterSuccessfulCapture(cell: TableCellState, set: RotatingPhraseSet): CellPhraseProgress {
        val current = current(cell, set)
        if (set.items.isEmpty()) return current
        val every = (cell.everyOverride ?: set.defaultEvery).coerceAtLeast(1)
        return if (current.completedShots + 1 >= every) {
            CellPhraseProgress((current.index + 1) % set.items.size, 0)
        } else current.copy(completedShots = current.completedShots + 1)
    }
}
