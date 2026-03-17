package com.dudoziworkshop.dzlog.feature.table.state

import com.dudoziworkshop.dzlog.feature.table.model.TableDefinitionState
import com.dudoziworkshop.dzlog.feature.table.model.TableEditMode
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableSelectionState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState

data class TableDetailScreenState(
    val definition: TableDefinitionState,
    val style: TableStyleState,
    val placement: TablePlacementState,
    val editMode: TableEditMode = TableEditMode.Normal,
    val selection: TableSelectionState = TableSelectionState(),
    val canUndo: Boolean = false,
)
