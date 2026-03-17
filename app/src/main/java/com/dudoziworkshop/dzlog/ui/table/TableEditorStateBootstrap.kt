package com.dudoziworkshop.dzlog.ui.table

import android.content.Context
import com.dudoziworkshop.dzlog.feature.table.model.TablePlacementState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.placement.loadTablePlacementState
import com.dudoziworkshop.dzlog.feature.table.state.loadTableStyleState

internal data class TableEditorBootstrapState(
    val placement: TablePlacementState,
    val style: TableStyleState,
)

internal fun loadTableEditorBootstrapState(context: Context): Result<TableEditorBootstrapState> = runCatching {
    TableEditorBootstrapState(
        placement = loadTablePlacementState(context),
        style = loadTableStyleState(context),
    )
}
