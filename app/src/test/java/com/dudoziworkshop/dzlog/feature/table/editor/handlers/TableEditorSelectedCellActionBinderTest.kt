package com.dudoziworkshop.dzlog.feature.table.editor.handlers

import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.CounterScopeMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.ui.table.PathGroupAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TableEditorSelectedCellActionBinderTest {

    @Test
    fun set_data_type_for_selected_updates_selected_cell_only() {
        val template = defaultTableTemplateState()
        val selected = template.cells.first()
        var updatedTemplate: TableTemplateState? = null

        TableEditorSelectedCellActionBinder.setDataTypeForSelected(
            bindings(template, selected) { updatedTemplate = it },
            TableCellDataType.NUMBER,
        )

        assertEquals(TableCellDataType.NUMBER, updatedTemplate?.cells?.first { it.cellId == selected.cellId }?.dataType)
    }

    @Test
    fun set_counter_scope_mode_ignores_non_date_time_cells() {
        val template = defaultTableTemplateState()
        val selected = template.cells.first().copy(dataType = TableCellDataType.TEXT)
        var updated = false

        TableEditorSelectedCellActionBinder.setCounterScopeModeForSelected(
            bindings(template.copy(cells = listOf(selected)), selected) { updated = true },
            CounterScopeMode.INCLUDE,
        )

        assertFalse(updated)
    }

    @Test
    fun apply_path_group_action_updates_template() {
        val template = defaultTableTemplateState()
        val selected = template.cells.first()
        var updatedTemplate: TableTemplateState? = null

        TableEditorSelectedCellActionBinder.applyPathGroupActionForSelected(
            bindings(template, selected) { updatedTemplate = it },
            PathGroupAction.G1,
        )

        assertEquals(selected.cellId, updatedTemplate?.cells?.first { it.groupLevel.name == "G1" }?.cellId)
    }

    @Test
    fun rotating_template_dialog_opens_only_for_rotating_cells() {
        val template = defaultTableTemplateState()
        val selected = template.cells.first().copy(dataType = TableCellDataType.ROTATING_TEXT)
        var openedCellId: String? = null
        var showPanel = false

        TableEditorSelectedCellActionBinder.openRotatingTemplateDialogForSelected(
            TableEditorSelectedCellActionBindings(
                currentTemplate = { template.copy(cells = listOf(selected)) },
                currentSelectedCell = { selected },
                updateTemplateDraft = {},
                showCellSettingsPanel = { showPanel = it },
                openRotatingPhraseTemplateDialog = { openedCellId = it },
            ),
            selected.cellId,
        )

        assertEquals(selected.cellId, openedCellId)
        assertFalse(showPanel)
    }

    private fun bindings(
        template: TableTemplateState,
        selectedCell: TableCellState,
        updateTemplateDraft: (TableTemplateState) -> Unit,
    ): TableEditorSelectedCellActionBindings {
        return TableEditorSelectedCellActionBindings(
            currentTemplate = { template },
            currentSelectedCell = { selectedCell },
            updateTemplateDraft = updateTemplateDraft,
            showCellSettingsPanel = {},
            openRotatingPhraseTemplateDialog = {},
        )
    }
}
