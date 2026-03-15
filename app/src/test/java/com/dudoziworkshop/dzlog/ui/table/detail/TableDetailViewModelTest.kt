package com.dudoziworkshop.dzlog.ui.table.detail

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.addRow
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableDetailViewModelTest {

    @Test
    fun structure_mode_selection_controls_row_insertion_point() {
        val vm = TableDetailViewModel(TableTemplateState.default)
        vm.dispatch(TableDetailAction.ToggleStructureMode)
        vm.dispatch(TableDetailAction.InjectSelectionRangeForTest(TableSelectionRange(0, 0, 0, 0)))

        val updated = vm.dispatch(TableDetailAction.AddRow)

        assertEquals(TableEditMode.Structure, vm.state.editMode)
        assertEquals(2, updated.rows)
        assertEquals(0, vm.state.selection.minRow)
        assertEquals(0, vm.state.selection.maxRow)
    }

    @Test
    fun delete_clears_selection_and_style_change_can_undo() {
        val vm = TableDetailViewModel(addRow(TableTemplateState.default))
        vm.dispatch(TableDetailAction.ToggleStructureMode)
        vm.dispatch(TableDetailAction.InjectSelectionRangeForTest(TableSelectionRange(0, 1, 0, 0)))
        vm.dispatch(TableDetailAction.RemoveRow)
        assertEquals(null, vm.state.selection.minRow)

        vm.dispatch(TableDetailAction.SetStyleBgStyle(2))
        assertTrue(vm.canUndo())
        vm.dispatch(TableDetailAction.Undo)
        assertEquals(0, vm.state.style.bgStyle)
    }
}
