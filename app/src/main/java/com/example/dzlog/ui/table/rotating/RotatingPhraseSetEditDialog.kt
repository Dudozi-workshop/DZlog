package com.example.dzlog.ui.table.rotating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dzlog.domain.model.RotatingPhraseSet
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

@Composable
fun RotatingPhraseSetEditDialog(
    phraseSet: RotatingPhraseSet,
    onClose: () -> Unit,
    onUpdateSet: ((RotatingPhraseSet) -> RotatingPhraseSet) -> Unit,
    onDeleteSet: (String) -> Unit
) {
    var showRenameDialog by remember(phraseSet.id) { mutableStateOf(false) }
    var renameInput by remember(phraseSet.id) { mutableStateOf(phraseSet.name) }
    var showDeleteConfirm by remember(phraseSet.id) { mutableStateOf(false) }

    var showItemInputDialog by remember(phraseSet.id) { mutableStateOf(false) }
    var editingItemIndex by remember(phraseSet.id) { mutableStateOf<Int?>(null) }
    var itemInput by remember(phraseSet.id) { mutableStateOf("") }
    val itemIds = remember(phraseSet.id) { mutableStateListOf<String>() }

    LaunchedEffect(phraseSet.id, phraseSet.items.size) {
        val targetSize = phraseSet.items.size
        while (itemIds.size < targetSize) {
            itemIds.add(UUID.randomUUID().toString())
        }
        while (itemIds.size > targetSize) {
            itemIds.removeAt(itemIds.lastIndex)
        }

        val idx = editingItemIndex
        if (idx != null && idx !in phraseSet.items.indices) {
            editingItemIndex = null
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onUpdateSet { set ->
            val items = set.items
            if (items.isEmpty()) return@onUpdateSet set

            val fromIndex = from.index
            val toIndex = to.index.coerceIn(0, items.lastIndex)
            if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
                return@onUpdateSet set
            }

            val newList = items.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            set.copy(items = newList)
        }

        if (from.index in itemIds.indices) {
            val idToMove = itemIds.removeAt(from.index)
            val insertIndex = to.index.coerceIn(0, itemIds.size)
            itemIds.add(insertIndex, idToMove)
        }
    }

    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = DDZColor.Card
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onClose) {
                        Text("< 뒤로", style = DDZTypography.ButtonText)
                    }
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("🗑", style = DDZTypography.ButtonText)
                    }
                }

                TextButton(onClick = {
                    renameInput = phraseSet.name
                    showRenameDialog = true
                }) {
                    Text(phraseSet.name, style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 340.dp)
                        .background(DDZColor.Surface, RoundedCornerShape(10.dp))
                        .padding(6.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clipToBounds(),
                        state = lazyListState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(
                            phraseSet.items.size,
                            key = { idx -> itemIds.getOrNull(idx) ?: "${phraseSet.id}-$idx" }
                        ) { index ->
                            val item = phraseSet.items.getOrNull(index) ?: return@items
                            val stableId = itemIds.getOrNull(index) ?: "${phraseSet.id}-$index"

                            ReorderableItem(reorderableState, key = stableId) { isDragging ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isDragging) DDZColor.Card.copy(alpha = 0.92f) else DDZColor.Card,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            editingItemIndex = index
                                            itemInput = item
                                            showItemInputDialog = true
                                        }
                                        .padding(horizontal = 6.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = DDZTypography.Body,
                                        color = DDZColor.TextPrimary
                                    )
                                    Text(
                                        "☰",
                                        modifier = with(this@ReorderableItem) { Modifier.draggableHandle() },
                                        style = DDZTypography.Body,
                                        color = DDZColor.TextMuted
                                    )
                                    TextButton(onClick = {
                                        if (index in itemIds.indices) {
                                            itemIds.removeAt(index)
                                        }
                                        onUpdateSet { set ->
                                            set.copy(items = set.items.filterIndexed { idx, _ -> idx != index })
                                        }
                                    }) { Text("🗑") }
                                }
                            }
                        }
                    }

                    if (phraseSet.items.isEmpty()) {
                        Text(
                            text = "항목을 추가해주세요.",
                            modifier = Modifier.align(Alignment.Center),
                            color = DDZColor.TextMuted,
                            style = DDZTypography.Body
                        )
                    }

                    RotatingScrollIndicator(
                        listState = lazyListState,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        editingItemIndex = null
                        itemInput = ""
                        showItemInputDialog = true
                    }
                ) {
                    Text("+ 문구 추가", style = DDZTypography.ButtonText)
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("세트 이름 변경") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    label = { Text("세트 이름") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = renameInput.trim()
                    if (trimmed.isNotBlank()) {
                        onUpdateSet { it.copy(name = trimmed) }
                    }
                    showRenameDialog = false
                }) {
                    Text("적용", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (showItemInputDialog) {
        val isEdit = editingItemIndex != null
        AlertDialog(
            onDismissRequest = { showItemInputDialog = false },
            title = { Text(if (isEdit) "문구 수정" else "문구 추가") },
            text = {
                OutlinedTextField(
                    value = itemInput,
                    onValueChange = { itemInput = it },
                    singleLine = true,
                    label = { Text("문구") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = itemInput.trim()
                    if (trimmed.isNotBlank()) {
                        if (editingItemIndex == null) {
                            itemIds.add(UUID.randomUUID().toString())
                            onUpdateSet { it.copy(items = it.items + trimmed) }
                        } else {
                            val idx = editingItemIndex!!
                            onUpdateSet { set ->
                                if (idx !in set.items.indices) {
                                    set
                                } else {
                                    set.copy(
                                        items = set.items.mapIndexed { i, value ->
                                            if (i == idx) trimmed else value
                                        }
                                    )
                                }
                            }
                        }
                    }
                    showItemInputDialog = false
                }) {
                    Text("적용", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showItemInputDialog = false }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("세트 삭제") },
            text = { Text("${phraseSet.name} 세트를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSet(phraseSet.id)
                    showDeleteConfirm = false
                }) {
                    Text("삭제", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }
}
