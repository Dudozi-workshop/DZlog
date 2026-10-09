package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.ui.common.DDZBottomSheet
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZContentDialog
import com.dudoziworkshop.dzlog.ui.common.DDZSettingRow
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TableEditorCellUiModelEditor(
    cell: TableEditorCellUiModel,
    phraseSets: List<com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet>,
    onValueChange: (String) -> Unit,
    onTypeChange: (TableEditorCellType) -> Unit,
    onDatePatternChange: (String) -> Unit,
    onApplyTimePolicy: () -> Unit,
    onPhraseSetChange: (String?) -> Unit,
    onPhraseEveryChange: (Int) -> Unit,
    onCreatePhraseSet: (String) -> Unit,
    onUpdatePhraseSet: (String, (com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> Unit,
    onDeletePhraseSet: (String) -> Unit,
    onClose: () -> Unit,
) {
    var showTypePicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPhrasePicker by remember { mutableStateOf(false) }
    var showCreatePhraseSet by remember { mutableStateOf(false) }
    var createPhraseSetName by remember { mutableStateOf("") }
    var editingPhraseSetId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DDZColor.Surface)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("선택한 셀", fontWeight = FontWeight.Bold)
            Text(
                "×",
                modifier = Modifier
                    .clickable(onClick = onClose)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                color = DDZColor.TextMuted,
            )
        }

        when (cell.type) {
            TableEditorCellType.TEXT,
            TableEditorCellType.NUMBER -> {
                DDZTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = cell.value,
                    onValueChange = onValueChange,
                    label = "값",
                )
            }

            TableEditorCellType.COUNTER -> {
                val currentCounter = cell.value.toIntOrNull()?.coerceAtLeast(0) ?: 1
                Text("자동번호", fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DDZButton(
                        text = "−",
                        style = DDZButtonStyle.Secondary,
                        minHeight = 40.dp,
                        onClick = { onValueChange((currentCounter - 1).coerceAtLeast(0).toString()) },
                    )
                    DDZTextField(
                        modifier = Modifier.weight(1f),
                        value = currentCounter.toString(),
                        onValueChange = { input ->
                            val digits = input.filter { it.isDigit() }
                            if (digits.isNotBlank()) {
                                onValueChange(digits)
                            }
                        },
                        label = "시작 번호",
                    )
                    DDZButton(
                        text = "+",
                        style = DDZButtonStyle.Secondary,
                        minHeight = 40.dp,
                        onClick = { onValueChange((currentCounter + 1).toString()) },
                    )
                }
                Text("촬영 성공 후 다음 번호로 증가합니다.", color = DDZColor.TextMuted)
            }

            TableEditorCellType.DATE -> {
                Text(
                    text = when (cell.formatPattern.ifBlank { "yyyyMMdd" }) {
                        "yyMMdd" -> "261007"
                        "MMdd" -> "1007"
                        else -> "20261007"
                    },
                    fontWeight = FontWeight.Bold,
                )
                DDZSettingRow(
                    label = "날짜 형식",
                    value = cell.formatPattern.ifBlank { "yyyyMMdd" },
                    onClick = { showDatePicker = true },
                )
            }

            TableEditorCellType.TIME -> {
                Text("1251", fontWeight = FontWeight.Bold)
                DDZSettingRow(
                    label = "시간 형식",
                    value = "HHmm · 분 단위 고정",
                    onClick = onApplyTimePolicy,
                )
            }

            TableEditorCellType.ROTATING_TEXT -> {
                val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
                Text(
                    selectedSet?.items?.firstOrNull().orEmpty().ifBlank { "문구 세트를 선택하세요" },
                    fontWeight = FontWeight.Bold,
                )
                DDZSettingRow(
                    label = "문구 세트",
                    value = selectedSet?.name ?: "선택 안 함",
                    onClick = { showPhrasePicker = true },
                )
                if (selectedSet != null) {
                    val everyValue = (cell.everyOverride ?: selectedSet.defaultEvery).coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("변경 주기", modifier = Modifier.weight(1f), color = DDZColor.TextMuted)
                        DDZButton(
                            text = "−",
                            style = DDZButtonStyle.Secondary,
                            minHeight = 40.dp,
                            onClick = { onPhraseEveryChange((everyValue - 1).coerceAtLeast(1)) },
                        )
                        Text(everyValue.toString() + "장")
                        DDZButton(
                            text = "+",
                            style = DDZButtonStyle.Secondary,
                            minHeight = 40.dp,
                            onClick = { onPhraseEveryChange(everyValue + 1) },
                        )
                    }
                }
            }
        }

        DDZSettingRow(
            label = "셀 종류",
            value = cell.type.label,
            onClick = { showTypePicker = true },
        )
    }

    if (showTypePicker) {
        DDZBottomSheet(
            title = "셀에 무엇을 표시할까요?",
            onDismiss = { showTypePicker = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TableEditorCellType.entries.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { type ->
                            DDZButton(
                                text = if (type == cell.type) type.label + " ✓" else type.label,
                                modifier = Modifier.weight(1f),
                                style = DDZButtonStyle.Secondary,
                                onClick = {
                                    onTypeChange(type)
                                    showTypePicker = false
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showDatePicker) {
        DDZBottomSheet(
            title = "날짜 형식",
            onDismiss = { showDatePicker = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf("yyyyMMdd", "yyMMdd", "MMdd").forEach { pattern ->
                    DDZButton(
                        text = if (cell.formatPattern.ifBlank { "yyyyMMdd" } == pattern) pattern + " ✓" else pattern,
                        modifier = Modifier.fillMaxWidth(),
                        style = DDZButtonStyle.Secondary,
                        onClick = {
                            onDatePatternChange(pattern)
                            showDatePicker = false
                        },
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showPhrasePicker) {
        DDZBottomSheet(
            title = "문구 세트",
            onDismiss = { showPhrasePicker = false },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {

                DDZButton(
                    text = "+ 새 문구 세트",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        createPhraseSetName = ""
                        showCreatePhraseSet = true
                    },
                )

                if (phraseSets.isEmpty()) {
                    Text("등록된 문구 세트가 없습니다.", color = DDZColor.TextMuted)
                } else {
                    phraseSets.forEach { set ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DDZButton(
                                text = set.name + " · " + set.items.size + "개" +
                                    if (cell.phraseSetId == set.id) " ✓" else "",
                                modifier = Modifier.weight(1f),
                                style = DDZButtonStyle.Secondary,
                                onClick = {
                                    onPhraseSetChange(set.id)
                                    showPhrasePicker = false
                                },
                            )
                            DDZButton(
                                text = "편집",
                                style = DDZButtonStyle.Text,
                                minHeight = 40.dp,
                                onClick = {
                                    editingPhraseSetId = set.id
                                    showPhrasePicker = false
                                },
                            )
                        }
                    }

                    DDZButton(
                        text = "선택 해제",
                        modifier = Modifier.fillMaxWidth(),
                        style = DDZButtonStyle.Text,
                        onClick = {
                            onPhraseSetChange(null)
                            showPhrasePicker = false
                        },
                    )
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }

    if (showCreatePhraseSet) {
        DDZContentDialog(
            title = "새 문구 세트",
            onDismiss = { showCreatePhraseSet = false },
            content = {
                DDZTextField(
                    value = createPhraseSetName,
                    onValueChange = { createPhraseSetName = it },
                    label = "세트 이름",
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            actions = {
                DDZButton(
                    text = "취소",
                    style = DDZButtonStyle.Secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { showCreatePhraseSet = false },
                )
                DDZButton(
                    text = "추가",
                    enabled = createPhraseSetName.trim().isNotBlank(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onCreatePhraseSet(createPhraseSetName.trim())
                        showCreatePhraseSet = false
                    },
                )
            },
        )
    }

    editingPhraseSetId
        ?.let { id -> phraseSets.firstOrNull { it.id == id } }
        ?.let { editingSet ->
            RotatingPhraseSetEditDialog(
                phraseSet = editingSet,
                onClose = { editingPhraseSetId = null },
                onUpdateSet = { transform ->
                    onUpdatePhraseSet(editingSet.id, transform)
                },
                onDeleteSet = { phraseSetId ->
                    onDeletePhraseSet(phraseSetId)
                    editingPhraseSetId = null
                },
            )
        }

}
