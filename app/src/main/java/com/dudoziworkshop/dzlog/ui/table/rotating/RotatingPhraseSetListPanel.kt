package com.dudoziworkshop.dzlog.ui.table.rotating

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import java.util.UUID

/** Only the phrase rows scroll. Header, interval and footer never move with them. */
@Composable
internal fun RotatingPhraseSetListPanel(
    phraseSet: RotatingPhraseSet,
    modifier: Modifier = Modifier,
    onUpdateSet: ((RotatingPhraseSet) -> RotatingPhraseSet) -> Unit,
    onEditItem: (Int, String) -> Unit,
    onEditPeriod: (Int) -> Unit,
    onBulkPeriod: (List<Int>) -> Unit,
    onAddItem: () -> Unit,
) {
    var searchExpanded by remember(phraseSet.id) { mutableStateOf(false) }
    var query by remember(phraseSet.id) { mutableStateOf("") }
    val visibleIndices = RotatingPhraseSearchPolicy.matchingIndices(phraseSet.items, query)
    var selecting by remember(phraseSet.id) { mutableStateOf(false) }
    val selected = remember(phraseSet.id) { mutableStateListOf<Int>() }
    val canReorder by rememberUpdatedState(!selecting && RotatingPhraseSearchPolicy.reorderEnabled(query))
    val rowIds = remember(phraseSet.id) {
        mutableStateListOf<String>().apply {
            repeat(phraseSet.items.size) { add(UUID.randomUUID().toString()) }
        }
    }
    LaunchedEffect(phraseSet.id, phraseSet.items.size) {
        while (rowIds.size < phraseSet.items.size) rowIds.add(UUID.randomUUID().toString())
        while (rowIds.size > phraseSet.items.size) rowIds.removeAt(rowIds.lastIndex)
        selected.removeAll { it !in phraseSet.items.indices }
    }

    val scroll = rememberLazyListState()
    val reorderable = rememberReorderableLazyListState(scroll) { from, to ->
        // No header item in this LazyColumn: drag indices now start at zero.
        if (canReorder && from.index in phraseSet.items.indices && to.index in phraseSet.items.indices) {
            if (from.index != to.index) {
                onUpdateSet { RotatingPhraseSetItemPolicy.moveItem(it, from.index, to.index) }
                if (from.index in rowIds.indices && to.index in rowIds.indices) {
                    rowIds.add(to.index, rowIds.removeAt(from.index))
                }
            }
        }
    }

    Surface(
        modifier = modifier,
        color = DDZColor.Surface,
        border = BorderStroke(1.dp, DDZColor.Border),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "문구 목록 (${phraseSet.items.size}개)",
                    modifier = Modifier.weight(1f),
                    style = DDZTypography.CardTitle,
                    color = DDZColor.TextPrimary,
                )
                DDZButton(
                    text = if (selecting) "취소" else "선택",
                    style = DDZButtonStyle.Secondary,
                    horizontalPadding = 6.dp,
                    minHeight = 40.dp,
                    onClick = {
                        selecting = !selecting
                        selected.clear()
                    },
                )
                IconButton(onClick = {
                    searchExpanded = !searchExpanded
                    if (!searchExpanded) query = ""
                }) {
                    Icon(Icons.Default.Search, contentDescription = "문구 검색")
                }
                IconButton(
                    enabled = !selecting && phraseSet.items.size < RotatingPhraseSet.MAX_ITEMS,
                    onClick = {
                        query = ""
                        onAddItem()
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "문구 추가", tint = DDZColor.PrimaryDark)
                }
            }
            if (searchExpanded) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("문구 검색") },
                    singleLine = true,
                )
            }
            if (selecting) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${selected.size}개 선택됨",
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                    DDZButton(
                        text = if (visibleIndices.isNotEmpty() && visibleIndices.all { it in selected })
                            "검색 결과 선택 해제" else "검색 결과 전체 선택",
                        style = DDZButtonStyle.Secondary,
                        horizontalPadding = 6.dp,
                        minHeight = 40.dp,
                        onClick = {
                            if (visibleIndices.all { it in selected }) {
                                selected.removeAll { it in visibleIndices }
                            } else {
                                visibleIndices.forEach { if (it !in selected) selected.add(it) }
                            }
                        },
                    )
                }
                DDZButton(
                    text = "선택한 ${selected.size}개 변경 주기 설정",
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selected.isNotEmpty(),
                    onClick = {
                        val indices = selected.toList().sorted()
                        selecting = false
                        selected.clear()
                        onBulkPeriod(indices)
                    },
                )
            } else if (!RotatingPhraseSearchPolicy.reorderEnabled(query)) {
                Text(
                    "검색 결과 ${visibleIndices.size}개 · 검색 중 순서 변경 불가",
                    style = DDZTypography.Caption,
                    color = DDZColor.TextSecondary,
                )
            }
            if (phraseSet.items.size >= RotatingPhraseSet.MAX_ITEMS) {
                Text("문구 최대 ${RotatingPhraseSet.MAX_ITEMS}개",
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
            HorizontalDivider(color = DDZColor.Border)
            LazyColumn(
                state = scroll,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                if (visibleIndices.isEmpty()) {
                    item {
                        Text(
                            if (phraseSet.items.isEmpty()) "문구를 추가해 주세요." else "검색 결과가 없습니다.",
                            modifier = Modifier.padding(vertical = 22.dp, horizontal = 8.dp),
                            style = DDZTypography.Body,
                            color = DDZColor.TextSecondary,
                        )
                    }
                }
                items(
                    count = visibleIndices.size,
                    key = { visiblePosition ->
                        val realIndex = visibleIndices[visiblePosition]
                        rowIds.getOrNull(realIndex) ?: "${phraseSet.id}-$realIndex"
                    },
                ) { visiblePosition ->
                    val index = visibleIndices[visiblePosition]
                    val item = phraseSet.items.getOrNull(index) ?: return@items
                    val stableKey = rowIds.getOrNull(index) ?: "${phraseSet.id}-$index"
                    ReorderableItem(reorderable, key = stableKey) { dragging ->
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (dragging) DDZColor.SelectedSoft else DDZColor.Surface,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .heightIn(min = 54.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (selecting) {
                                    Checkbox(
                                        checked = index in selected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (index !in selected) selected.add(index)
                                            } else {
                                                selected.remove(index)
                                            }
                                        },
                                    )
                                } else IconButton(
                                    enabled = canReorder,
                                    onClick = {},
                                    modifier = if (canReorder) with(this@ReorderableItem) {
                                        Modifier.draggableHandle()
                                    } else Modifier,
                                ) {
                                    Icon(
                                        Icons.Default.DragHandle,
                                        contentDescription = if (canReorder) "문구 순서 변경" else "검색 중 순서 변경 불가",
                                        tint = if (canReorder) DDZColor.IconMuted else DDZColor.TextDisabled,
                                    )
                                }
                                Text(
                                    (index + 1).toString(),
                                    modifier = Modifier.width(27.dp),
                                    style = DDZTypography.Caption,
                                    color = DDZColor.TextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    item,
                                    modifier = Modifier.weight(1f)
                                        .clickable {
                                            if (selecting) {
                                                if (index in selected) selected.remove(index) else selected.add(index)
                                            } else onEditItem(index, item)
                                        }
                                        .padding(vertical = 14.dp),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = DDZTypography.Body,
                                    color = DDZColor.TextPrimary,
                                )
                                Text(
                                    "${phraseSet.everyForItem(index)}장",
                                    modifier = Modifier
                                        .clickable {
                                            if (selecting) {
                                                if (index in selected) selected.remove(index) else selected.add(index)
                                            } else onEditPeriod(index)
                                        }
                                        .widthIn(min = 48.dp)
                                        .padding(horizontal = 6.dp, vertical = 12.dp),
                                    textAlign = TextAlign.Center,
                                    style = DDZTypography.Caption,
                                    color = if (phraseSet.itemEveryOverrides.getOrNull(index) != null)
                                        DDZColor.PrimaryDark else DDZColor.TextSecondary,
                                )
                                if (!selecting) IconButton(onClick = {
                                    if (index in rowIds.indices) rowIds.removeAt(index)
                                    onUpdateSet { RotatingPhraseSetItemPolicy.removeItem(it, index) }
                                }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "문구 삭제", tint = DDZColor.IconMuted)
                                }
                            }
                            HorizontalDivider(color = DDZColor.Border)
                        }
                    }
                }
            }
        }
    }
}
