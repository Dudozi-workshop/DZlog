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
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults

class IosCaptureSaveCoordinatorTest {
    private class FakeMedia(
        private val fail: Boolean = false,
        private val rolledBack: Boolean = false,
    ) : CaptureMediaRepository {
        var saved = emptyList<SavedCaptureMedia>()
        override suspend fun saveAllOrRollback(
            plan: CaptureMediaSavePlan, jpegByRole: Map<CaptureMediaRole, ByteArray>,
        ): ConfirmedCaptureMediaSave {
            if (rolledBack) throw PhotoKitSaveRolledBackException("Confirmed transaction rollback")
            if (fail) error("Simulated PhotoKit failure")
            val assets = plan.targets.mapIndexed { index, target -> PersistedCaptureAsset(target, "asset-12-$index") }
            saved = assets.map { SavedCaptureMedia(plan.captureId, it, CaptureMediaAvailability.PRESENT) }
            return plan.confirm(assets)
        }
        override suspend fun readAsset(assetId: String): ByteArray? = null
        override suspend fun listSavedAssets(): List<SavedCaptureMedia> = saved
    }

    @Test fun successfulSaveCommitsWorkspaceOnce() = runSynchronously {
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
                runSynchronously {
                    coordinator.saveAndCommit(before, requireNotNull(before.active), plan,
                        mapOf(CaptureMediaRole.ORIGINAL to byteArrayOf(1)), { actual }, { true })
                }
            }
        }
    }

    @Test fun failedPreferenceWriteIsRecoveredFromPersistedPhotoKitReceipt() = runSynchronously {
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

    @Test fun failedPhotoKitSaveLeavesWorkspaceUntouchedAndBlocksRetry() = runSynchronously {
        withFixture { store, before, plan ->
            val coordinator = IosCaptureSaveCoordinator(FakeMedia(fail = true),
                IosCaptureWorkspaceJournal(store, "pending"))
            var actual = before
            assertFailsWith<IllegalStateException> {
                runSynchronously {
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

    @Test fun allModesRecoverCompleteReceiptsWithoutChangingSaveSettings() = runSynchronously {
        for (mode in SaveMode.entries) withFixture(mode) { store, before, plan ->
            val media = FakeMedia()
            val coordinator = IosCaptureSaveCoordinator(media, IosCaptureWorkspaceJournal(store, "pending"))
            var actual = before
            val payloads = plan.targets.associate { it.role to byteArrayOf(1) }
            assertFalse(coordinator.saveAndCommit(before, checkNotNull(before.active), plan,
                payloads, { actual }, { false }))
            assertEquals(before, actual)
            assertTrue(coordinator.hasPendingCapture())
            assertTrue(coordinator.recoverPending({ actual }, { actual = it; true }))
            assertEquals(13, actual.active?.nextCounter)
            assertEquals(mode, actual.active?.saveMode)
            assertEquals(before.active?.capturePlacement, actual.active?.capturePlacement)
            assertFalse(coordinator.hasPendingCapture())
            assertTrue(coordinator.recoverPending({ actual }, { error("Must not persist twice") }))
        }
    }

    @Test fun confirmedRollbackClearsJournalAndAllowsRetryInEveryMode() = runSynchronously {
        for (mode in SaveMode.entries) withFixture(mode) { store, before, plan ->
            val journal = IosCaptureWorkspaceJournal(store, "pending")
            val coordinator = IosCaptureSaveCoordinator(FakeMedia(rolledBack = true), journal)
            val payloads = plan.targets.associate { it.role to byteArrayOf(1) }
            assertFailsWith<PhotoKitSaveRolledBackException> {
                runSynchronously {
                    coordinator.saveAndCommit(before, checkNotNull(before.active), plan,
                        payloads, { before }, { error("Rollback must not commit workspace") })
                }
            }
            assertFalse(coordinator.hasPendingCapture())
            var actual = before
            assertTrue(IosCaptureSaveCoordinator(FakeMedia(), journal).saveAndCommit(
                before, checkNotNull(before.active), plan, payloads, { actual }, { actual = it; true }))
            assertEquals(13, actual.active?.nextCounter)
        }
    }

    private fun <T> runSynchronously(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) {
                completed = result
            }
        })
        return checkNotNull(completed) {
            "This test fixture must complete its suspend calls without async suspension."
        }.getOrThrow()
    }

    private suspend fun withFixture(
        mode: SaveMode = SaveMode.ORIGINAL_ONLY,
        block: suspend (NSUserDefaults, EditorWorkspace, CaptureMediaSavePlan) -> Unit,
    ) {
        val suite = "dzlog.capture.coordinator.test.${NSUUID().UUIDString}"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            val template = SavedTableTemplate("field", "현장 기록", TableTemplateState(1, 1, emptyList()),
                TableStyleState(), 0L)
            val document = EditorDocument(template, saveMode = mode, nextCounter = 12,
                capturePlacement = com.dudoziworkshop.dzlog.data.template.EditorCapturePlacement(
                    com.dudoziworkshop.dzlog.data.template.EditorCaptureAnchor.CUSTOM, 25, 75, 40, 20, 90))
            val workspace = EditorWorkspace(listOf(document), "field")
            val path = "Pictures/DZlog/Field/"
            val preview = FinalCapturePreview(emptyList(), TablePatch.EMPTY, "Field_0012.jpg", 12, 0,
                CounterScope(buildCounterPath(path, mode), "Field_", 12, false), path)
            block(defaults, workspace, buildCaptureMediaSavePlan(preview, mode, "shot-12"))
        } finally {
            defaults.removePersistentDomainForName(suite)
        }
    }
}

