package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.ui.table.section.FileNameSlotUiItem
import com.dudoziworkshop.dzlog.ui.table.section.PathSlotUiItem

data class StructureSlotSnapshot(
    val fileNameSlots: List<FileNameSlotUiItem?>,
    val pathSlots: List<PathSlotUiItem?>,
)

data class StructureSlotDeletionResult(
    val snapshotToStore: StructureSlotSnapshot?,
    val nextFileNameSlots: List<FileNameSlotUiItem?>,
    val nextPathSlots: List<PathSlotUiItem?>,
    val nextFileNameSlotsDirtySinceStructureChange: Boolean,
    val nextPathSlotsDirtySinceStructureChange: Boolean,
)

data class StructureSlotRestoreResult(
    val nextFileNameSlots: List<FileNameSlotUiItem?>,
    val nextPathSlots: List<PathSlotUiItem?>,
    val restoredFileNameSlotsSnapshot: Boolean,
    val restoredPathSlotsSnapshot: Boolean,
    val nextFileNameSlotsDirtySinceStructureChange: Boolean,
    val nextPathSlotsDirtySinceStructureChange: Boolean,
)

object TableEditorStructureSlotHandlers {
    fun handleStructureDeletion(
        deletedCellIds: Set<String>,
        currentFileNameSlots: List<FileNameSlotUiItem?>,
        currentPathSlots: List<PathSlotUiItem?>,
        removeCellRefsFromFileNameSlots: (List<FileNameSlotUiItem?>, Set<String>) -> List<FileNameSlotUiItem?>,
        removeCellRefsFromPathSlots: (List<PathSlotUiItem?>, Set<String>) -> List<PathSlotUiItem?>,
    ): StructureSlotDeletionResult {
        val snapshotToStore = deletedCellIds.takeIf { it.isNotEmpty() }?.let {
            StructureSlotSnapshot(
                fileNameSlots = currentFileNameSlots,
                pathSlots = currentPathSlots,
            )
        }

        return StructureSlotDeletionResult(
            snapshotToStore = snapshotToStore,
            nextFileNameSlots = removeCellRefsFromFileNameSlots(currentFileNameSlots, deletedCellIds),
            nextPathSlots = removeCellRefsFromPathSlots(currentPathSlots, deletedCellIds),
            nextFileNameSlotsDirtySinceStructureChange = false,
            nextPathSlotsDirtySinceStructureChange = false,
        )
    }

    fun handleStructureRestore(
        restoredSnapshot: StructureSlotSnapshot,
        currentFileNameSlots: List<FileNameSlotUiItem?>,
        currentPathSlots: List<PathSlotUiItem?>,
        fileNameSlotsDirtySinceStructureChange: Boolean,
        pathSlotsDirtySinceStructureChange: Boolean,
    ): StructureSlotRestoreResult {
        val shouldRestoreFileNameSlots = !fileNameSlotsDirtySinceStructureChange
        val shouldRestorePathSlots = !pathSlotsDirtySinceStructureChange

        return StructureSlotRestoreResult(
            nextFileNameSlots = if (shouldRestoreFileNameSlots) restoredSnapshot.fileNameSlots else currentFileNameSlots,
            nextPathSlots = if (shouldRestorePathSlots) restoredSnapshot.pathSlots else currentPathSlots,
            restoredFileNameSlotsSnapshot = shouldRestoreFileNameSlots,
            restoredPathSlotsSnapshot = shouldRestorePathSlots,
            nextFileNameSlotsDirtySinceStructureChange = false,
            nextPathSlotsDirtySinceStructureChange = false,
        )
    }
}
