package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_UI_MAX_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.editor.withDataType

internal fun mockCellsFromTemplate(templateState: TableTemplateState): List<MockCell> =
    templateState.cells
        .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))
        .mapIndexed { index, cell ->
            MockCell(
                id = index,
                domainCellId = cell.cellId,
                value = cell.rawText,
                type = cell.dataType.toMockCellType(),
            )
        }


internal fun applyMockCellValue(
    templateState: TableTemplateState,
    domainCellId: String,
    nextValue: String,
): TableTemplateState =
    templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId != domainCellId) {
                cell
            } else {
                when (cell.dataType) {
                    TableCellDataType.TEXT -> cell.copy(
                        rawText = nextValue,
                        typedValue = CellValue.Text(nextValue),
                    )
                    TableCellDataType.NUMBER -> cell.copy(
                        rawText = nextValue,
                        typedValue = CellValue.Number(nextValue),
                    )
                    TableCellDataType.COUNTER -> {
                        val parsed = nextValue.trim().toIntOrNull()
                        if (parsed == null || parsed < 0) {
                            cell
                        } else {
                            cell.copy(
                                rawText = nextValue,
                                typedValue = CellValue.CounterSeed(parsed),
                            )
                        }
                    }
                    TableCellDataType.DATE,
                    TableCellDataType.TIME,
                    TableCellDataType.ROTATING_TEXT -> cell
                }
            }
        }
    )

internal fun applyMockCellType(
    templateState: TableTemplateState,
    domainCellId: String,
    nextType: MockCellType,
): TableTemplateState =
    templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId == domainCellId) {
                cell.withDataType(nextType.toDomainDataType())
            } else {
                cell
            }
        }
    )

private fun MockCellType.toDomainDataType(): TableCellDataType =
    when (this) {
        MockCellType.TEXT -> TableCellDataType.TEXT
        MockCellType.NUMBER -> TableCellDataType.NUMBER
        MockCellType.COUNTER -> TableCellDataType.COUNTER
        MockCellType.DATE -> TableCellDataType.DATE
        MockCellType.TIME -> TableCellDataType.TIME
        MockCellType.ROTATING_TEXT -> TableCellDataType.ROTATING_TEXT
    }

internal fun mockSaveRulesDraftFromTemplate(
    templateState: TableTemplateState,
    includePathInScope: Boolean,
    includeFilenameInScope: Boolean,
): MockSaveRulesDraft {
    val fileNameItems = List(FILE_NAME_SLOT_COUNT) { index ->
        templateState.fileNameSlotDrafts
            .getOrNull(index)
            .toMockRuleItem(templateState)
    }
    val pathItems = List(PATH_SLOT_UI_MAX_COUNT) { index ->
        templateState.pathSlotDrafts
            .getOrNull(index)
            .toMockRuleItem(templateState)
    }
    return MockSaveRulesDraft(
        fileNameItems = fileNameItems,
        pathItems = pathItems,
        includePathInScope = includePathInScope,
        includeFilenameInScope = includeFilenameInScope,
    )
}

internal fun applyMockSaveRulesDraft(
    templateState: TableTemplateState,
    draft: MockSaveRulesDraft,
): TableTemplateState {
    val fileNameDrafts = List(FILE_NAME_SLOT_COUNT) { index ->
        draft.fileNameItems.getOrNull(index).toDomainDraft()
    }
    val visiblePathDrafts = List(PATH_SLOT_UI_MAX_COUNT) { index ->
        draft.pathItems.getOrNull(index).toDomainDraft()
    }
    val preservedPathTail = templateState.pathSlotDrafts.drop(PATH_SLOT_UI_MAX_COUNT)

    return templateState.copy(
        fileNameSlotDrafts = fileNameDrafts,
        pathSlotDrafts = visiblePathDrafts + preservedPathTail,
    )
}

private fun TableCellDataType.toMockCellType(): MockCellType =
    when (this) {
        TableCellDataType.TEXT -> MockCellType.TEXT
        TableCellDataType.NUMBER -> MockCellType.NUMBER
        TableCellDataType.COUNTER -> MockCellType.COUNTER
        TableCellDataType.DATE -> MockCellType.DATE
        TableCellDataType.TIME -> MockCellType.TIME
        TableCellDataType.ROTATING_TEXT -> MockCellType.ROTATING_TEXT
    }

private fun TableEditorSlotDraft?.toMockRuleItem(
    templateState: TableTemplateState,
): MockRuleItem? {
    val draft = this ?: return null
    return when (draft.kind.uppercase()) {
        "CELL" -> {
            val cellId = draft.cellId ?: return null
            val label = templateState.cells
                .firstOrNull { it.cellId == cellId }
                ?.rawText
                ?.trim()
                .orEmpty()
                .ifBlank { "셀" }
            MockRuleItem(
                sourceType = MockRuleSourceType.CELL,
                value = label,
                cellId = cellId,
            )
        }
        "MANUAL" -> MockRuleItem(
            sourceType = MockRuleSourceType.MANUAL,
            value = draft.manualText.orEmpty(),
        )
        "FORMAT" -> when (draft.formatType?.uppercase()) {
            "DATE" -> MockRuleItem(MockRuleSourceType.DATE, "날짜")
            "TIME" -> MockRuleItem(MockRuleSourceType.TIME, "시간")
            "ROTATING_TEXT" -> MockRuleItem(MockRuleSourceType.ROTATING_TEXT, "순환문구")
            else -> null
        }
        else -> null
    }
}

private fun MockRuleItem?.toDomainDraft(): TableEditorSlotDraft? {
    val item = this ?: return null
    return when (item.sourceType) {
        MockRuleSourceType.CELL -> item.cellId?.let { cellId ->
            TableEditorSlotDraft(
                kind = "CELL",
                label = "셀",
                cellId = cellId,
            )
        }
        MockRuleSourceType.MANUAL -> item.value
            .trim()
            .takeIf { it.isNotBlank() }
            ?.let { text ->
                TableEditorSlotDraft(
                    kind = "MANUAL",
                    label = "직접입력",
                    manualText = text,
                )
            }
        MockRuleSourceType.DATE -> TableEditorSlotDraft(
            kind = "FORMAT",
            label = "날짜",
            formatType = "DATE",
        )
        MockRuleSourceType.TIME -> TableEditorSlotDraft(
            kind = "FORMAT",
            label = "시간",
            formatType = "TIME",
        )
        MockRuleSourceType.ROTATING_TEXT -> TableEditorSlotDraft(
            kind = "FORMAT",
            label = "순환문구",
            formatType = "ROTATING_TEXT",
        )
    }
}
