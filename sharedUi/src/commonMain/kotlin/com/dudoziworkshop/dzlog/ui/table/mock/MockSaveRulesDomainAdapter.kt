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
import com.dudoziworkshop.dzlog.domain.model.newTableElementId

fun mockCellsFromTemplate(
    templateState: TableTemplateState,
    resolvedTextByCellId: Map<String, String> = emptyMap(),
): List<TableEditorCellUiModel> =
    templateState.cells
        .sortedWith(compareBy({ it.rowIndex }, { it.colIndex }))
        .mapIndexed { index, cell ->
            TableEditorCellUiModel(
                id = index,
                domainCellId = cell.cellId,
                previewValue = resolvedTextByCellId[cell.cellId],
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
                phraseIndex = cell.phraseIndex,
                phraseCompletedShots = cell.phraseCompletedShots,
                rowIndex = cell.rowIndex,
                colIndex = cell.colIndex,
                rowSpan = cell.rowSpan,
                colSpan = cell.colSpan,
                isCovered = com.dudoziworkshop.dzlog.feature.table.editor.TableStructureRangeActions
                    .isCoveredCell(templateState.cells, cell.rowIndex, cell.colIndex),
            )
        }


fun applyTableEditorCellUiModelValue(
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

fun applyTableEditorCellType(
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


fun applyMockDatePattern(
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

fun applyMockTimeFormatPolicy(
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

fun applyMockPhraseSet(
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
                    if (cell.phraseSetId == safeId) cell else
                        cell.copy(phraseSetId = safeId, phraseIndex = 0, phraseCompletedShots = 0)
                }
            } else {
                cell
            }
        }
    )
}

fun applyMockPhraseEvery(
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
                cell.copy(everyOverride = every.coerceAtLeast(1), phraseCompletedShots = 0)
            } else {
                cell
            }
        }
    )


fun createMockPhraseSet(
    templateState: TableTemplateState,
    name: String,
    id: String = newTableElementId(),
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

fun updateMockPhraseSet(
    templateState: TableTemplateState,
    phraseSetId: String,
    transform: (RotatingPhraseSet) -> RotatingPhraseSet,
): TableTemplateState =
    templateState.let { state ->
        val old = state.phraseSets.firstOrNull { it.id == phraseSetId } ?: return@let state
        val updated = transform(old)
        state.copy(
            phraseSets = state.phraseSets.map { if (it.id == phraseSetId) updated else it },
            cells = state.cells.map { cell ->
                if (cell.phraseSetId == phraseSetId && old.items != updated.items)
                    cell.copy(phraseIndex = 0, phraseCompletedShots = 0)
                else if (cell.phraseSetId == phraseSetId && old.defaultEvery != updated.defaultEvery && cell.everyOverride == null)
                    cell.copy(phraseCompletedShots = 0)
                else cell
            },
        )
    }

fun deleteMockPhraseSet(
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

fun mockSaveRulesDraftFromTemplate(
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

fun applyMockSaveRulesDraft(
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
            "DATE" -> MockRuleItem(
                sourceType = MockRuleSourceType.DATE,
                value = dateFormatExample(draft.formatPattern),
                formatPattern = draft.formatPattern ?: "yyyyMMdd",
            )
            "TIME" -> MockRuleItem(
                sourceType = MockRuleSourceType.TIME,
                value = timeFormatExample(draft.formatPattern),
                formatPattern = draft.formatPattern ?: "HHmm",
            )
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
            formatPattern = item.formatPattern ?: "yyyyMMdd",
        )
        MockRuleSourceType.TIME -> TableEditorSlotDraft(
            kind = "FORMAT",
            label = "시간",
            formatType = "TIME",
            formatPattern = item.formatPattern ?: "HHmm",
        )
        MockRuleSourceType.ROTATING_TEXT -> TableEditorSlotDraft(
            kind = "FORMAT",
            label = "순환문구",
            formatType = "ROTATING_TEXT",
        )
    }
}


private fun dateFormatExample(pattern: String?): String =
    runCatching {
        editorDateExample(pattern?.takeIf { it.isNotBlank() } ?: "yyyyMMdd")
    }.getOrElse { "" }

private fun timeFormatExample(pattern: String?): String =
    when (pattern) {
        "HH-mm" -> "09-30"
        else -> "0930"
    }
