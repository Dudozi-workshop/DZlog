package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.common.DDZTopBarIconButton
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.RotatingCounterProgressMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
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
    var showMenu by remember(phraseSet.id) { mutableStateOf(false) }
    var showRenameDialog by remember(phraseSet.id) { mutableStateOf(false) }
    var renameInput by remember(phraseSet.id) { mutableStateOf(phraseSet.name) }
    var showDeleteConfirm by remember(phraseSet.id) { mutableStateOf(false) }

    var showItemInputDialog by remember(phraseSet.id) { mutableStateOf(false) }
    var editingItemIndex by remember(phraseSet.id) { mutableStateOf<Int?>(null) }
    var itemInput by remember(phraseSet.id) { mutableStateOf("") }
    val itemIds = remember(phraseSet.id) { mutableStateListOf<String>() }

    fun openRenameDialog() {
        renameInput = phraseSet.name
        if (!showRenameDialog) showRenameDialog = true
    }

    fun closeRenameDialog() {
        if (showRenameDialog) showRenameDialog = false
    }

    fun openItemInputDialog(index: Int?, initialText: String) {
        editingItemIndex = index
        itemInput = initialText
        if (!showItemInputDialog) showItemInputDialog = true
    }

    fun closeItemInputDialog() {
        if (showItemInputDialog) showItemInputDialog = false
    }

    fun openDeleteConfirmDialog() {
        if (!showDeleteConfirm) showDeleteConfirm = true
    }

    fun closeDeleteConfirmDialog() {
        if (showDeleteConfirm) showDeleteConfirm = false
    }

    LaunchedEffect(phraseSet.id, phraseSet.items.size) {
        val targetSize = phraseSet.items.size
        while (itemIds.size < targetSize) {
            itemIds.add(UUID.randomUUID().toString())
        }
        while (itemIds.size > targetSize) {
            itemIds.removeAt(itemIds.lastIndex)
        }

        val clampedEditingIndex = editingItemIndex?.takeIf { it in phraseSet.items.indices }
        if (editingItemIndex != clampedEditingIndex) {
            editingItemIndex = clampedEditingIndex
        }
    }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        onUpdateSet { set ->
            val items = set.items
            if (items.isEmpty()) return@onUpdateSet set

            // The first lazy item is the set name; only phrase rows can be reordered.
            val fromIndex = from.index - 1
            val toIndex = to.index - 1
            if (fromIndex !in items.indices || toIndex !in items.indices || fromIndex == toIndex) {
                return@onUpdateSet set
            }

            val newList = items.toMutableList().apply {
                add(toIndex, removeAt(fromIndex))
            }
            set.copy(items = newList)
        }

        val fromIndex = from.index - 1
        val toIndex = to.index - 1
        if (fromIndex in itemIds.indices && toIndex in itemIds.indices && fromIndex != toIndex) {
            itemIds.add(toIndex, itemIds.removeAt(fromIndex))
        }
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().imePadding(),
            containerColor = DDZColor.Background,
            topBar = {
                DDZTopBar(title = "순환문구", onBack = onClose, actions = {
                    Box {
                        DDZTopBarIconButton(Icons.Default.MoreVert, "세트 메뉴", { showMenu = true })
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("세트 삭제", color = DDZColor.Destructive) },
                                onClick = { showMenu = false; openDeleteConfirmDialog() },
                                leadingIcon = { Icon(Icons.Default.DeleteOutline, null, tint = DDZColor.Destructive) },
                            )
                        }
                    }
                })
            },
        ) { insets ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(insets).padding(horizontal = 18.dp),
                state = lazyListState,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "set-name") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(phraseSet.name, style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
                            Text("${phraseSet.items.size}개 문구", style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                        }
                        IconButton(onClick = ::openRenameDialog) {
                            Icon(Icons.Default.Edit, "세트 이름 변경", tint = DDZColor.PrimaryDark)
                        }
                    }
                }
                items(
                    phraseSet.items.size,
                    key = { idx -> itemIds.getOrNull(idx) ?: "${phraseSet.id}-$idx" },
                ) { index ->
                    val item = phraseSet.items.getOrNull(index) ?: return@items
                    val stableId = itemIds.getOrNull(index) ?: "${phraseSet.id}-$index"
                    ReorderableItem(reorderableState, key = stableId) { isDragging ->
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(
                                    if (isDragging) DDZColor.SelectedSoft else DDZColor.Surface,
                                    RoundedCornerShape(12.dp),
                                )
                                .heightIn(min = 56.dp)
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(
                                onClick = {},
                                modifier = with(this@ReorderableItem) { Modifier.draggableHandle() },
                            ) {
                                Icon(Icons.Default.DragHandle, "문구 순서 변경", tint = DDZColor.IconMuted)
                            }
                            Text(
                                item,
                                modifier = Modifier.weight(1f).clickable {
                                    openItemInputDialog(index, item)
                                }.padding(vertical = 16.dp),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = DDZTypography.Body,
                                color = DDZColor.TextPrimary,
                            )
                            IconButton(onClick = {
                                if (index in itemIds.indices) itemIds.removeAt(index)
                                onUpdateSet { set ->
                                    set.copy(items = set.items.filterIndexed { idx, _ -> idx != index })
                                }
                            }) {
                                Icon(Icons.Default.DeleteOutline, "문구 삭제", tint = DDZColor.IconMuted)
                            }
                        }
                    }
                }
                item(key = "phrase-actions") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (phraseSet.items.isEmpty()) {
                            Text(
                                "문구를 추가해주세요.",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                style = DDZTypography.Body,
                                color = DDZColor.TextSecondary,
                            )
                        }
                        DDZButton(
                            text = "문구 추가",
                            onClick = { openItemInputDialog(null, "") },
                            leadingIcon = Icons.Default.Add,
                            style = DDZButtonStyle.Secondary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("번호 진행 방식", style = DDZTypography.CardTitle, color = DDZColor.TextPrimary)
                            RotatingCounterProgressMode.entries.forEach { mode ->
                                val selected = phraseSet.counterProgressMode == mode
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(
                                            if (selected) DDZColor.SelectedSoft else DDZColor.Surface,
                                            RoundedCornerShape(12.dp),
                                        )
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = { onUpdateSet { it.copy(counterProgressMode = mode) } },
                                        )
                                        .heightIn(min = 56.dp)
                                        .padding(end = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = null,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = DDZColor.SelectedDark,
                                            unselectedColor = DDZColor.IconMuted,
                                        ),
                                    )
                                    Column(Modifier.padding(vertical = 8.dp)) {
                                        Text(
                                            if (mode == RotatingCounterProgressMode.PER_PHRASE)
                                                "문구별로 따로" else "문구가 바뀌어도 계속",
                                            style = DDZTypography.Body,
                                            color = DDZColor.TextPrimary,
                                        )
                                        Text(
                                            if (mode == RotatingCounterProgressMode.PER_PHRASE)
                                                "A 0001 → B 0001 → A 0002" else "A 0001 → B 0002 → A 0003",
                                            style = DDZTypography.Caption,
                                            color = DDZColor.TextSecondary,
                                        )
                                    }
                                }
                            }
                            Text(
                                "변경한 내용은 표 편집에서 저장하면 적용돼요.",
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                                style = DDZTypography.Caption,
                                color = DDZColor.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        var renameDraft by remember(showRenameDialog, phraseSet.id) { mutableStateOf(renameInput) }
        AlertDialog(
            containerColor = DDZColor.Surface,
            titleContentColor = DDZColor.TextPrimary,
            textContentColor = DDZColor.TextPrimary,
            onDismissRequest = ::closeRenameDialog,
            title = { Text("세트 이름 변경") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    singleLine = true,
                    label = { Text("세트 이름") }
                )
            },
            confirmButton = {
                TextButton(enabled = renameDraft.isNotBlank(), onClick = {
                    val trimmed = renameDraft.trim()
                    if (trimmed.isNotBlank()) {
                        onUpdateSet { it.copy(name = trimmed) }
                    }
                    closeRenameDialog()
                }) {
                    Text("적용", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = ::closeRenameDialog) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (showItemInputDialog) {
        var itemDraft by remember(showItemInputDialog, editingItemIndex, phraseSet.id) { mutableStateOf(itemInput) }
        val isEdit = editingItemIndex != null
        AlertDialog(
            containerColor = DDZColor.Surface,
            titleContentColor = DDZColor.TextPrimary,
            textContentColor = DDZColor.TextPrimary,
            onDismissRequest = ::closeItemInputDialog,
            title = { Text(if (isEdit) "문구 수정" else "문구 추가") },
            text = {
                OutlinedTextField(
                    value = itemDraft,
                    onValueChange = { itemDraft = it },
                    singleLine = true,
                    label = { Text("문구") }
                )
            },
            confirmButton = {
                TextButton(enabled = itemDraft.isNotBlank(), onClick = {
                    val trimmed = itemDraft.trim()
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
                    closeItemInputDialog()
                }) {
                    Text("적용", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = ::closeItemInputDialog) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            containerColor = DDZColor.Surface,
            titleContentColor = DDZColor.TextPrimary,
            textContentColor = DDZColor.TextPrimary,
            onDismissRequest = ::closeDeleteConfirmDialog,
            title = { Text("세트 삭제") },
            text = { Text("${phraseSet.name} 세트를 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteSet(phraseSet.id)
                }) {
                    Text("삭제", style = DDZTypography.ButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = ::closeDeleteConfirmDialog) {
                    Text("취소", style = DDZTypography.ButtonText)
                }
            }
        )
    }
}

