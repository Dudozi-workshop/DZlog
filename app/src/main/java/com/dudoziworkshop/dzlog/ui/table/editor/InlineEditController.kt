package com.dudoziworkshop.dzlog.ui.table.editor

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.evaluateCounterEditConflict
import com.dudoziworkshop.dzlog.feature.table.policy.openCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.parseNonNegativeInt
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState

data class CommitResult(
    val nextInlineState: InlineEditState,
    val updatedTemplateState: TableTemplateState?,
    val openedCounterConflict: TableCounterConflictDialogState?,
    val committedCounterSeed: Int?
)

fun startInlineEditing(
    state: InlineEditState,
    cellId: String,
    initialText: String
): InlineEditState {
    return state.copy(
        editingCellId = cellId,
        editingValue = initialText,
        editingOriginalValue = initialText
    )
}

fun clearInlineEditing(state: InlineEditState): InlineEditState {
    return state.copy(
        editingCellId = null,
        editingValue = "",
        editingOriginalValue = ""
    )
}

fun commitInlineEditIfNeeded(
    inlineState: InlineEditState,
    templateState: TableTemplateState,
    autoNextCounterValue: Int,
    updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState
): CommitResult {
    val id = inlineState.editingCellId ?: return CommitResult(
        nextInlineState = inlineState,
        updatedTemplateState = null,
        openedCounterConflict = null,
        committedCounterSeed = null
    )

    val cell = templateState.cells.firstOrNull { it.cellId == id }

    if (cell != null && cell.dataType == TableCellDataType.COUNTER) {
        val conflict = evaluateCounterEditConflict(
            oldValueText = inlineState.editingOriginalValue,
            newValueText = inlineState.editingValue,
            streamNext = autoNextCounterValue
        )
        if (parseNonNegativeInt(inlineState.editingValue) == null) {
            return CommitResult(
                nextInlineState = inlineState,
                updatedTemplateState = null,
                openedCounterConflict = null,
                committedCounterSeed = null
            )
        }
        if (conflict != null) {
            return CommitResult(
                nextInlineState = inlineState,
                updatedTemplateState = null,
                openedCounterConflict = openCounterConflictDialog(
                    editingCellId = id,
                    conflict = conflict
                ),
                committedCounterSeed = null
            )
        }
    }

    val target = templateState.cells.firstOrNull { it.cellId == id }
    val normalizedValueText = if (target?.dataType == TableCellDataType.COUNTER) {
        val value = parseNonNegativeInt(inlineState.editingValue)
        if (value == null) {
            return CommitResult(
                nextInlineState = clearInlineEditing(inlineState),
                updatedTemplateState = null,
                openedCounterConflict = null,
                committedCounterSeed = null
            )
        }
        value.toString()
    } else {
        inlineState.editingValue
    }

    val updated = updateCell(templateState, id) { c ->
        when (c.dataType) {
            TableCellDataType.TEXT -> c.copy(
                rawText = normalizedValueText,
                typedValue = CellValue.Text(normalizedValueText)
            )

            TableCellDataType.NUMBER -> c.copy(
                rawText = normalizedValueText,
                typedValue = CellValue.Number(normalizedValueText)
            )

            TableCellDataType.COUNTER -> {
                val seed = normalizedValueText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
                c.copy(typedValue = CellValue.CounterSeed(seed))
            }

            else -> c
        }
    }

    val committedCounterSeed = if (target?.dataType == TableCellDataType.COUNTER) {
        val seed = normalizedValueText.trim().toIntOrNull()?.coerceAtLeast(0) ?: 0
        seed.coerceAtLeast(1)
    } else {
        null
    }

    return CommitResult(
        nextInlineState = clearInlineEditing(inlineState),
        updatedTemplateState = updated,
        openedCounterConflict = null,
        committedCounterSeed = committedCounterSeed
    )
}
