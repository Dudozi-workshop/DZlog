package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.ConfirmedCaptureMediaSave
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

class IosCaptureSaveCoordinatorTest {
    private class FakeMedia(
        private val fail: Boolean = false,
    ) : CaptureMediaRepository {
        var saved = emptyList<SavedCaptureMedia>()
        override suspend fun saveAllOrRollback(
            plan: CaptureMediaSavePlan, jpegByRole: Map<CaptureMediaRole, ByteArray>,
        ): ConfirmedCaptureMediaSave {
            if (fail) error("Simulated PhotoKit failure")
            val assets = plan.targets.map { PersistedCaptureAsset(it, "asset-12") }
            saved = assets.map { SavedCaptureMedia(plan.captureId, it, CaptureMediaAvailability.PRESENT) }
            return plan.confirm(assets)
        }
        override suspend fun readAsset(assetId: String): ByteArray? = null
        override suspend fun listSavedAssets(): List<SavedCaptureMedia> = saved
    }

    @Test fun successfulSaveCommitsWorkspaceOnce() = runBlocking {
        withFixture { store, before, plan ->
            val media = FakeMedia()
            val journal = IosCaptureWorkspaceJournal(store, "pending")
            val coordinator = IosCaptureSaveCoordinator(media, journal)
            var actual = before
            assertTrue(coordinator.saveAndCommit(
                before, requireNotNull(before.active), plan,
                mapOf(CaptureMediaRole.ORIGINAL to byteArrayOf(1)),
                { actual }, { next -> actual = next; true },
            ))
            assertEquals(13, actual.active?.nextCounter)
            assertFalse(coordinator.hasPendingCapture())
            assertFailsWith<IllegalStateException> {
                runBlocking {
                    coordinator.saveAndCommit(before, requireNotNull(before.active), plan,
                        mapOf(CaptureMediaRole.ORIGINAL to byteArrayOf(1)), { actual }, { true })
                }
            }
        }
    }

    @Test fun failedPreferenceWriteIsRecoveredFromPersistedPhotoKitReceipt() = runBlocking {
        withFixture { store, before, plan ->
            val media = FakeMedia()
            val journal = IosCaptureWorkspaceJournal(store, "pending")
            val coordinator = IosCaptureSaveCoordinator(media, journal)
            var actual = before
            assertFalse(coordinator.saveAndCommit(
                before, requireNotNull(before.active), plan,
                mapOf(CaptureMediaRole.ORIGINAL to byteArrayOf(1)),
                { actual }, { false },
            ))
            assertTrue(coordinator.hasPendingCapture())
            assertEquals(12, actual.active?.nextCounter)
            assertTrue(coordinator.recoverPending({ actual }, { next -> actual = next; true }))
            assertEquals(13, actual.active?.nextCounter)
            assertFalse(coordinator.hasPendingCapture())
        }
    }

    @Test fun failedPhotoKitSaveLeavesWorkspaceUntouchedAndBlocksRetry() = runBlocking {
        withFixture { store, before, plan ->
            val coordinator = IosCaptureSaveCoordinator(FakeMedia(fail = true),
                IosCaptureWorkspaceJournal(store, "pending"))
            var actual = before
            assertFailsWith<IllegalStateException> {
                runBlocking {
                    coordinator.saveAndCommit(before, requireNotNull(before.active), plan,
                        mapOf(CaptureMediaRole.ORIGINAL to byteArrayOf(1)),
                        { actual }, { next -> actual = next; true })
                }
            }
            assertEquals(12, actual.active?.nextCounter)
            assertTrue(coordinator.hasPendingCapture())
            assertFalse(coordinator.recoverPending({ actual }, { next -> actual = next; true }))
        }
    }

    private suspend fun withFixture(
        block: suspend (NSUserDefaults, EditorWorkspace, CaptureMediaSavePlan) -> Unit,
    ) {
        val suite = "dzlog.capture.coordinator.test.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val template = SavedTableTemplate("field", "현장 기록", TableTemplateState(1, 1, emptyList()),
                TableStyleState(), 0L)
            val document = EditorDocument(template, saveMode = SaveMode.ORIGINAL_ONLY, nextCounter = 12)
            val workspace = EditorWorkspace(listOf(document), "field")
            val path = "Pictures/DZlog/Field/"
            val preview = FinalCapturePreview(emptyList(), TablePatch.EMPTY, "Field_0012.jpg", 12, 0,
                CounterScope(buildCounterPath(path, SaveMode.ORIGINAL_ONLY), "Field_", 12, false), path)
            block(defaults, workspace, buildCaptureMediaSavePlan(preview, SaveMode.ORIGINAL_ONLY, "shot-12"))
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }
}
