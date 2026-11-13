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
import androidx.compose.ui.text.style.TextAlign
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
    val rowHeight = 48.dp
    val listHeight = minOf(
        rowHeight * set.items.size.coerceAtMost(5),
        LocalConfiguration.current.screenHeightDp.dp * 0.36f,
    )
    val every = (cell.everyOverride ?: set.everyForItem(index)).coerceAtLeast(1)
    val validCount = count.toIntOrNull()?.takeIf { it in 0 until every }

    LaunchedEffect(cell.cellId, set.id) {
        listState.scrollToItem(index)
    }

    DDZBottomSheet(onDismiss = onDismiss) {
        Text(
            "문구 진행 상태",
            modifier = Modifier.fillMaxWidth(),
            style = DDZTypography.SectionTitle,
            color = DDZColor.TextPrimary,
            textAlign = TextAlign.Center,
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
            color = DDZColor.Border,
        )
        // One table border, centered header text and left-aligned phrase values.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, DDZColor.Border),
            shape = RoundedCornerShape(10.dp),
            color = DDZColor.Surface,
        ) {
            Column {
                Surface(color = DDZColor.SurfaceSoft) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "순서", modifier = Modifier.width(42.dp),
                            style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "문구명", modifier = Modifier.weight(1f),
                            style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            "주기", modifier = Modifier.width(44.dp),
                            style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                            textAlign = TextAlign.Center,
                        )
                        Box(Modifier.width(22.dp))
                    }
                }
                HorizontalDivider(color = DDZColor.Border)
                // At most five visible rows; only this section scrolls.
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(listHeight),
                    state = listState,
                ) {
                    itemsIndexed(set.items, key = { i, _ -> i }) { i, phrase ->
                        if (i > 0) HorizontalDivider(color = DDZColor.Border)
                        val chosen = i == index
                        val period = (cell.everyOverride ?: set.everyForItem(i)).coerceAtLeast(1)
                        Surface(
                            onClick = {
                                if (!chosen) {
                                    index = i
                                    count = "0"
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(rowHeight - if (i > 0) 1.dp else 0.dp),
                            color = if (chosen) DDZColor.SelectedSoft else DDZColor.Surface,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    (i + 1).toString().padStart(2, '0'),
                                    modifier = Modifier.width(42.dp),
                                    style = DDZTypography.Secondary, color = DDZColor.TextSecondary,
                                    textAlign = TextAlign.Center,
                                )
                                Text(
                                    phrase, modifier = Modifier.weight(1f),
                                    style = DDZTypography.Body, color = DDZColor.TextPrimary,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Start,
                                )
                                Text(
                                    "${period}장", modifier = Modifier.width(44.dp),
                                    style = DDZTypography.Secondary, color = DDZColor.TextPrimary,
                                    textAlign = TextAlign.Center,
                                )
                                Box(Modifier.width(22.dp), contentAlignment = Alignment.Center) {
                                    if (chosen) Text(
                                        "✓", style = DDZTypography.Body,
                                        color = DDZColor.SelectionEmphasis,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 14.dp, bottom = 14.dp),
            color = DDZColor.Border,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "저장 완료 장수",
                style = DDZTypography.Body,
                color = DDZColor.TextPrimary,
            )
            if (every == 1) {
                Text("0 / 1장", style = DDZTypography.Body, color = DDZColor.TextPrimary)
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    DDZButton(
                        text = "−", modifier = Modifier.width(36.dp),
                        minHeight = 36.dp, horizontalPadding = 4.dp,
                        style = DDZButtonStyle.Secondary,
                        enabled = validCount != null && validCount > 0,
                        onClick = { count = ((validCount ?: 0) - 1).toString() },
                    )
                    Text(
                        "$count / ${every}장",
                        modifier = Modifier.clickable { showNumberInput = true }
                            .padding(horizontal = 7.dp, vertical = 9.dp),
                        style = DDZTypography.Body,
                        color = DDZColor.TextPrimary,
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
        }
        if (validCount == null) {
            Text(
                "완료 장수는 0~${every - 1}장 사이로 입력해 주세요.",
                style = DDZTypography.Caption,
                color = DDZColor.Destructive,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
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
