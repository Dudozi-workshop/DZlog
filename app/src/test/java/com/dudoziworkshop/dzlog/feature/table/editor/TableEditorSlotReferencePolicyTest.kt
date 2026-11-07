package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import org.junit.Assert.assertEquals
import org.junit.Test

class TableEditorSlotReferencePolicyTest {
    @Test
    fun deleted_cell_references_are_compacted_without_dropping_unrelated_slots() {
        val deleted = TableEditorSlotDraft(kind = "CELL", label = "removed", cellId = "deleted")
        val kept = TableEditorSlotDraft(kind = "CELL", label = "kept", cellId = "kept")
        val manual = TableEditorSlotDraft(kind = "MANUAL", label = "constant", manualText = "constant")

        assertEquals(
            listOf(kept, manual, null, null),
            TableEditorSlotReferencePolicy.removeDeletedCellRefs(
                slots = listOf(deleted, null, kept, manual),
                deletedCellIds = setOf("deleted"),
                size = 4,
            ),
        )
    }

    @Test
    fun empty_and_unreferenced_slots_follow_existing_normalization() {
        val slot = TableEditorSlotDraft(kind = "CELL", label = "live", cellId = "live")
        assertEquals(
            listOf(slot, null, null),
            TableEditorSlotReferencePolicy.removeDeletedCellRefs(
                slots = listOf(null, slot),
                deletedCellIds = emptySet(),
                size = 3,
            ),
        )
    }
}
