package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.DeletedStructureSnapshot
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.StructureRestoreAxis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorStructureActionsTest {

    @Test
    fun resolve_deletion_range_uses_last_row_fallback_when_structure_selection_is_null() {
        val template = addRow(defaultTableTemplateState())

        val range = TableEditorStructureActions.resolveDeletionRange(
            axis = StructureRestoreAxis.ROW,
            isStructureEditMode = true,
            structureSelectionRange = null,
            currentTemplate = template,
        )

        assertEquals((template.rows - 1)..(template.rows - 1), range)
    }

    @Test
    fun add_or_restore_with_row_stack_returns_restore_result() {
        val base = defaultTableTemplateState()
        val deletedTemplate = addRow(base)
        val deletedSnapshot = DeletedStructureSnapshot(
            cells = deletedTemplate.cells.filter { it.rowIndex == deletedTemplate.rows - 1 },
        )

        val result: StructureActionResult = TableEditorStructureActions.addOrRestore(
            addOrRestoreInput(
                axis = StructureRestoreAxis.ROW,
                currentTemplate = base,
                deletedRowsStack = listOf(deletedSnapshot),
            )
        )

        assertEquals("add_row_restore", result.actionLabel)
        assertEquals(StructureRestoreAxis.ROW, result.consumedDeletedStackAxis)
        assertNull(result.deletedSnapshotToPush)
        assertNull(result.deletionDebugInfo)
        assertNotNull(result.restoreDebugInfo)
        assertEquals(base.rows + 1, result.nextTemplate.rows)
    }

    @Test
    fun add_or_restore_with_col_stack_returns_restore_result() {
        val base = defaultTableTemplateState()
        val deletedTemplate = addColumn(base)
        val deletedSnapshot = DeletedStructureSnapshot(
            cells = deletedTemplate.cells.filter { it.colIndex == deletedTemplate.cols - 1 },
        )

        val result: StructureActionResult = TableEditorStructureActions.addOrRestore(
            addOrRestoreInput(
                axis = StructureRestoreAxis.COL,
                currentTemplate = base,
                deletedColsStack = listOf(deletedSnapshot),
            )
        )

        assertEquals("add_col_restore", result.actionLabel)
        assertEquals(StructureRestoreAxis.COL, result.consumedDeletedStackAxis)
        assertNull(result.deletedSnapshotToPush)
        assertNull(result.deletionDebugInfo)
        assertNotNull(result.restoreDebugInfo)
        assertEquals(base.cols + 1, result.nextTemplate.cols)
    }

    @Test
    fun add_or_restore_without_stack_returns_blank_add_result() {
        val base = defaultTableTemplateState()

        val result: StructureActionResult = TableEditorStructureActions.addOrRestore(
            addOrRestoreInput(
                axis = StructureRestoreAxis.ROW,
                currentTemplate = base,
            )
        )

        assertEquals("add_row_blank", result.actionLabel)
        assertNull(result.consumedDeletedStackAxis)
        assertNull(result.deletedSnapshotToPush)
        assertNull(result.deletionDebugInfo)
        assertNull(result.restoreDebugInfo)
        assertEquals(base.rows + 1, result.nextTemplate.rows)
    }

    @Test
    fun add_or_restore_clamps_structure_selection_in_structure_mode() {
        val base = defaultTableTemplateState()
        val selectionRange = TableSelectionRange(
            minRow = 0,
            maxRow = 4,
            minCol = 0,
            maxCol = 10,
        )

        val result = TableEditorStructureActions.addOrRestore(
            addOrRestoreInput(
                axis = StructureRestoreAxis.ROW,
                isStructureEditMode = true,
                structureSelectionRange = selectionRange,
                currentTemplate = base,
            )
        )

        assertEquals(
            TableSelectionRange(
                minRow = 0,
                maxRow = result.nextTemplate.rows - 1,
                minCol = 0,
                maxCol = result.nextTemplate.cols - 1,
            ),
            result.nextStructureSelectionRange,
        )
        assertEquals(
            result.nextTemplate.cells
                .filter { result.nextStructureSelectionRange?.contains(it.rowIndex, it.colIndex) == true }
                .map { it.cellId }
                .toSet(),
            result.nextStructureSelectedCellIds,
        )
        assertNull(result.nextSelectedCellId)
    }

    @Test
    fun remove_uses_common_result_type_and_pushes_deleted_snapshot() {
        val template = addRow(defaultTableTemplateState())

        val result: StructureActionResult = TableEditorStructureActions.remove(
            removeInput(
                axis = StructureRestoreAxis.ROW,
                isStructureEditMode = true,
                currentTemplate = template,
            )
        )

        assertEquals("remove_row", result.actionLabel)
        assertEquals(StructureRestoreAxis.ROW, result.deletedSnapshotAxis)
        assertNotNull(result.deletedSnapshotToPush)
        assertNotNull(result.deletionDebugInfo)
        assertNull(result.restoreDebugInfo)
        assertNull(result.nextStructureSelectionRange)
        assertTrue(result.nextStructureSelectedCellIds.isEmpty())
        assertNull(result.nextSelectedCellId)
    }

    @Test
    fun remove_clears_structure_selection_but_keeps_normal_selected_cell_when_it_survives() {
        val template = addRow(defaultTableTemplateState())
        val selectedCellId = template.cells.first { it.rowIndex == 0 && it.colIndex == 0 }.cellId

        val result = TableEditorStructureActions.remove(
            removeInput(
                axis = StructureRestoreAxis.ROW,
                isStructureEditMode = false,
                structureSelectionRange = TableSelectionRange(
                    minRow = 0,
                    maxRow = 1,
                    minCol = 0,
                    maxCol = 1,
                ),
                currentTemplate = template,
                selectedCellId = selectedCellId,
            )
        )

        assertNull(result.nextStructureSelectionRange)
        assertTrue(result.nextStructureSelectedCellIds.isEmpty())
        assertEquals(selectedCellId, result.nextSelectedCellId)
    }

    @Test
    fun remove_uses_normalized_range_for_payload_and_mutation() {
        val template = defaultTableTemplateState()

        val result = TableEditorStructureActions.remove(
            removeInput(
                axis = StructureRestoreAxis.ROW,
                isStructureEditMode = true,
                structureSelectionRange = TableSelectionRange(
                    minRow = 0,
                    maxRow = template.rows - 1,
                    minCol = 0,
                    maxCol = template.cols - 1,
                ),
                currentTemplate = template,
            )
        )

        assertEquals(1, result.nextTemplate.rows)
        assertEquals(0..0, result.deletionDebugInfo?.deletedRange)
        assertEquals(template.cols, result.deletedSnapshotToPush?.cells?.size)
    }

    private fun addOrRestoreInput(
        axis: StructureRestoreAxis,
        isStructureEditMode: Boolean = false,
        structureSelectionRange: TableSelectionRange? = null,
        currentTemplate: TableTemplateState = defaultTableTemplateState(),
        deletedRowsStack: List<DeletedStructureSnapshot> = emptyList(),
        deletedColsStack: List<DeletedStructureSnapshot> = emptyList(),
    ): TableEditorStructureAddOrRestoreActionInput {
        return TableEditorStructureAddOrRestoreActionInput(
            axis = axis,
            isStructureEditMode = isStructureEditMode,
            structureSelectionRange = structureSelectionRange,
            selectedCellId = currentTemplate.cells.firstOrNull()?.cellId,
            currentTemplate = currentTemplate,
            deletedRowsStack = deletedRowsStack,
            deletedColsStack = deletedColsStack,
            currentFileNameSlots = emptyList(),
            currentPathSlots = emptyList(),
            fileNameSlotsDirtySinceStructureChange = false,
            pathSlotsDirtySinceStructureChange = false,
            sanitizeTemplate = { it },
            applyFileNameSlots = { template, _ -> template },
            applyPathSlots = { template, _ -> template },
        )
    }

    private fun removeInput(
        axis: StructureRestoreAxis,
        isStructureEditMode: Boolean = false,
        structureSelectionRange: TableSelectionRange? = null,
        currentTemplate: TableTemplateState = defaultTableTemplateState(),
        selectedCellId: String? = currentTemplate.cells.firstOrNull()?.cellId,
    ): TableEditorStructureRemoveActionInput {
        return TableEditorStructureRemoveActionInput(
            axis = axis,
            isStructureEditMode = isStructureEditMode,
            structureSelectionRange = structureSelectionRange,
            selectedCellId = selectedCellId,
            currentTemplate = currentTemplate,
            currentFileNameSlots = emptyList(),
            currentPathSlots = emptyList(),
            fileNameSlotsDirtySinceStructureChange = false,
            pathSlotsDirtySinceStructureChange = false,
            sanitizeTemplate = { it },
            applyFileNameSlots = { template, _ -> template },
            applyPathSlots = { template, _ -> template },
            removeCellRefsFromFileNameSlots = { slots, _ -> slots },
            removeCellRefsFromPathSlots = { slots, _ -> slots },
        )
    }
}
