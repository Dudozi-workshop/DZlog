package com.dudoziworkshop.dzlog.ui.table.mock

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.data.template.tableTemplateStateFromJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.preview.*
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import com.dudoziworkshop.dzlog.domain.time.CaptureTime
import com.dudoziworkshop.dzlog.feature.table.policy.applyPhrasePeriodResolutions
import com.dudoziworkshop.dzlog.feature.table.policy.quickPhrasePeriodConflicts
import org.junit.Assert.*
import org.junit.Test

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [33])
class TableEditorPhraseSettingsTest {
    private val set = RotatingPhraseSet("set", "처리구", listOf("T1", "T2", "T3", "T4"), 12)
    private val cell = TableCellState(0, 0, cellId = "cell", dataType = TableCellDataType.ROTATING_TEXT,
        phraseSetId = set.id, phraseIndex = 2, phraseCompletedShots = 5)
    private val template = TableTemplateState(1, 1, listOf(cell), phraseSets = listOf(set))

    @Test fun period_change_keeps_item_and_its_completed_count() {
        val next = applyMockPhraseEvery(template, cell.cellId, 6).cells.single()
        assertEquals(2, next.phraseIndex)
        assertEquals(5, next.phraseCompletedShots)
        assertEquals(6, next.everyOverride)
    }

    @Test fun selecting_same_set_and_renaming_preserve_progress() {
        assertEquals(template, applyMockPhraseSet(template, cell.cellId, set.id))
        val renamed = updateMockPhraseSet(template, set.id) { it.copy(name = "이름 변경") }
        assertEquals(cell, renamed.cells.single())
    }

    @Test fun appendOnlyKeepsExistingCameraPhraseAndCapturedCount() {
        val appended = updateMockPhraseSet(template, set.id) {
            com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy.addItem(it, "T5")
        }
        assertEquals(listOf("T1", "T2", "T3", "T4", "T5"), appended.phraseSets.single().items)
        assertEquals(cell, appended.cells.single())
        assertEquals(2, appended.cells.single().phraseIndex)
        assertEquals(5, appended.cells.single().phraseCompletedShots)

        val renamed = updateMockPhraseSet(appended, set.id) {
            com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy.renameItem(it, 0, "T1 수정")
        }
        assertEquals(0, renamed.cells.single().phraseIndex)
        assertEquals(0, renamed.cells.single().phraseCompletedShots)
    }

    @Test fun different_set_and_changed_item_order_start_at_first_item() {
        val other = set.copy(id = "other")
        val changed = applyMockPhraseSet(template.copy(phraseSets = listOf(set, other)), cell.cellId, other.id)
        assertEquals(0, changed.cells.single().phraseIndex)
        assertEquals(0, changed.cells.single().phraseCompletedShots)
        val reordered = updateMockPhraseSet(template, set.id) { it.copy(items = it.items.reversed()) }
        assertEquals(0, reordered.cells.single().phraseIndex)
        assertEquals(0, reordered.cells.single().phraseCompletedShots)
        assertEquals(cell, template.cells.single())
    }
    @Test fun replacement_updates_shared_set_and_resets_related_progress_only() {
        val companion = cell.copy(cellId = "second", colIndex = 1, phraseIndex = 1, phraseCompletedShots = 2)
        val unrelatedSet = RotatingPhraseSet("other", "다른 세트", listOf("X", "Y"))
        val unrelated = cell.copy(cellId = "unrelated", colIndex = 2, phraseSetId = "other")
        val counter = TableCellState(0, 3, cellId = "counter", dataType = TableCellDataType.COUNTER,
            typedValue = CellValue.CounterSeed(11))
        val shared = TableTemplateState(1, 4, listOf(cell, companion, unrelated, counter),
            phraseSets = listOf(set, unrelatedSet))
        val updated = updateMockPhraseSet(shared, set.id) { it.copy(items = listOf("초기 조사", "최종 조사")) }
        assertEquals(listOf("초기 조사", "최종 조사"), updated.phraseSets[0].items)
        assertEquals(set.counterProgressMode, updated.phraseSets[0].counterProgressMode)
        assertEquals(set.defaultEvery, updated.phraseSets[0].defaultEvery)
        assertEquals(unrelatedSet, updated.phraseSets[1])
        assertEquals(0, updated.cells[0].phraseIndex)
        assertEquals(0, updated.cells[0].phraseCompletedShots)
        assertEquals(0, updated.cells[1].phraseIndex)
        assertEquals(0, updated.cells[1].phraseCompletedShots)
        assertEquals(unrelated, updated.cells[2])
        assertEquals(counter, updated.cells[3])
    }

