package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgressPolicy
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

@Composable
internal fun CameraQuickPhraseProgressCard(
    cell: TableCellState,
    set: RotatingPhraseSet,
    enabled: Boolean,
    onProgressChange: (Int, Int) -> Unit,
) {
    var open by remember(cell.cellId) { mutableStateOf(false) }
    val progress = CellPhraseProgressPolicy.current(cell, set)
    val every = (cell.everyOverride ?: set.everyForItem(progress.index)).coerceAtLeast(1)
    val phrase = set.items[progress.index]
    val next = set.items[(progress.index + 1) % set.items.size]
    Surface(
        onClick = { open = true },
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DDZColor.Border),
        color = DDZColor.Surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("문구 진행 상태", style = DDZTypography.Secondary, color = DDZColor.TextPrimary)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(phrase, modifier = Modifier.weight(1f), style = DDZTypography.Body,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, color = DDZColor.TextPrimary)
                Text("${progress.completedShots} / ${every}장", style = DDZTypography.Secondary,
                    color = DDZColor.TextSecondary)
            }
            // Display-only progress line; never participates in table rendering.
            Box(Modifier.fillMaxWidth().height(4.dp)) {
                Surface(color = DDZColor.Border, shape = RoundedCornerShape(50)) {
                    Box(Modifier.fillMaxWidth().height(4.dp))
                }
                Surface(color = DDZColor.SelectedDark, shape = RoundedCornerShape(50)) {
                    Box(Modifier.fillMaxWidth(progress.completedShots.toFloat() / every).height(4.dp))
                }
            }
            Text("${every - progress.completedShots}장 후 다음: $next", style = DDZTypography.Caption,
                color = DDZColor.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    if (open) {
        CameraQuickPhraseProgressDialog(
            cell = cell, set = set,
            onDismiss = { open = false },
            onApply = { index, completed -> onProgressChange(index, completed); open = false },
        )
    }
}

/** The single stepper edits the selected scope; button labels always show both stored values. */
@Composable
internal fun CameraQuickPhrasePeriodControl(
    cell: TableCellState,
    set: RotatingPhraseSet,
    enabled: Boolean,
    onPeriodChange: (Boolean, Int) -> Unit,
) {
    var expanded by remember(cell.cellId) { mutableStateOf(false) }
    var numberDialog by remember(cell.cellId) { mutableStateOf(false) }
    val isSet = cell.everyOverride == null
    val currentIndex = cell.phraseIndex.coerceIn(0, set.items.lastIndex)
    // Keep the last local value while temporarily following the set in this editor.
    var rememberedIndividual by remember(cell.cellId, set.id) {
        mutableStateOf(cell.everyOverride ?: set.everyForItem(currentIndex))
    }
    LaunchedEffect(cell.everyOverride) {
        cell.everyOverride?.let { rememberedIndividual = it }
    }
    val cellValue = cell.everyOverride ?: rememberedIndividual
    val activeValue = if (isSet) set.defaultEvery else cellValue
    val effectiveEvery = cell.everyOverride ?: set.everyForItem(currentIndex)

    Surface(color = DDZColor.Surface, border = BorderStroke(1.dp, DDZColor.Border),
        shape = RoundedCornerShape(10.dp)) {
        Column {
            Column(
                Modifier.fillMaxWidth().clickable(enabled) { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("변경 주기", color = DDZColor.TextPrimary, style = DDZTypography.Secondary)
                if (!expanded) Text(
                    "${if (isSet) "세트 설정" else "개별 설정"} · 현재 ${effectiveEvery}장",
                    color = DDZColor.TextSecondary, style = DDZTypography.Secondary)
            }
            if (expanded) {
                Column(Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DDZButton(
                            text = "세트 설정 (${set.defaultEvery}장)",
                            modifier = Modifier.weight(1f),
                            minHeight = 36.dp, horizontalPadding = 2.dp,
                            textStyleOverride = DDZTypography.Caption,
                            style = DDZButtonStyle.Secondary,
                            containerColorOverride = if (isSet) DDZColor.SelectedSoft else null,
                            enabled = enabled,
                            onClick = { if (!isSet) onPeriodChange(true, set.defaultEvery) },
                        )
                        DDZButton(
                            text = "개별 설정 (${cellValue}장)",
                            modifier = Modifier.weight(1f),
                            minHeight = 36.dp, horizontalPadding = 2.dp,
                            textStyleOverride = DDZTypography.Caption,
                            style = DDZButtonStyle.Secondary,
                            containerColorOverride = if (!isSet) DDZColor.SelectedSoft else null,
                            enabled = enabled,
                            onClick = { if (isSet) onPeriodChange(false, cellValue) },
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically) {
                        DDZButton(text = "−", modifier = Modifier.width(36.dp),
                            style = DDZButtonStyle.Secondary, minHeight = 36.dp,
                            horizontalPadding = 3.dp, enabled = enabled && activeValue > 1,
                            onClick = { onPeriodChange(isSet, activeValue - 1) })
                        Text("${activeValue}장",
                            modifier = Modifier.clickable(enabled) { numberDialog = true }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            style = DDZTypography.Body, color = DDZColor.TextPrimary)
                        DDZButton(text = "+", modifier = Modifier.width(36.dp),
                            style = DDZButtonStyle.Secondary, minHeight = 36.dp,
                            horizontalPadding = 3.dp,
                            enabled = enabled && activeValue < RotatingPhraseSet.MAX_EVERY,
                            onClick = { onPeriodChange(isSet, activeValue + 1) })
                    }
                    Text(
                        if (isSet) "세트 기본 주기를 변경합니다." else "현재 셀에만 적용돼요.",
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                        maxLines = 1, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
    if (numberDialog) {
        QuickPhrasePeriodNumberDialog(initial = activeValue,
            onDismiss = { numberDialog = false },
            onApply = {
                numberDialog = false
                onPeriodChange(isSet, it)
            })
    }
}

@Composable
private fun QuickPhrasePeriodNumberDialog(initial: Int, onDismiss: () -> Unit, onApply: (Int) -> Unit) {
    var text by remember(initial) { mutableStateOf(initial.toString()) }
    val value = text.toIntOrNull()?.takeIf { it in 1..RotatingPhraseSet.MAX_EVERY }
    DDZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("변경 주기 입력") },
        text = {
            OutlinedTextField(value = text,
                onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) text = it },
                label = { Text("장수") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, isError = value == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        },
        dismissButton = {
            DDZDialogAction(onClick = onDismiss, style = DDZButtonStyle.Secondary) { Text("취소") }
        },
        confirmButton = {
            DDZDialogAction(onClick = { value?.let(onApply) }, enabled = value != null) { Text("적용") }
        },
    )
}

