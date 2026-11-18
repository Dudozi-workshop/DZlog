package com.dudoziworkshop.dzlog.domain.phrase

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState

/**
 * A camera-only preview cursor. This never changes stored template progress or
 * MediaStore, and never pretends that a photo was saved.
 *
 * A negative offset walks backwards through per-phrase intervals. The current
 * value describes the NEXT exposure; it is not the number of saved photos.
 */
internal object ManualCaptureStepPolicy {
    fun adjustNextCounter(next: Int, offset: Int): Int =
        (next.toLong() + offset.toLong()).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()

    fun canStepBack(next: Int, offset: Int): Boolean =
        next.toLong() + offset.toLong() > 1L

    fun offsetTemplate(template: TableTemplateState, steps: Int): TableTemplateState {
        if (steps == 0) return template
        val sets = template.phraseSets.associateBy { it.id }
        val roots = template.cells.filter { cell ->
            cell.dataType == TableCellDataType.ROTATING_TEXT &&
                template.cells.none { root ->
                    root.cellId != cell.cellId &&
                        cell.rowIndex in root.rowIndex until (root.rowIndex + root.rowSpan) &&
                        cell.colIndex in root.colIndex until (root.colIndex + root.colSpan)
                }
        }.map { it.cellId }.toSet()
        var changed = false
        val nextCells = template.cells.map { cell ->
            val set = sets[cell.phraseSetId]
            if (cell.cellId !in roots || set == null || set.items.isEmpty()) cell
            else {
                val adjusted = move(cell, set, steps)
                if (adjusted != cell) changed = true
                adjusted
            }
        }
        return if (changed) template.copy(cells = nextCells) else template
    }

    private fun move(
        cell: TableCellState,
        set: RotatingPhraseSet,
        steps: Int,
    ): TableCellState {
        val sizes = set.items.indices.map { index ->
            CellPhraseProgressPolicy.everyFor(cell, set, index).toLong()
        }
        val cycle = sizes.sum().coerceAtLeast(1L)
        val current = CellPhraseProgressPolicy.current(cell, set)
        val position = sizes.take(current.index).sum() + current.completedShots
        val normalized = Math.floorMod(position + steps.toLong(), cycle)
        var remainder = normalized
        sizes.forEachIndexed { index, length ->
            if (remainder < length) {
                return cell.copy(
                    phraseIndex = index,
                    phraseCompletedShots = remainder.toInt(),
                )
            }
            remainder -= length
        }
        return cell // unreachable with a valid non-empty set
    }
}
