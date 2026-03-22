package com.dudoziworkshop.dzlog.feature.table.editor

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.TableCounterConflictDialogState
import com.dudoziworkshop.dzlog.feature.table.policy.evaluateCounterEditConflict
import com.dudoziworkshop.dzlog.feature.table.policy.nextLowCounterWarningLatch
import com.dudoziworkshop.dzlog.feature.table.policy.openCounterConflictDialog
import com.dudoziworkshop.dzlog.feature.table.policy.parseNonNegativeInt

internal fun parseInlineCounterValue(text: String): Int? = parseNonNegativeInt(text)

data class TableEditorCounterInlineCommitInput(
    val cellId: String,
    val editingValue: String,
    val currentTemplate: TableTemplateState,
    val autoNextCounterValue: Int,
    val lowCounterWarningLatchedInSession: Boolean,
    val updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
)

data class TableEditorCounterInlineCommitResult(
    val outcome: TableEditorCounterInlineCommitOutcome,
    val nextTemplate: TableTemplateState? = null,
    val nextLowCounterWarningLatchedInSession: Boolean,
    val openedCounterConflict: TableCounterConflictDialogState? = null,
    val committedCounterSeed: Int? = null,
)

enum class TableEditorCounterInlineCommitOutcome {
    BLOCKED_INVALID,
    BLOCKED_CONFLICT,
    COMMITTED,
}

object TableEditorCounterInlineCommitResolver {

    fun resolve(input: TableEditorCounterInlineCommitInput): TableEditorCounterInlineCommitResult {
        val pendingValue = parseInlineCounterValue(input.editingValue)
            ?: return TableEditorCounterInlineCommitResult(
                outcome = TableEditorCounterInlineCommitOutcome.BLOCKED_INVALID,
                nextLowCounterWarningLatchedInSession = input.lowCounterWarningLatchedInSession,
            )

        val nextLowWarningLatch = nextLowCounterWarningLatch(
            pendingCounterCommitValue = pendingValue,
            streamNext = input.autoNextCounterValue,
        )
        val conflict = evaluateCounterEditConflict(
            newValueText = input.editingValue,
            streamNext = input.autoNextCounterValue,
            lowCounterWarningLatchedInSession = input.lowCounterWarningLatchedInSession,
        )
        if (conflict != null) {
            return TableEditorCounterInlineCommitResult(
                outcome = TableEditorCounterInlineCommitOutcome.BLOCKED_CONFLICT,
                nextLowCounterWarningLatchedInSession = nextLowWarningLatch,
                openedCounterConflict = openCounterConflictDialog(
                    editingCellId = input.cellId,
                    conflict = conflict,
                ),
            )
        }

        return TableEditorCounterInlineCommitResult(
            outcome = TableEditorCounterInlineCommitOutcome.COMMITTED,
            nextTemplate = normalizeCounterValue(
                template = input.currentTemplate,
                cellId = input.cellId,
                pendingValue = pendingValue,
                updateCell = input.updateCell,
            ),
            nextLowCounterWarningLatchedInSession = nextLowWarningLatch,
            committedCounterSeed = pendingValue.coerceAtLeast(1),
        )
    }

    private fun normalizeCounterValue(
        template: TableTemplateState,
        cellId: String,
        pendingValue: Int,
        updateCell: (TableTemplateState, String, (TableCellState) -> TableCellState) -> TableTemplateState,
    ): TableTemplateState {
        return updateCell(template, cellId) { cell ->
            cell.copy(
                rawText = pendingValue.toString(),
                typedValue = CellValue.CounterSeed(pendingValue.coerceAtLeast(0)),
            )
        }
    }
}
