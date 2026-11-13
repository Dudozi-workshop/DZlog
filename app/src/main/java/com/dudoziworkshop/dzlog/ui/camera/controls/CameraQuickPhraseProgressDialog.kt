package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Compact bottom-sheet editor: only the phrase list scrolls, never the progress footer. */
@Composable
internal fun CameraQuickPhraseProgressDialog(
    cell: TableCellState,
    set: RotatingPhraseSet,
    onDismiss: () -> Unit,
    onApply: (Int, Int) -> Unit,
) {
    var index by remember(cell.cellId, set.id) {
        mutableStateOf(cell.phraseIndex.coerceIn(0, set.items.lastIndex))
    }
    var count by remember(cell.cellId, set.id) {
        mutableStateOf(cell.phraseCompletedShots.toString())
    }
    var showNumberInput by remember(cell.cellId, set.id) { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val listHeight = minOf(
        (set.items.size.coerceAtMost(5) * 54).dp,
        LocalConfiguration.current.screenHeightDp.dp * 0.36f,
    )
    val currentPhrase = set.items[index]
    val every = (cell.everyOverride ?: set.everyForItem(index)).coerceAtLeast(1)
    val numeric = count.toIntOrNull()
    val validCount = numeric?.takeIf { it in 0 until every }
    val nextPhrase = set.items[(index + 1) % set.items.size]

    LaunchedEffect(cell.cellId, set.id) {
        listState.scrollToItem(index)
    }

    DDZBottomSheet(onDismiss = onDismiss, title = "문구 진행 상태") {
        Text(
            "${set.name} · 문구 ${set.items.size}개 · 기본 ${set.defaultEvery}장",
            color = DDZColor.TextSecondary,
            style = DDZTypography.Caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 5.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("순서", modifier = Modifier.width(42.dp),
                color = DDZColor.TextSecondary, style = DDZTypography.Caption)
            Text("다음 촬영 문구", modifier = Modifier.weight(1f),
                color = DDZColor.TextSecondary, style = DDZTypography.Caption)
            Text("변경 주기", color = DDZColor.TextSecondary, style = DDZTypography.Caption)
            Box(Modifier.width(24.dp))
        }
        // The viewport grows only up to five rows, then the list scrolls internally.
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(listHeight),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            itemsIndexed(set.items, key = { i, _ -> i }) { i, phrase ->
                val isSelected = i == index
                val period = (cell.everyOverride ?: set.everyForItem(i)).coerceAtLeast(1)
                val typeLabel = when {
                    cell.everyOverride != null -> "개별 설정"
                    set.itemEveryOverrides.getOrNull(i) != null -> "문구별 설정"
                    else -> null
                }
                Surface(
                    onClick = {
                        if (index != i) {
                            index = i
                            count = "0"
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(51.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) DDZColor.SelectedSoft else DDZColor.Surface,
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) DDZColor.SelectedDark else DDZColor.Border,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text((i + 1).toString().padStart(2, '0'),
                            modifier = Modifier.width(42.dp),
                            color = DDZColor.TextSecondary, style = DDZTypography.Secondary)
                        Text(phrase, modifier = Modifier.weight(1f),
                            color = DDZColor.TextPrimary, style = DDZTypography.Body,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${period}장", color = DDZColor.TextPrimary,
                                style = DDZTypography.Secondary)
                            if (typeLabel != null) Text(
                                typeLabel, color = DDZColor.TextSecondary,
                                style = DDZTypography.Caption,
                            )
                        }
                        Box(Modifier.width(24.dp), contentAlignment = Alignment.CenterEnd) {
                            if (isSelected) Text("✓", color = DDZColor.SelectionEmphasis,
                                style = DDZTypography.Body)
                        }
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            color = DDZColor.Border,
        )
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("저장 완료 장수", color = DDZColor.TextPrimary, style = DDZTypography.Body)
                Text("${index + 1}/${set.items.size} · $currentPhrase",
                    color = DDZColor.TextSecondary, style = DDZTypography.Caption,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DDZButton(
                    text = "−", modifier = Modifier.width(36.dp),
                    minHeight = 36.dp, horizontalPadding = 4.dp,
                    style = DDZButtonStyle.Secondary,
                    enabled = validCount != null && validCount > 0,
                    onClick = { count = ((validCount ?: 0) - 1).toString() },
                )
                Text(
                    "${count}장", modifier = Modifier.clickable { showNumberInput = true }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    color = DDZColor.TextPrimary, style = DDZTypography.Body,
                )
                DDZButton(
                    text = "+", modifier = Modifier.width(36.dp),
                    minHeight = 36.dp, horizontalPadding = 4.dp,
                    style = DDZButtonStyle.Secondary,
                    enabled = validCount != null && validCount < every - 1,
                    onClick = { count = ((validCount ?: 0) + 1).toString() },
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(4.dp)
                .background(DDZColor.Border, RoundedCornerShape(4.dp)),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth((validCount ?: 0).toFloat() / every)
                    .fillMaxHeight()
                    .background(DDZColor.SelectedDark, RoundedCornerShape(4.dp)),
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${every - (validCount ?: 0)}장 후 다음: $nextPhrase",
                modifier = Modifier.weight(1f),
                style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${count}/${every}장",
                style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
        if (validCount == null) Text(
            "완료 장수는 0~${every - 1}장 사이로 입력해 주세요.",
            style = DDZTypography.Caption, color = DDZColor.Destructive,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DDZButton(
                text = "취소", modifier = Modifier.weight(1f),
                style = DDZButtonStyle.Secondary, onClick = onDismiss,
            )
            DDZButton(
                text = "적용", modifier = Modifier.weight(1f),
                enabled = validCount != null,
                onClick = { validCount?.let { onApply(index, it) } },
            )
        }
    }

    if (showNumberInput) {
        var numberDraft by remember(showNumberInput, index) { mutableStateOf(count) }
        val number = numberDraft.toIntOrNull()?.takeIf { it in 0 until every }
        DDZAlertDialog(
            onDismissRequest = { showNumberInput = false },
            title = { Text("저장 완료 장수") },
            text = {
                OutlinedTextField(
                    value = numberDraft,
                    onValueChange = {
                        if (it.length <= 6 && it.all(Char::isDigit)) numberDraft = it
                    },
                    label = { Text("0~${every - 1}장") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = number == null,
                    singleLine = true,
                )
            },
            dismissButton = {
                DDZDialogAction(
                    onClick = { showNumberInput = false },
                    style = DDZButtonStyle.Secondary,
                ) { Text("취소") }
            },
            confirmButton = {
                DDZDialogAction(enabled = number != null, onClick = {
                    number?.let { count = it.toString(); showNumberInput = false }
                }) { Text("적용") }
            },
        )
    }
}
