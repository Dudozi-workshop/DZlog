package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.TableMergeDecision
import com.dudoziworkshop.dzlog.feature.table.render.TableLayoutCalculator

internal object TableEditorV2StructureController {

    fun addRow(
        templateState: TableTemplateState,
        selection: MockLayoutSelection,
    ): TableTemplateState =
        addMockLayoutRow(templateState, selection)

    fun addColumn(
        templateState: TableTemplateState,
        selection: MockLayoutSelection,
    ): TableTemplateState =
        addMockLayoutColumn(templateState, selection)

    fun removeRows(
        templateState: TableTemplateState,
        selection: MockLayoutSelection,
    ): TableTemplateState =
        removeMockLayoutRows(templateState, selection)

    fun removeColumns(
        templateState: TableTemplateState,
        selection: MockLayoutSelection,
    ): TableTemplateState =
        removeMockLayoutColumns(templateState, selection)

    // Equalization changes only the internal distribution, never cell/merge data or style.
    fun equalizeRows(templateState: TableTemplateState): TableTemplateState {
        val weights = TableLayoutCalculator.resolveWeights(templateState.rowWeights, templateState.rows)
        return if (weights.distinct().size <= 1) templateState
        else templateState.copy(rowWeights = List(weights.size) { weights.average().toFloat() })
    }

    fun equalizeColumns(templateState: TableTemplateState): TableTemplateState {
        val weights = TableLayoutCalculator.resolveWeights(templateState.colWeights, templateState.cols)
        return if (weights.distinct().size <= 1) templateState
        else templateState.copy(colWeights = List(weights.size) { weights.average().toFloat() })
    }

    fun resolveMergeDecision(
        templateState: TableTemplateState,
        selection: MockLayoutSelection,
    ): TableMergeDecision =
        resolveMockLayoutMergeDecision(
            templateState = templateState,
            selection = selection,
            populatedCellIds = templateState.cells
                .filter(::hasMeaningfulMergeContent)
                .map { it.cellId }
                .toSet(),
        )

    fun applyMergeDecision(
        templateState: TableTemplateState,
        decision: TableMergeDecision,
    ): TableTemplateState =
        applyMockLayoutMergeDecision(templateState, decision)

    private fun hasMeaningfulMergeContent(cell: com.dudoziworkshop.dzlog.domain.model.TableCellState): Boolean {
        if (cell.rawText.isNotBlank()) return true
        if (cell.dataType != TableCellDataType.TEXT) return true
        if (cell.phraseSetId != null) return true
        if (cell.formatPattern.isNotBlank()) return true
        return false
    }
}
