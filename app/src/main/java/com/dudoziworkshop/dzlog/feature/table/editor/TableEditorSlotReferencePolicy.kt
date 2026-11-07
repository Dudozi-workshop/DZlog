package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft

/** Shared structure-edit rule: compact filename/path slots after cells are deleted. */
internal object TableEditorSlotReferencePolicy {
    fun removeDeletedCellRefs(
        slots: List<TableEditorSlotDraft?>,
        deletedCellIds: Set<String>,
        size: Int,
    ): List<TableEditorSlotDraft?> {
        val retained = slots.filterNotNull().filter { slot ->
            slot.cellId == null || slot.cellId !in deletedCellIds
        }
        return List(size) { index -> retained.getOrNull(index) }
    }
}
