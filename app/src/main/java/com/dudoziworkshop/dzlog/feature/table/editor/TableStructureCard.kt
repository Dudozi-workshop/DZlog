package com.dudoziworkshop.dzlog.feature.table.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TableStructureCard(
    isStructureMode: Boolean,
    canUndo: Boolean,
    onToggleMode: () -> Unit,
    onAddRow: () -> Unit,
    onAddColumn: () -> Unit,
    onRemoveRow: () -> Unit,
    onRemoveColumn: () -> Unit,
    onResetWeights: () -> Unit,
    onDistributeEvenly: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("표 구조설정")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onToggleMode) { Text(if (isStructureMode) "기본모드" else "구조모드") }
            Button(onClick = onUndo, enabled = canUndo) { Text("Undo") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onAddRow) { Text("행 추가") }
            Button(onClick = onAddColumn) { Text("열 추가") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onRemoveRow) { Text("행 삭제") }
            Button(onClick = onRemoveColumn) { Text("열 삭제") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onResetWeights) { Text("비율 초기화") }
            Button(onClick = onDistributeEvenly) { Text("균등 분배") }
        }
    }
}
