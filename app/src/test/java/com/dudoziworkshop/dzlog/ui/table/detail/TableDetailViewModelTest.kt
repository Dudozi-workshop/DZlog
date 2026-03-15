package com.dudoziworkshop.dzlog.ui.table.detail

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode
import org.junit.Assert.assertEquals
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
    }
}