    @Test fun editingOnePhraseCycleNeverChangesTheSetDefaultOrOtherPhrases() {
        val changed = updateMockPhraseSet(template, set.id) {
            com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy.setEveryForItems(
                it, listOf(2), 3,
            )
        }
        val updatedSet = changed.phraseSets.single()
        assertEquals(12, updatedSet.defaultEvery)
        assertEquals(listOf(12, 12, 3, 12), updatedSet.items.indices.map(updatedSet::everyForItem))
        assertEquals(listOf(null, null, 3, null), updatedSet.itemEveryOverrides)
        assertEquals(cell, changed.cells.single())
        assertEquals(12, template.phraseSets.single().defaultEvery)
    }

    @Test fun perItemPeriodUpdatesPreserveProgressUntilConflictIsResolved() {
        val inheritedCurrent = cell.copy(cellId = "inherited-current", colIndex = 0,
            phraseIndex = 2, phraseCompletedShots = 5, everyOverride = null)
        val inheritedOther = cell.copy(cellId = "inherited-other", colIndex = 1,
            phraseIndex = 1, phraseCompletedShots = 3, everyOverride = null)
        val fixedCurrent = cell.copy(cellId = "fixed-current", colIndex = 2,
            phraseIndex = 2, phraseCompletedShots = 2, everyOverride = 4)
        val working = TableTemplateState(1, 3,
            listOf(inheritedCurrent, inheritedOther, fixedCurrent), phraseSets = listOf(set))
        val changed = updateMockPhraseSet(working, set.id) {
            com.dudoziworkshop.dzlog.domain.phrase.RotatingPhraseSetItemPolicy.setEvery(it, 2, 3)
        }
        assertEquals(5, changed.cells[0].phraseCompletedShots)
        assertEquals(2, changed.cells[0].phraseIndex)
        assertEquals(1, com.dudoziworkshop.dzlog.feature.table.policy.quickPhrasePeriodConflicts(
            working, changed).size)
        assertEquals(inheritedOther, changed.cells[1])
        assertEquals(fixedCurrent, changed.cells[2])
        assertEquals(3, changed.phraseSets.single().everyForItem(2))
        assertEquals(12, changed.phraseSets.single().everyForItem(1))
    }

    @Test fun resolvedSetPeriodReachesTheSavedEditorPreviewAndNextCapture() {
        val source = template.copy(fileNameSlotDrafts = listOf(TableEditorSlotDraft("CELL", "처리구", cellId = cell.cellId)))
        val proposed = updateMockPhraseSet(source, set.id) { it.copy(defaultEvery = 3) }
        val conflicts = quickPhrasePeriodConflicts(source, proposed)
        assertEquals(listOf(cell.cellId), conflicts.map { it.cellId })
        assertNull(applyPhrasePeriodResolutions(proposed, conflicts, emptyMap()))
        assertEquals(5, source.cells.single().phraseCompletedShots)

        val resolved = requireNotNull(applyPhrasePeriodResolutions(proposed, conflicts,
            mapOf(cell.cellId to CellPhraseProgress(index = 3, completedShots = 2))))
        val saved = requireNotNull(tableTemplateStateFromJson(resolved.toJsonString()))
        val time = CaptureTime(0, "en-US", "UTC")
        val input = PreviewInput(saved, time, 4, "yyyyMMdd", "HHmm", "_", true, true, SaveMode.BOTH, 12, 0)
        val preview = buildPreview(input)
        val scope = buildScope(CaptureScopeInput(saved, time, 4, "yyyyMMdd", "HHmm", "_", true, SaveMode.BOTH, 0))
        val capture = buildCapturePreview(scope,
            FinalCapturePreviewInput(saved, time, 4, "yyyyMMdd", "HHmm", "_", true, true, SaveMode.BOTH, 12, 0))
        assertEquals("T4", preview.plan.resolvedCells.single().resolvedText)
        assertEquals("T4", capture.resolvedCells.single().resolvedText)
        assertEquals(preview.previewNaming.displayName, capture.displayName)
        assertTrue(capture.displayName.contains("T4"))
        val next = saved.applyPatch(capture.tablePatch)
        assertEquals("T1", buildPreview(input.copy(templateState = next)).plan.resolvedCells.single().resolvedText)
        assertEquals(0, next.cells.single().phraseCompletedShots)
    }
}
