package com.dudoziworkshop.dzlog.ui.table.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

@Composable
fun TableEditorDebugOverlay(
    visible: Boolean,
    state: TableEditorDebugOverlayState,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SmallFloatingActionButton(onClick = onToggle) {
                Text(if (visible) "DBG-" else "DBG+")
            }

            if (visible) {
                Card(
                    modifier = Modifier
                        .widthIn(max = 360.dp)
                        .fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Table Debug Overlay", style = MaterialTheme.typography.titleSmall)
                        DebugLine("lastAction", state.lastAction)
                        DebugLine("mode", state.currentMode)
                        DebugLine("bottomPanel", state.bottomPanelMode)
                        DebugLine("rows/cols", "${state.rows} / ${state.cols}")
                        DebugLine("selectedCellId", state.selectedCellId)
                        DebugLine("selectedRowCol", state.selectedRowCol)
                        DebugLine("selection", state.selectionSummary)
                        DebugLine("deletedStacks", state.deletedStacksSummary)
                        DebugLine("restoreAxis", state.restoreAxis)
                        DebugLine("targetIndex", state.restoreTargetIndex)
                        DebugLine("hasSlotSnapshot", state.hasSlotSnapshot.toString())
                        DebugLine("fileDirty", state.fileNameSlotsDirty.toString())
                        DebugLine("pathDirty", state.pathSlotsDirty.toString())
                        DebugLine("undo", state.undoSummary)
                        DebugBlock("deleted", state.deletedSnapshotSummary)
                        DebugBlock("restored", state.restoredPayloadSummary)
                        DebugBlock("reindexed", state.reindexedPayloadSummary)
                        DebugBlock("merged", state.mergedAxisSummary)
                        DebugBlock("sanitized", state.sanitizedAxisSummary)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            OutlinedButton(onClick = onToggle) {
                                Text("Hide")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun DebugBlock(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}
