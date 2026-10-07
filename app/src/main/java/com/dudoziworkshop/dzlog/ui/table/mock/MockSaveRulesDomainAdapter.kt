package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_UI_MAX_COUNT
import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.HourSystem
import com.dudoziworkshop.dzlog.domain.model.TimeFormatOptions
import com.dudoziworkshop.dzlog.domain.model.TimeSeparator
import com.dudoziworkshop.dzlog.feature.table.editor.withDataType
import java.util.UUID

internal fun mockCellsFromTemplate(templateState: TableTemplateState): List<TableEditorCellUiModel> =
    templateState.cells
        .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))
        .mapIndexed { index, cell ->
            TableEditorCellUiModel(
                id = index,
                domainCellId = cell.cellId,
                value = when (cell.dataType) {
                    TableCellDataType.COUNTER ->
                        (cell.typedValue as? CellValue.CounterSeed)?.start?.toString() ?: "1"
                    TableCellDataType.DATE -> when (cell.formatPattern.ifBlank { "yyyyMMdd" }) {
                        "yyMMdd" -> "날짜(yyMMdd)"
                        "MMdd" -> "날짜(MMdd)"
                        else -> "날짜(yyyyMMdd)"
                    }
                    TableCellDataType.TIME -> "시간(HHmm)"
                    TableCellDataType.ROTATING_TEXT -> templateState.phraseSets
                        .firstOrNull { it.id == cell.phraseSetId }
                        ?.items
                        ?.firstOrNull()
                        .orEmpty()
                        .ifBlank { "순환문구" }
                    else -> cell.rawText
                },
                type = cell.dataType.toTableEditorCellType(),
                formatPattern = cell.formatPattern,
                phraseSetId = cell.phraseSetId,
                everyOverride = cell.everyOverride,
                rowIndex = cell.rowIndex,
                colIndex = cell.colIndex,
                rowSpan = cell.rowSpan,
                colSpan = cell.colSpan,
                isCovered = com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
                    .isCoveredCell(templateState.cells, cell.rowIndex, cell.colIndex),
            )
        }


internal fun applyTableEditorCellUiModelValue(
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

internal fun applyTableEditorCellType(
    templateState: TableTemplateState,
    domainCellId: String,
    nextType: TableEditorCellType,
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

private fun TableEditorCellType.toDomainDataType(): TableCellDataType =
    when (this) {
        TableEditorCellType.TEXT -> TableCellDataType.TEXT
        TableEditorCellType.NUMBER -> TableCellDataType.NUMBER
        TableEditorCellType.COUNTER -> TableCellDataType.COUNTER
        TableEditorCellType.DATE -> TableCellDataType.DATE
        TableEditorCellType.TIME -> TableCellDataType.TIME
        TableEditorCellType.ROTATING_TEXT -> TableCellDataType.ROTATING_TEXT
    }


internal fun applyMockDatePattern(
    templateState: TableTemplateState,
    domainCellId: String,
    pattern: String,
): TableTemplateState {
    val normalized = pattern.takeIf { it in setOf("yyyyMMdd", "yyMMdd", "MMdd") } ?: "yyyyMMdd"
    return templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId == domainCellId && cell.dataType == TableCellDataType.DATE) {
                cell.copy(formatPattern = normalized)
            } else {
                cell
            }
        }
    )
}

internal fun applyMockTimeFormatPolicy(
    templateState: TableTemplateState,
    domainCellId: String,
): TableTemplateState =
    templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId == domainCellId && cell.dataType == TableCellDataType.TIME) {
                cell.copy(
                    timeFormatOptions = TimeFormatOptions(
                        hourSystem = HourSystem.H24,
                        includeSeconds = false,
                        separator = TimeSeparator.NONE,
                    ),
                    formatPattern = "HHmm",
                )
            } else {
                cell
            }
        }
    )

internal fun applyMockPhraseSet(
    templateState: TableTemplateState,
    domainCellId: String,
    phraseSetId: String?,
): TableTemplateState {
    val safeId = phraseSetId?.takeIf { id -> templateState.phraseSets.any { it.id == id } }
    return templateState.copy(
        cells = templateState.cells.map { cell ->
            if (cell.cellId == domainCellId && cell.dataType == TableCellDataType.ROTATING_TEXT) {
                if (safeId == null) {
                    cell.copy(phraseSetId = null, everyOverride = null)
                } else {
                    cell.copy(phraseSetId = safeId)
                }
            } else {
                cell
            }
        }
    )
}

internal fun applyMockPhraseEvery(
    templateState: TableTemplateState,
    domainCellId: String,
    every: Int,
): TableTemplateState =
    templateState.copy(
        cells = templateState.cells.map { cell ->
            if (
                cell.cellId == domainCellId &&
                cell.dataType == TableCellDataType.ROTATING_TEXT &&
                cell.phraseSetId != null
            ) {
                cell.copy(everyOverride = every.coerceAtLeast(1))
            } else {
                cell
            }
        }
    )


internal fun createMockPhraseSet(
    templateState: TableTemplateState,
    name: String,
    id: String = UUID.randomUUID().toString(),
): TableTemplateState {
    val normalized = name.trim()
    if (normalized.isBlank()) return templateState
    val created = RotatingPhraseSet(
        id = id,
        name = normalized,
        items = emptyList(),
        defaultEvery = 1,
    )
    return templateState.copy(phraseSets = templateState.phraseSets + created)
}

internal fun updateMockPhraseSet(
    templateState: TableTemplateState,
    phraseSetId: String,
    transform: (RotatingPhraseSet) -> RotatingPhraseSet,
): TableTemplateState =
    templateState.copy(
        phraseSets = templateState.phraseSets.map { set ->
            if (set.id == phraseSetId) transform(set) else set
        }
    )

internal fun deleteMockPhraseSet(
    templateState: TableTemplateState,
    phraseSetId: String,
): TableTemplateState =
    templateState.copy(
        phraseSets = templateState.phraseSets.filterNot { it.id == phraseSetId },
        cells = templateState.cells.map { cell ->
            if (cell.phraseSetId == phraseSetId) {
                cell.copy(phraseSetId = null, everyOverride = null)
            } else {
                cell
            }
        }
    )

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
    val pathDrafts = List(PATH_SLOT_UI_MAX_COUNT) { index ->
        draft.pathItems.getOrNull(index).toDomainDraft()
    }

    return templateState.copy(
        fileNameSlotDrafts = fileNameDrafts,
        pathSlotDrafts = pathDrafts,
    )
}

private fun TableCellDataType.toTableEditorCellType(): TableEditorCellType =
    when (this) {
        TableCellDataType.TEXT -> TableEditorCellType.TEXT
        TableCellDataType.NUMBER -> TableEditorCellType.NUMBER
        TableCellDataType.COUNTER -> TableEditorCellType.COUNTER
        TableCellDataType.DATE -> TableEditorCellType.DATE
        TableCellDataType.TIME -> TableEditorCellType.TIME
        TableCellDataType.ROTATING_TEXT -> TableEditorCellType.ROTATING_TEXT
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
