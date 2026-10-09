package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.naming.NamingSlotPreview
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

private enum class RuleEditorTarget {
    VALUE,
    TYPE,
    ADD,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TableEditorRuleListEditor(
    isFileName: Boolean,
    items: List<MockRuleItem?>,
    cells: List<TableEditorCellUiModel>,
    rows: Int,
    cols: Int,
    onManualPreview: (Boolean, Int, String) -> NamingSlotPreview,
    onItemsChange: (List<MockRuleItem?>) -> Unit,
) {
    val compactItems = items.filterNotNull()
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editorTarget by remember { mutableStateOf<RuleEditorTarget?>(null) }
    var manualDraft by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val itemKeys = remember { mutableStateListOf<String>() }

    LaunchedEffect(compactItems.size) {
        while (itemKeys.size < compactItems.size) {
            itemKeys.add(UUID.randomUUID().toString())
        }
        while (itemKeys.size > compactItems.size) {
            itemKeys.removeAt(itemKeys.lastIndex)
        }
    }

    fun emitCompacted(next: List<MockRuleItem>) {
        val max = items.size.coerceAtLeast(1)
        onItemsChange(next.take(max) + List((max - next.size).coerceAtLeast(0)) { null })
    }

    fun replaceAt(index: Int, item: MockRuleItem) {
        if (index !in compactItems.indices) return
        emitCompacted(compactItems.toMutableList().also { it[index] = item })
        editorTarget = null
    }

    fun removeAt(index: Int) {
        if (index !in compactItems.indices) return
        if (index in itemKeys.indices) {
            itemKeys.removeAt(index)
        }
        emitCompacted(compactItems.filterIndexed { itemIndex, _ -> itemIndex != index })
    }

    fun append(item: MockRuleItem) {
        if (compactItems.size >= items.size) return
        itemKeys.add(UUID.randomUUID().toString())
        emitCompacted(compactItems + item)
        editorTarget = null
    }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        if (from.index !in compactItems.indices || to.index !in compactItems.indices) {
            return@rememberReorderableLazyListState
        }
        val reordered = compactItems.toMutableList()
        val moved = reordered.removeAt(from.index)
        val insertIndex = to.index.coerceIn(0, reordered.size)
        reordered.add(insertIndex, moved)
        if (from.index in itemKeys.indices) {
            val movedKey = itemKeys.removeAt(from.index)
            itemKeys.add(insertIndex.coerceIn(0, itemKeys.size), movedKey)
        }
        emitCompacted(reordered)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 330.dp),
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(
            items = compactItems,
            key = { index, _ -> itemKeys.getOrNull(index) ?: "rule-$index" },
        ) { index, item ->
            ReorderableItem(
                state = reorderableState,
                key = itemKeys.getOrNull(index) ?: "rule-$index",
            ) { isDragging ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        value == SwipeToDismissBoxValue.EndToStart ||
                            value == SwipeToDismissBoxValue.Settled
                    }
                )
                val rowScope = rememberCoroutineScope()

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    enableDismissFromEndToStart = true,
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DDZColor.DestructiveSoft, RoundedCornerShape(14.dp))
                                .clickable {
                                    removeAt(index)
                                    rowScope.launch { dismissState.reset() }
                                }
                                .padding(horizontal = 18.dp, vertical = 18.dp),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = DDZColor.Destructive,
                                )
                                Text(
                                    "삭제",
                                    style = DDZTypography.ButtonText,
                                    color = DDZColor.Destructive,
                                )
                            }
                        }
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDragging) DDZColor.SurfaceSoft else DDZColor.Surface,
                                RoundedCornerShape(14.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "순서 변경",
                            tint = DDZColor.IconMuted,
                            modifier = with(this@ReorderableItem) { Modifier.draggableHandle() },
                        )
                        Text(
                            text = item.value.ifBlank { "값 없음" },
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editingIndex = index
                                    manualDraft = if (item.sourceType == MockRuleSourceType.MANUAL) item.value else ""
                                    editorTarget = RuleEditorTarget.VALUE
                                }
                                .padding(vertical = 5.dp),
                            style = DDZTypography.SettingLabel,
                            color = DDZColor.TextPrimary,
                        )
                        Row(
                            modifier = Modifier
                                .clickable {
                                    editingIndex = index
                                    editorTarget = RuleEditorTarget.TYPE
                                }
                                .padding(vertical = 5.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = ruleTypeLabel(item.sourceType),
                                style = DDZTypography.Caption,
                                color = DDZColor.TextSecondary,
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = DDZColor.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }

    DDZButton(
        text = if (isFileName) "+ 구성요소 추가" else "+ 폴더 추가",
        modifier = Modifier.fillMaxWidth(),
        style = DDZButtonStyle.Secondary,
        enabled = compactItems.size < items.size,
        onClick = {
            editingIndex = null
            editorTarget = RuleEditorTarget.ADD
        },
    )

    val target = editorTarget
    if (target != null) {
        val active = editingIndex?.let { compactItems.getOrNull(it) }

        when {
            target == RuleEditorTarget.VALUE && active?.sourceType == MockRuleSourceType.CELL -> {
                DDZBottomSheet(
                    title = "셀 선택",
                    onDismiss = { editorTarget = null },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (row in 0 until rows) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                for (col in 0 until cols) {
                                    val cell = cells.getOrNull(row * cols + col)
                                    DDZButton(
                                        text = cell?.value?.ifBlank { "빈 셀" } ?: "-",
                                        modifier = Modifier.weight(1f),
                                        style = DDZButtonStyle.Secondary,
                                        enabled = cell != null && cell.type != TableEditorCellType.COUNTER,
                                        onClick = {
                                            val index = editingIndex ?: return@DDZButton
                                            val selected = cell ?: return@DDZButton
                                            replaceAt(
                                                index,
                                                MockRuleItem(
                                                    sourceType = MockRuleSourceType.CELL,
                                                    value = selected.value,
                                                    cellId = selected.domainCellId,
                                                )
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            target == RuleEditorTarget.VALUE && active?.sourceType == MockRuleSourceType.MANUAL -> {
                DDZBottomSheet(
                    title = if (isFileName) "직접 입력" else "폴더명 입력",
                    onDismiss = { editorTarget = null },
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        DDZTextField(
                            modifier = Modifier.fillMaxWidth(),
                            value = manualDraft,
                            onValueChange = { manualDraft = it },
                            label = if (isFileName) "파일명 값" else "폴더명",
                        )
                        val pendingPreview = editingIndex?.let { index ->
                            onManualPreview(isFileName, index, manualDraft)
                        }
                        if (pendingPreview != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .background(DDZColor.SurfaceSoft, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text("미리보기", style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                                Text(
                                    buildAnnotatedString {
                                        append(pendingPreview.text)
                                        if (pendingPreview.highlightStart >= 0 &&
                                            pendingPreview.highlightEnd > pendingPreview.highlightStart &&
                                            pendingPreview.highlightEnd <= pendingPreview.text.length
                                        ) {
                                            addStyle(
                                                SpanStyle(color = DDZColor.SelectedDark, fontWeight = FontWeight.Bold),
                                                pendingPreview.highlightStart,
                                                pendingPreview.highlightEnd,
                                            )
                                        }
                                    },
                                    style = DDZTypography.Body,
                                    color = DDZColor.TextPrimary,
                                )
                            }
                        }
                        DDZButton(
                            text = "확인",
                            modifier = Modifier.fillMaxWidth(),
                            enabled = manualDraft.trim().isNotBlank(),
                            onClick = {
                                val index = editingIndex ?: return@DDZButton
                                replaceAt(
                                    index,
                                    MockRuleItem(
                                        sourceType = MockRuleSourceType.MANUAL,
                                        value = manualDraft.trim(),
                                    )
                                )
                            },
                        )
                    }
                }
            }

            target == RuleEditorTarget.VALUE && active?.sourceType == MockRuleSourceType.DATE -> {
                ChoiceSheet(
                    title = "날짜 형식",
                    options = listOf("20261008", "2026-10-08", "26.10.08", "10월 08일"),
                    onDismiss = { editorTarget = null },
                    onSelect = { value ->
                        val index = editingIndex ?: return@ChoiceSheet
                        replaceAt(
                            index,
                            MockRuleItem(
                                sourceType = MockRuleSourceType.DATE,
                                value = value,
                                formatPattern = when (value) {
                                    "2026-10-08" -> "yyyy-MM-dd"
                                    "26.10.08" -> "yy.MM.dd"
                                    "10월 08일" -> "MM월 dd일"
                                    else -> "yyyyMMdd"
                                },
                            )
                        )
                    },
                )
            }

            target == RuleEditorTarget.VALUE && active?.sourceType == MockRuleSourceType.TIME -> {
                ChoiceSheet(
                    title = "시간 형식",
                    options = listOf("0930"),
                    onDismiss = { editorTarget = null },
                    onSelect = { value ->
                        val index = editingIndex ?: return@ChoiceSheet
                        replaceAt(
                            index,
                            MockRuleItem(
                                sourceType = MockRuleSourceType.TIME,
                                value = value,
                                formatPattern = "HHmm",
                            )
                        )
                    },
                )
            }

            target == RuleEditorTarget.VALUE && active?.sourceType == MockRuleSourceType.ROTATING_TEXT -> {
                ChoiceSheet(
                    title = "순환문구",
                    options = listOf("순환문구"),
                    onDismiss = { editorTarget = null },
                    onSelect = { value ->
                        val index = editingIndex ?: return@ChoiceSheet
                        replaceAt(index, MockRuleItem(MockRuleSourceType.ROTATING_TEXT, value))
                    },
                )
            }

            else -> {
                val title = when (target) {
                    RuleEditorTarget.TYPE -> "종류 변경"
                    RuleEditorTarget.ADD -> if (isFileName) "구성요소 추가" else "폴더 추가"
                    RuleEditorTarget.VALUE -> "값 설정"
                }
                DDZBottomSheet(
                    title = title,
                    onDismiss = { editorTarget = null },
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            MockRuleSourceType.CELL,
                            MockRuleSourceType.MANUAL,
                            MockRuleSourceType.DATE,
                            MockRuleSourceType.TIME,
                            MockRuleSourceType.ROTATING_TEXT,
                        ).forEach { type ->
                            DDZButton(
                                text = ruleTypeLabel(type),
                                modifier = Modifier.fillMaxWidth(),
                                style = DDZButtonStyle.Secondary,
                                onClick = {
                                    val next = defaultRuleItem(type)
                                    if (target == RuleEditorTarget.ADD) {
                                        append(next)
                                    } else {
                                        val index = editingIndex ?: return@DDZButton
                                        replaceAt(index, next)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChoiceSheet(
    title: String,
    options: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    DDZBottomSheet(title = title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                DDZButton(
                    text = option,
                    modifier = Modifier.fillMaxWidth(),
                    style = DDZButtonStyle.Secondary,
                    onClick = { onSelect(option) },
                )
            }
        }
    }
}

private fun defaultRuleItem(type: MockRuleSourceType): MockRuleItem =
    when (type) {
        MockRuleSourceType.CELL -> MockRuleItem(type, "")
        MockRuleSourceType.MANUAL -> MockRuleItem(type, "")
        MockRuleSourceType.DATE -> MockRuleItem(
            sourceType = type,
            value = "20261008",
            formatPattern = "yyyyMMdd",
        )
        MockRuleSourceType.TIME -> MockRuleItem(
            sourceType = type,
            value = "0930",
            formatPattern = "HHmm",
        )
        MockRuleSourceType.ROTATING_TEXT -> MockRuleItem(type, "순환문구")
    }

private fun ruleTypeLabel(type: MockRuleSourceType): String =
    when (type) {
        MockRuleSourceType.CELL -> "셀"
        MockRuleSourceType.MANUAL -> "직접입력"
        MockRuleSourceType.DATE -> "날짜"
        MockRuleSourceType.TIME -> "시간"
        MockRuleSourceType.ROTATING_TEXT -> "순환문구"
    }
