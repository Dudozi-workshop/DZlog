package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloseFullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.feature.help.ui.LocalTutorialReadOnly
import com.dudoziworkshop.dzlog.feature.table.editor.TableEditorTextFavoritesViewModel
import com.dudoziworkshop.dzlog.ui.common.DDZButton
import com.dudoziworkshop.dzlog.ui.common.DDZButtonStyle
import com.dudoziworkshop.dzlog.ui.common.DDZTextField
import com.dudoziworkshop.dzlog.ui.common.input.decimalInputOrPrevious
import com.dudoziworkshop.dzlog.ui.table.rotating.RotatingPhraseSetEditDialog
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography
import com.dudoziworkshop.dzlog.ui.theme.DDZLayout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TableEditorCellUiModelEditor(
    cell: TableEditorCellUiModel,
    phraseSets: List<com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet>,
    onValueChange: (String) -> Unit,
    textFavoritesModel: TableEditorTextFavoritesViewModel,
    onDatePatternChange: (String) -> Unit,
    onApplyTimePolicy: () -> Unit,
    onPhraseSetChange: (String?) -> Unit,
    onPhraseEveryChange: (Int) -> Unit,
    onPhraseFollowSet: () -> Unit,
    onPhraseProgressChange: (Int, Int) -> Unit,
    onCreatePhraseSet: (String) -> String?,
    onUpdatePhraseSet: (String, (com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet) -> Unit,
    onDeletePhraseSet: (String) -> Unit,
    compactInput: Boolean = false,
    compactFavoritesExpanded: Boolean = false,
    onCompactFavoritesExpandedChange: (Boolean) -> Unit = {},
    inputExpanded: Boolean,
    expandedInputHeight: Dp,
    onToggleInputExpanded: () -> Unit,
    onInputFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteActionsEnabled = !LocalTutorialReadOnly.current
    var showDatePicker by remember(cell.id) { mutableStateOf(false) }
    var showPhrasePicker by remember(cell.id) { mutableStateOf(false) }
    var showPhraseProgress by remember(cell.id) { mutableStateOf(false) }
    var editingPhraseSetId by remember(cell.id) { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalTextStyle provides DDZTypography.Body) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(DDZColor.Surface)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    when (cell.type) {
                        TableEditorCellType.TEXT,
                        TableEditorCellType.NUMBER -> {
                            val isText = cell.type == TableEditorCellType.TEXT
                            var textDraft by remember(cell.id) {
                                mutableStateOf(TextFieldValue(cell.value, TextRange(cell.value.length)))
                            }
                            LaunchedEffect(cell.id, cell.value) {
                                if (isText && textDraft.text != cell.value) {
                                    textDraft = TextFieldValue(cell.value, TextRange(cell.value.length))
                                }
                            }
                            Box(Modifier.fillMaxWidth()) {
                                if (isText) {
                                    DDZTextField(
                                        modifier = Modifier.fillMaxWidth()
                                            .height(if (inputExpanded) expandedInputHeight else DDZLayout.QuickInput.RowHeight)
                                            .onFocusChanged { if (it.isFocused) onInputFocus() },
                                        value = textDraft,
                                        onValueChange = { next ->
                                            textDraft = next
                                            onValueChange(next.text)
                                        },
                                        label = "값",
                                        singleLine = false,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                        trailingIcon = {
                                            androidx.compose.foundation.layout.Spacer(
                                                Modifier.width(DDZLayout.QuickInput.Touch * 2)
                                            )
                                        },
                                    )
                                } else {
                                    DDZTextField(
                                        modifier = Modifier.fillMaxWidth()
                                            .onFocusChanged { if (it.isFocused) onInputFocus() },
                                        value = cell.value,
                                        onValueChange = { next ->
                                            onValueChange(decimalInputOrPrevious(next, cell.value))
                                        },
                                        label = "값",
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        trailingIcon = {
                                            androidx.compose.material3.TextButton(onClick = {
                                                onValueChange(if (cell.value.startsWith("-")) cell.value.drop(1) else "-${cell.value}")
                                            }) { Text("±") }
                                        },
                                    )
                                }
                                if (isText) Row(
                                    modifier = Modifier.align(Alignment.BottomEnd),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    TableEditorTextFavoriteStar(cell.value, textFavoritesModel, enabled = favoriteActionsEnabled)
                                    IconButton(
                                        onClick = onToggleInputExpanded,
                                        modifier = Modifier.size(DDZLayout.QuickInput.Touch),
                                    ) {
                                        Icon(
                                            if (inputExpanded) Icons.Filled.CloseFullscreen else Icons.Filled.OpenInFull,
                                            if (inputExpanded) "입력창 축소" else "입력창 확대",
                                            tint = DDZColor.TextMuted, modifier = Modifier.size(DDZLayout.QuickInput.Icon),
                                        )
                                    }
                                }
                            }
                        }

                        TableEditorCellType.COUNTER -> {
                            val currentCounter = cell.value.toIntOrNull()?.coerceAtLeast(0) ?: 1
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
                                    modifier = Modifier.weight(1f)
                                        .onFocusChanged { if (it.isFocused) onInputFocus() },
                                    value = currentCounter.toString(),
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.isNotBlank()) {
                                            onValueChange(digits)
                                        }
                                    },
                                    label = "시작 번호",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                                DDZButton(
                                    text = "+",
                                    style = DDZButtonStyle.Secondary,
                                    minHeight = 40.dp,
                                    onClick = { onValueChange((currentCounter + 1).toString()) },
                                )
                            }
                        }

                        TableEditorCellType.DATE -> {
                            Text(
                                text = (cell.previewValue ?: cell.value),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        TableEditorCellType.TIME,
                        TableEditorCellType.ROTATING_TEXT -> Unit
                    }
                }
            }

            // Common cell controls live in the fixed bottom row; only details scroll here.
            if (!compactInput || cell.type == TableEditorCellType.TEXT) Column(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (cell.type) {
                    TableEditorCellType.TEXT -> TableEditorTextFavoritesRow(
                        model = textFavoritesModel,
                        onApply = onValueChange,
                        enabled = favoriteActionsEnabled,
                        compact = compactInput,
                        compactExpanded = compactFavoritesExpanded,
                        onCompactExpandedChange = onCompactFavoritesExpandedChange,
                    )
                    TableEditorCellType.COUNTER -> Text(
                        "촬영 성공 후 다음 번호로 증가합니다.", color = DDZColor.TextMuted,
                        style = DDZTypography.Caption,
                    )
                    TableEditorCellType.DATE -> TableEditorSettingRow(
                        label = "날짜 형식",
                        value = tableEditorDateFormatLabel(cell.formatPattern),
                        onClick = { showDatePicker = true },
                    )
                    TableEditorCellType.TIME -> TableEditorSettingRow(
                        label = "시간 형식",
                        value = "HHmm · 분 단위 고정",
                        onClick = onApplyTimePolicy,
                    )
                    TableEditorCellType.ROTATING_TEXT -> {
                        val selectedSet = phraseSets.firstOrNull { it.id == cell.phraseSetId }
                        TableEditorCompactSettingRow(
                            label = "문구 세트",
                            value = selectedSet?.name ?: "선택 안 함",
                            onClick = { showPhrasePicker = true },
                        )
                        if (selectedSet != null) {
                            HorizontalDivider(thickness = 1.dp, color = DDZColor.Border)
                            val index = cell.phraseIndex.coerceIn(0, (selectedSet.items.size - 1).coerceAtLeast(0))
                            val phraseOverride = selectedSet.itemEveryOverrides.getOrNull(index)
                            val everyValue = (cell.everyOverride ?: selectedSet.everyForItem(index))
                                .coerceAtLeast(1)
                            TableEditorPhrasePeriodRow(
                                every = everyValue,
                                overridden = phraseOverride != null || cell.everyOverride != null,
                                onEveryChange = onPhraseEveryChange,
                                onFollowSet = onPhraseFollowSet,
                            )
                            if (selectedSet.items.isNotEmpty()) {
                                val completed = cell.phraseCompletedShots.coerceIn(0, everyValue - 1)
                                HorizontalDivider(thickness = 1.dp, color = DDZColor.Border)
                                TableEditorCompactSettingRow(
                                    label = "문구 진행 상태",
                                    value = "${selectedSet.items[index]} · ${completed}/${everyValue}장",
                                    onClick = { showPhraseProgress = true },
                                )
                            }
                        }
                    }
                    else -> Unit
                }

            }
        }

    }

    if (showPhraseProgress) {
        phraseSets.firstOrNull { it.id == cell.phraseSetId && it.items.isNotEmpty() }?.let { set ->
            TableEditorPhraseProgressDialog(cell, set, onPhraseProgressChange, { showPhraseProgress = false })
        }
    }

    if (showDatePicker) {
        TableEditorDateFormatSheet(
            selectedPattern = cell.formatPattern,
            onDismiss = { showDatePicker = false },
            onSelect = { pattern ->
                onDatePatternChange(pattern)
                showDatePicker = false
            },
        )
    }

    if (showPhrasePicker) {
        TableEditorPhraseSetSheet(
            cellId = cell.id,
            phraseSets = phraseSets,
            selectedSetId = cell.phraseSetId,
            onSelect = { id ->
                onPhraseSetChange(id)
                showPhrasePicker = false
            },
            onEdit = { id ->
                editingPhraseSetId = id
                showPhrasePicker = false
            },
            onCreate = { name ->
                onCreatePhraseSet(name)?.let { createdSetId ->
                    showPhrasePicker = false
                    editingPhraseSetId = createdSetId
                }
            },
            onDismiss = { showPhrasePicker = false },
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

