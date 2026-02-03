package com.example.dzlog.ui.log

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    // 선택모드에서 상단 버튼이 겹치는 문제 방지: 상단은 카운트만 표시
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$selectedCount selected")
    }
}

@Composable
fun SelectionBottomBar(
    onClose: () -> Unit,
    onSelectAll: (() -> Unit)? = null,
    onShare: () -> Unit,
    shareEnabled: Boolean = true,
    onDelete: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onClose) { Text("Close") }
            Button(onClick = { onSelectAll?.invoke() }, enabled = onSelectAll != null) { Text("All") }
            Button(onClick = onShare, enabled = shareEnabled) { Text("Share") }
            Button(onClick = { onDelete?.invoke() }, enabled = onDelete != null) { Text("Delete") }
        }
    }
}
