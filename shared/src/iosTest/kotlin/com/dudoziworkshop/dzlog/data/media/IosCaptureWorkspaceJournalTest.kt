package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

class IosCaptureWorkspaceJournalTest {
    @Test fun photoKitSuccessWithPreferencesFailureRecoversExactlyOnceAfterRelaunch() {
        withFixture { defaults, workspace, plan ->
            val journal = IosCaptureWorkspaceJournal(defaults, "journal")
            val captured = requireNotNull(workspace.active)
            journal.stage(workspace, captured, plan)
            assertFailsWith<IllegalStateException> { journal.stage(workspace, captured, plan) }
            var current = workspace
            val receipt = plan.confirm(listOf(PersistedCaptureAsset(plan.targets.single(), "photo-12")))
            assertFalse(journal.complete(receipt, { current }, { false }))
            assertEquals(12, current.active?.nextCounter)
            assertNotNull(journal.pending())

            val reopened = IosCaptureWorkspaceJournal(defaults, "journal")
            val indexed = listOf(SavedCaptureMedia(
                plan.captureId, receipt.assets.single(), CaptureMediaAvailability.PRESENT,
            ))
            assertTrue(reopened.recover(indexed, { current }, { next -> current = next; true }))
            assertEquals(13, current.active?.nextCounter)
            assertEquals(CellValue.CounterSeed(13), current.active?.template?.templateState?.cells?.single()?.typedValue)
            assertTrue(reopened.pending() == null)
            assertTrue(reopened.recover(indexed, { current }, { false }))
            assertEquals(13, current.active?.nextCounter)
        }
    }

    @Test fun absentOrInaccessibleAssetsNeverAdvanceCounter() {
        withFixture { defaults, workspace, plan ->
            val journal = IosCaptureWorkspaceJournal(defaults, "journal")
            journal.stage(workspace, requireNotNull(workspace.active), plan)
            var current = workspace
            assertFalse(journal.recover(emptyList(), { current }, { current = it; true }))
            val asset = SavedCaptureMedia(
                plan.captureId, PersistedCaptureAsset(plan.targets.single(), "photo"),
                CaptureMediaAvailability.INACCESSIBLE,
            )
            assertFalse(journal.recover(listOf(asset), { current }, { current = it; true }))
            assertNotNull(journal.pending())
            assertEquals(12, current.active?.nextCounter)
        }
    }

    @Test fun unrelatedWorkspaceEditPreventsStaleCommit() {
        withFixture { defaults, workspace, plan ->
            val journal = IosCaptureWorkspaceJournal(defaults, "journal")
            journal.stage(workspace, requireNotNull(workspace.active), plan)
            val changed = workspace.copy(documents = workspace.documents.map { it.copy(counterPadding = 2) })
            val receipt = plan.confirm(listOf(PersistedCaptureAsset(plan.targets.single(), "photo-12")))
            assertFalse(journal.complete(receipt, { changed }, { true }))
            assertNotNull(journal.pending())
        }
    }

    private fun withFixture(block: (NSUserDefaults, EditorWorkspace, com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan) -> Unit) {
        val suite = "dzlog.capture.journal.test.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val template = SavedTableTemplate(
                "blueberry", "현장 기록",
                TableTemplateState(1, 1, listOf(
                    TableCellState(0, 0, cellId = "number", dataType = TableCellDataType.COUNTER,
                        typedValue = CellValue.CounterSeed(12)),
                )),
                TableStyleState(), 0L,
            )
            val doc = EditorDocument(template, saveMode = SaveMode.ORIGINAL_ONLY, nextCounter = 12)
            val workspace = EditorWorkspace(listOf(doc), template.id)
            val path = "Pictures/DZlog/Field/"
            val plan = buildCaptureMediaSavePlan(
                FinalCapturePreview(
                    resolvedCells = emptyList(),
                    tablePatch = TablePatch(mapOf("number" to "13")),
                    displayName = "Field_0012.jpg", usedCounter = 12,
                    nextPhraseProgressCursor = 0,
                    counterScope = CounterScope(buildCounterPath(path, SaveMode.ORIGINAL_ONLY),
                        "Field_", 12, false),
                    relativePathPreview = path,
                ),
                SaveMode.ORIGINAL_ONLY, "capture-12",
            )
            block(defaults, workspace, plan)
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }
}
