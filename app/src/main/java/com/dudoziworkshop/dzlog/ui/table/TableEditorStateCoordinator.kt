package com.dudoziworkshop.dzlog.ui.table

import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode
import com.dudoziworkshop.dzlog.feature.table.state.TableDetailScreenState
import com.dudoziworkshop.dzlog.ui.table.detail.TableDetailAction

internal data class TableEditorDetailBridge(
    val detailState: TableDetailScreenState,
    val dispatch: (TableDetailAction) -> TableTemplateState,
    val syncTemplate: (TableTemplateState, Boolean) -> Unit,
)

internal fun TableEditorDetailBridge.isStructureMode(): Boolean =
    detailState.editMode == TableEditMode.Structure

internal fun TableEditorDetailBridge.selectionRangeOrNull(): TableSelectionRange? =
    detailState.selection.minRow?.let { minRow ->
        TableSelectionRange(
            minRow = minRow,
            maxRow = detailState.selection.maxRow ?: minRow,
            minCol = detailState.selection.minCol ?: 0,
            maxCol = detailState.selection.maxCol ?: 0,
        )
    }
