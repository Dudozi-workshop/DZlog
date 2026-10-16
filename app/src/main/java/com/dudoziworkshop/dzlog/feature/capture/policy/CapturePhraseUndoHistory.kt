package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress

/**
 * In-session undo journal. A capture may own several MediaStore outputs, but its
 * phrase progress must roll back exactly once after ALL outputs are deleted.
 * The journal never changes photographs or counter state.
 */
class CapturePhraseUndoHistory {
    data class Entry(
        val templateId: String,
        val initialUris: Set<String>,
        val before: Map<String, CellPhraseProgress>,
        val after: Map<String, CellPhraseProgress>,
        val phraseSetIds: Map<String, String?>,
    )

    private class Tracked(val entry: Entry) {
        val remainingUris = entry.initialUris.toMutableSet()
    }

    private val entries = mutableListOf<Tracked>()
    private val galleryChangedUris = mutableSetOf<String>()

    /**
     * Gallery delete/move is not a camera Undo: it must not roll back phrase
     * progress. Fence all captures committed before that edit; a fresh shutter
     * records new URIs and remains individually undoable.
     */
    @Synchronized
    fun invalidateUndoForExistingCaptures(uris: Collection<String>) {
        galleryChangedUris.addAll(uris.filter(String::isNotBlank))
    }

    @Synchronized
    fun record(
        templateId: String?,
        uris: Collection<String>,
        before: TableTemplateState,
        after: TableTemplateState,
    ) {
        if (templateId.isNullOrBlank()) return
        val normalized = uris.filter { it.isNotBlank() }.toSet()
        if (normalized.isEmpty()) return
        val beforeCells = before.cells.associateBy { it.cellId }
        val afterCells = after.cells.associateBy { it.cellId }
        val keys = afterCells.keys.filter { id ->
            afterCells[id]?.dataType == TableCellDataType.ROTATING_TEXT &&
                beforeCells[id]?.dataType == TableCellDataType.ROTATING_TEXT
        }
        fun states(cells: Map<String, com.dudoziworkshop.dzlog.domain.model.TableCellState>) =
            keys.associateWith { id ->
                val cell = requireNotNull(cells[id])
                CellPhraseProgress(cell.phraseIndex, cell.phraseCompletedShots)
            }

        entries.removeAll { it.entry.initialUris.any(normalized::contains) }
        entries.add(Tracked(Entry(
            templateId = templateId,
            initialUris = normalized,
            before = states(beforeCells),
            after = states(afterCells),
            phraseSetIds = keys.associateWith { afterCells[it]?.phraseSetId },
        )))
        // A camera session retains at most a bounded number of undoable shots.
        if (entries.size > 256) entries.removeAt(0)
    }

    @Synchronized
    fun canUndo(uris: Collection<String>, activeTemplateId: String?): Boolean {
        val keys = uris.toSet()
        if (keys.any(galleryChangedUris::contains)) return false
        val tracked = entries.lastOrNull { item -> item.remainingUris.any(keys::contains) }
        return tracked == null || tracked.entry.templateId == activeTemplateId
    }

    /**
     * Called for confirmed deleted URIs. Partial deletion keeps the original
     * phrase progress and the record until every output has been removed.
     */
    @Synchronized
    fun deleted(uris: Collection<String>): Entry? {
        val deleted = uris.toSet()
        val tracked = entries.lastOrNull { item -> item.remainingUris.any(deleted::contains) }
            ?: return null
        tracked.remainingUris.removeAll(deleted)
        return tracked.entry.takeIf { tracked.remainingUris.isEmpty() }
    }

    /** A partially deleted capture cannot safely advance to another shutter. */
    @Synchronized
    fun hasIncompleteDeletion(): Boolean =
        entries.any { it.remainingUris.size < it.entry.initialUris.size && it.remainingUris.isNotEmpty() }

    /** Restore only phrase cells; do not overwrite the latest filename/counter/template settings. */
    fun restore(current: TableTemplateState, entry: Entry): TableTemplateState? {
        val cellMap = current.cells.associateBy { it.cellId }
        if (entry.after.any { (id, expected) ->
                val cell = cellMap[id] ?: return@any true
                cell.dataType != TableCellDataType.ROTATING_TEXT ||
                    cell.phraseSetId != entry.phraseSetIds[id] ||
                    cell.phraseIndex != expected.index ||
                    cell.phraseCompletedShots != expected.completedShots
            }
        ) return null
        return current.copy(cells = current.cells.map { cell ->
            val previous = entry.before[cell.cellId]
            if (previous == null) cell else cell.copy(
                phraseIndex = previous.index,
                phraseCompletedShots = previous.completedShots,
            )
        })
    }

    @Synchronized
    fun acknowledgeRestored(entry: Entry) {
        entries.removeAll { it.entry === entry }
    }

    @Synchronized
    fun clear() {
        entries.clear()
        galleryChangedUris.clear()
    }
}
