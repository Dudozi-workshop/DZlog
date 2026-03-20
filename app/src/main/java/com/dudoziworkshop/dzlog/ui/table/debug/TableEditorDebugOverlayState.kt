package com.dudoziworkshop.dzlog.ui.table.debug

import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.feature.table.editor.TableSelectionRange
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureDeletionDebugInfo
import com.dudoziworkshop.dzlog.feature.table.editor.coordinator.TableEditorStructureRestoreDebugInfo
import com.dudoziworkshop.dzlog.ui.table.section.BottomEditorPanelMode

data class TableEditorDebugOverlaySource(
    val lastAction: String,
    val currentMode: String,
    val bottomPanelMode: BottomEditorPanelMode,
    val rows: Int,
    val cols: Int,
    val selectedCellId: String?,
    val selectedCell: TableCellState?,
    val selectionRange: TableSelectionRange?,
    val deletedRowsStackSize: Int,
    val deletedColsStackSize: Int,
    val latestDeletionDebugInfo: TableEditorStructureDeletionDebugInfo?,
    val latestRestoreDebugInfo: TableEditorStructureRestoreDebugInfo?,
    val fileNameSlotsDirty: Boolean,
    val pathSlotsDirty: Boolean,
    val undoSummary: String,
)

data class TableEditorDebugOverlayState(
    val lastAction: String,
    val currentMode: String,
    val bottomPanelMode: String,
    val rows: Int,
    val cols: Int,
    val selectedCellId: String,
    val selectedRowCol: String,
    val selectionSummary: String,
    val deletedSnapshotSummary: String,
    val restoredPayloadSummary: String,
    val reindexedPayloadSummary: String,
    val mergedAxisSummary: String,
    val sanitizedAxisSummary: String,
    val restoreAxis: String,
    val restoreTargetIndex: String,
    val hasSlotSnapshot: Boolean,
    val fileNameSlotsDirty: Boolean,
    val pathSlotsDirty: Boolean,
    val undoSummary: String,
    val deletedStacksSummary: String,
)
