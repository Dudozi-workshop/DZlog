package com.dudoziworkshop.dzlog.ui.camera.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.feature.table.policy.QuickPhrasePeriodConflict
import com.dudoziworkshop.dzlog.ui.common.DDZAlertDialog
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZDialogAction
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

/** Nothing is committed until every newly overflowing cell has a valid choice. */
@Composable
internal fun CameraQuickPhraseConflictDialog(
    proposed: TableTemplateState,
    conflicts: List<QuickPhrasePeriodConflict>,
    onDismiss: () -> Unit,
    onApply: (Map<String, CellPhraseProgress>) -> Unit,
) {
    val indices = remember(conflicts) {
        mutableStateMapOf<String, Int>().apply {
            conflicts.forEach { put(it.cellId, it.phraseIndex) }
        }
    }
    val counts = remember(conflicts) {
        mutableStateMapOf<String, String>().apply {
            conflicts.forEach { put(it.cellId, (it.newEvery - 1).toString()) }
        }
    }
    val sets = proposed.phraseSets.associateBy { it.id }
    val cells = proposed.cells.associateBy { it.cellId }
    val choices = conflicts.mapNotNull { conflict ->
        val cell = cells[conflict.cellId] ?: return@mapNotNull null
        val set = sets[cell.phraseSetId] ?: return@mapNotNull null
        val index = indices[conflict.cellId] ?: return@mapNotNull null
        if (index !in set.items.indices) return@mapNotNull null
        val every = (cell.everyOverride ?: set.everyForItem(index)).coerceAtLeast(1)
        val count = counts[conflict.cellId]?.toIntOrNull() ?: return@mapNotNull null
        if (count !in 0 until every) return@mapNotNull null
        conflict.cellId to CellPhraseProgress(index, count)
    }.toMap()
    val canApply = choices.size == conflicts.size
    DDZAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("완료 장수 조정") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("새 주기를 초과한 ${conflicts.size}개 셀의 진행 상태를 조정해 주세요.",
                    style = DDZTypography.Secondary, color = DDZColor.TextSecondary)
                conflicts.forEach { conflict ->
                    val cell = cells[conflict.cellId] ?: return@forEach
                    val set = sets[cell.phraseSetId] ?: return@forEach
                    val index = indices[conflict.cellId] ?: conflict.phraseIndex
                    val currentEvery = (cell.everyOverride ?: set.everyForItem(index)).coerceAtLeast(1)
                    Text("${conflict.row}행 ${conflict.column}열 · ${set.items[index]}",
                        style = DDZTypography.Body, color = DDZColor.TextPrimary)
                    Text("기존 ${conflict.completed}장 · 변경 주기 ${conflict.newEvery}장",
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DDZButton(text = "현재 문구", modifier = Modifier.weight(1f),
                            minHeight = 36.dp, horizontalPadding = 4.dp,
                            style = DDZButtonStyle.Secondary,
                            containerColorOverride = if (index == conflict.phraseIndex) DDZColor.SelectedSoft else null,
                            onClick = {
                                indices[conflict.cellId] = conflict.phraseIndex
                                counts[conflict.cellId] = "0"
                            })
                        DDZButton(text = "다음 문구", modifier = Modifier.weight(1f),
                            minHeight = 36.dp, horizontalPadding = 4.dp,
                            style = DDZButtonStyle.Secondary,
                            containerColorOverride = if (index != conflict.phraseIndex) DDZColor.SelectedSoft else null,
                            onClick = {
                                indices[conflict.cellId] = (conflict.phraseIndex + 1) % set.items.size
                                counts[conflict.cellId] = "0"
                            })
                    }
                    OutlinedTextField(
                        value = counts[conflict.cellId].orEmpty(),
                        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) counts[conflict.cellId] = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("저장 완료 장수 (0~${currentEvery - 1})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = conflict.cellId !in choices,
                    )
                }
            }
        },
        dismissButton = {
            DDZDialogAction(onClick = onDismiss, style = DDZButtonStyle.Secondary) { Text("취소") }
        },
        confirmButton = {
            DDZDialogAction(enabled = canApply, onClick = { if (canApply) onApply(choices) }) {
                Text("적용")
            }
        },
    )
}
