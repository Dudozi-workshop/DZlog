package com.example.dzlog.ui.table.section

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.dzlog.domain.model.TableCellDataType
import com.example.dzlog.domain.model.TableCellState
import com.example.dzlog.domain.model.TableTemplateState
import com.example.dzlog.ui.table.CellHeaderBadges
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZTypography

/**
 * 셀 그리드의 표시/선택/인라인 편집 UI를 담당하는 stateless 섹션.
 */
@Composable
fun TableGridSection(
    templateState: TableTemplateState,
    displayTextProvider: (String) -> String,
    selectedCellId: String?,
    editingCellId: String?,
    onSelectCell: (String) -> Unit,
    onDoubleClickCell: (TableCellState) -> Unit,
    editingValue: String,
    onEditingValueChange: (String) -> Unit,
    onCommitInline: () -> Unit,
    inlineFocusRequester: FocusRequester,
    onInlineFocusLostCommit: () -> Unit
) {
    @Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val safeCols = templateState.cols.coerceAtLeast(1)
        val cellW = remember(maxWidth, safeCols) { maxWidth / safeCols }

        val minCellHeight = 48.dp
        val defaultCellHeight = 64.dp
        val maxVisibleRows = (maxHeight / minCellHeight).toInt().coerceAtLeast(1)
        val needsVerticalScroll = templateState.rows > maxVisibleRows
        val gridScrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (needsVerticalScroll) Modifier.verticalScroll(gridScrollState) else Modifier)
        ) {
            repeat(templateState.rows) { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(templateState.cols) { col ->
                        val cell = templateState.cells.firstOrNull {
                            it.rowIndex == row && it.colIndex == col
                        }

                        val isSelectedCell = cell?.cellId == selectedCellId
                        val isEditingCell = cell?.cellId == editingCellId
                        val cellBackground = if (isEditingCell) DDZColor.Success.copy(alpha = 0.2f) else DDZColor.Card

                        Box(
                            modifier = Modifier
                                .width(cellW)
                                .height(defaultCellHeight)
                                .padding(2.dp)
                                .background(cellBackground, RoundedCornerShape(8.dp))
                                .border(
                                    width = when {
                                        isEditingCell -> 2.dp
                                        isSelectedCell -> 2.dp
                                        else -> 1.dp
                                    },
                                    color = when {
                                        isEditingCell -> DDZColor.Success
                                        isSelectedCell -> DDZColor.Primary
                                        else -> DDZColor.Border
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .combinedClickable(
                                    enabled = (cell != null && !isEditingCell),
                                    onClick = {
                                        if (cell == null) return@combinedClickable
                                        onSelectCell(cell.cellId)
                                    },
                                    onDoubleClick = {
                                        if (cell == null) return@combinedClickable
                                        onDoubleClickCell(cell)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cell != null) {
                                val display = displayTextProvider(cell.cellId)

                                val canInlineEdit =
                                    (cell.dataType == TableCellDataType.TEXT ||
                                        cell.dataType == TableCellDataType.NUMBER ||
                                        cell.dataType == TableCellDataType.COUNTER)

                                if (isEditingCell && canInlineEdit) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) { CellHeaderBadges(cell) }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val keyboardType = when (cell.dataType) {
                                                TableCellDataType.NUMBER -> KeyboardType.Decimal
                                                TableCellDataType.COUNTER -> KeyboardType.Number
                                                else -> KeyboardType.Text
                                            }

                                            var hasEverFocused by remember(cell.cellId) {
                                                mutableStateOf(false)
                                            }

                                            BasicTextField(
                                                value = editingValue,
                                                onValueChange = onEditingValueChange,
                                                singleLine = true,
                                                textStyle = DDZTypography.Caption.copy(color = DDZColor.TextPrimary),
                                                cursorBrush = SolidColor(DDZColor.Success),
                                                keyboardOptions = KeyboardOptions(
                                                    keyboardType = keyboardType,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(onDone = { onCommitInline() }),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                                                    .focusRequester(inlineFocusRequester)
                                                    .onFocusChanged { state ->
                                                        if (state.isFocused) {
                                                            if (!hasEverFocused) {
                                                                hasEverFocused = true
                                                            }
                                                        } else if (hasEverFocused) {
                                                            onInlineFocusLostCommit()
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(14.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) { CellHeaderBadges(cell) }

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(display, style = DDZTypography.Caption, color = DDZColor.TextPrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
