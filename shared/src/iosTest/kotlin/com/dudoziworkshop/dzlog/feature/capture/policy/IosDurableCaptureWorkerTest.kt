@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.media.*
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.*
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.*

class IosDurableCaptureWorkerTest {
    private class FakeMedia : CaptureMediaRepository {
        val saveOrder = mutableListOf<String>()
        private val saved = mutableListOf<SavedCaptureMedia>()

        override suspend fun saveAllOrRollback(
            plan: CaptureMediaSavePlan,
            jpegByRole: Map<CaptureMediaRole, ByteArray>,
        ): ConfirmedCaptureMediaSave {
            plan.validatePayloads(jpegByRole)
            saveOrder += plan.captureId
            val assets = plan.targets.mapIndexed { index, target ->
                PersistedCaptureAsset(target, "asset-${plan.captureId}-$index")
            }
            saved += assets.map { SavedCaptureMedia(plan.captureId, it, CaptureMediaAvailability.PRESENT) }
            return plan.confirm(assets)
        }

        override suspend fun readAsset(assetId: String): ByteArray? = null
        override suspend fun listSavedAssets(): List<SavedCaptureMedia> = saved.toList()
    }

    @Test fun drainsReadyCapturesStrictlyInReservationOrder() = withFixture { staging, journal, media, initial ->
        val jpeg = byteArrayOf(1, 2, 3)
        val first = snapshot("capture-1", 1, initial, jpeg)
        val second = snapshot("capture-2", 2, first.workspaceAfter, jpeg)
        staging.stage(second, jpeg)
        staging.stage(first, jpeg)
        var workspace = initial

        val results = runSynchronously { worker(staging, journal, media,
            { workspace }, { workspace = it; true }).processAvailable() }

        assertEquals(listOf("capture-1", "capture-2"), media.saveOrder)
        assertEquals(listOf(IosDurableCaptureWorkerStatus.COMMITTED,
            IosDurableCaptureWorkerStatus.COMMITTED), results.map { it.status })
        assertEquals(9, workspace.active?.nextCounter)
        assertEquals(DurableCaptureStage.COMMITTED, staging.load("capture-1")?.record?.stage)
        assertEquals(DurableCaptureStage.COMMITTED, staging.load("capture-2")?.record?.stage)
    }

    @Test fun drainsThreeRapidCapturesAndAdvancesWorkspaceExactlyOnceEach() = withFixture { staging, journal, media, initial ->
        val jpeg = byteArrayOf(1, 2, 3)
        val first = snapshot("capture-1", 1, initial, jpeg)
        val second = snapshot("capture-2", 2, first.workspaceAfter, jpeg)
        val third = snapshot("capture-3", 3, second.workspaceAfter, jpeg)
        staging.stage(third, jpeg)
        staging.stage(first, jpeg)
        staging.stage(second, jpeg)
        var workspace = initial

        val results = runSynchronously { worker(staging, journal, media,
            { workspace }, { workspace = it; true }).processAvailable() }

        assertEquals(listOf("capture-1", "capture-2", "capture-3"), media.saveOrder)
        assertEquals(List(3) { IosDurableCaptureWorkerStatus.COMMITTED },
            results.map { it.status })
        assertEquals(10, workspace.active?.nextCounter)
        assertEquals(listOf(8, 9, 10), listOf(first, second, third).map {
            it.workspaceAfter.active?.nextCounter
        })
        assertTrue(listOf(first, second, third).all {
            staging.load(it.record.captureId)?.record?.stage == DurableCaptureStage.COMMITTED
        })
    }

    @Test fun renderFailureLeavesHeadReadyAndNeverSkipsToLaterCapture() = withFixture { staging, journal, media, initial ->
        val jpeg = byteArrayOf(1, 2, 3)
        val first = snapshot("capture-1", 1, initial, jpeg)
        val second = snapshot("capture-2", 2, first.workspaceAfter, jpeg)
        staging.stage(first, jpeg)
        staging.stage(second, jpeg)

        assertFails { runSynchronously {
            IosDurableCaptureWorker(staging, media, { initial }, { true },
                { _, _ -> error("render failed") }, journal).processAvailable()
        } }
        assertTrue(media.saveOrder.isEmpty())
        assertEquals(DurableCaptureStage.READY, staging.load("capture-1")?.record?.stage)
        assertEquals(DurableCaptureStage.READY, staging.load("capture-2")?.record?.stage)
    }

    @Test fun relaunchRecoversReceiptBeforeProcessingNextCapture() = withFixture { staging, journal, media, initial ->
        val jpeg = byteArrayOf(1, 2, 3)
        val first = snapshot("capture-1", 1, initial, jpeg)
        val second = snapshot("capture-2", 2, first.workspaceAfter, jpeg)
        staging.stage(first, jpeg)
        staging.stage(second, jpeg)
        var workspace = initial

        val waiting = runSynchronously { worker(staging, journal, media,
            { workspace }, { false }).processAvailable() }
        assertEquals(IosDurableCaptureWorkerStatus.WAITING_FOR_RECOVERY, waiting.single().status)
        assertEquals(DurableCaptureStage.PROCESSING, staging.load("capture-1")?.record?.stage)
        assertEquals(listOf("capture-1"), media.saveOrder)

        val recovered = runSynchronously { worker(staging, journal, media,
            { workspace }, { workspace = it; true }).processAvailable() }
        assertEquals(listOf(IosDurableCaptureWorkerStatus.RECOVERED,
            IosDurableCaptureWorkerStatus.COMMITTED), recovered.map { it.status })
        assertEquals(listOf("capture-1", "capture-2"), media.saveOrder)
        assertEquals(9, workspace.active?.nextCounter)
    }

    @Test fun changedWorkspaceMarksHeadForReviewAndBlocksLaterCaptures() = withFixture { staging, journal, media, initial ->
        val jpeg = byteArrayOf(1, 2, 3)
        val first = snapshot("capture-1", 1, initial, jpeg)
        val second = snapshot("capture-2", 2, first.workspaceAfter, jpeg)
        staging.stage(first, jpeg)
        staging.stage(second, jpeg)
        val changed = initial.copy(settings = initial.settings.copy(showCameraGrid = true))

        val result = runSynchronously { worker(staging, journal, media,
            { changed }, { true }).processAvailable() }
        assertEquals(IosDurableCaptureWorkerStatus.NEEDS_REVIEW, result.single().status)
        assertEquals(DurableCaptureStage.NEEDS_REVIEW, staging.load("capture-1")?.record?.stage)
        assertEquals(DurableCaptureReviewReason.WORKSPACE_CHANGED_BEFORE_SAVE,
            staging.load("capture-1")?.record?.reviewReason)
        assertEquals(DurableCaptureStage.READY, staging.load("capture-2")?.record?.stage)
        assertTrue(media.saveOrder.isEmpty())
    }

    private fun worker(
        staging: IosDurableCaptureStagingStore,
        journal: IosCaptureWorkspaceJournal,
        media: FakeMedia,
        load: () -> EditorWorkspace,
        persist: (EditorWorkspace) -> Boolean,
    ) = IosDurableCaptureWorker(staging, media, load, persist,
        buildPayloads = { snapshot, source ->
            snapshot.savePlan.targets.associate { target -> target.role to source }
        }, journal = journal)

    private fun snapshot(
        captureId: String,
        order: Long,
        before: EditorWorkspace,
        jpeg: ByteArray,
    ): DurableCaptureSnapshot {
        val document = checkNotNull(before.active)
        val counter = checkNotNull(document.nextCounter)
        val patch = TablePatch(mapOf("counter" to (counter + 1).toString()))
        val scope = CounterScope("Pictures/DZlog/", "", counter, false)
        val preview = FinalCapturePreview(
            listOf(ResolvedCell("counter", TableCellDataType.COUNTER,
                resolvedText = counter.toString().padStart(4, '0'), isEmpty = false)),
            patch, "${counter.toString().padStart(4, '0')}.jpg", counter, 0,
            scope, "Pictures/DZlog/",
        )
        val plan = CaptureMediaSavePlan(captureId, counter, scope, patch, listOf(
            CaptureMediaTarget(CaptureMediaRole.ORIGINAL, "Pictures/DZlog/", preview.displayName),
        ))
        val after = prepareCaptureWorkspaceUpdate(before, document, plan)
        return DurableCaptureSnapshot(
            DurableCaptureRecord(captureId, order, document.template.id, null, counter, 0,
                DurableCaptureStage.READY),
            document, before, after, FrozenCapturePreview.freeze(preview), plan,
            IosDurableCaptureStagingStore.sourceIdentity(jpeg),
        )
    }

    private fun initialWorkspace(): EditorWorkspace {
        val template = SavedTableTemplate("template-a", "촬영",
            TableTemplateState(1, 1, listOf(TableCellState(
                cellId = "counter", rowIndex = 0, colIndex = 0,
                dataType = TableCellDataType.COUNTER, rawText = "7",
                typedValue = CellValue.CounterSeed(7),
            ))), TableStyleState(), 10L)
        return EditorWorkspace(listOf(EditorDocument(template,
            saveMode = SaveMode.ORIGINAL_ONLY, nextCounter = 7, usesAutoNext = false)), template.id)
    }

    private fun withFixture(block: (
        IosDurableCaptureStagingStore,
        IosCaptureWorkspaceJournal,
        FakeMedia,
        EditorWorkspace,
    ) -> Unit) {
        val token = NSUUID().UUIDString
        val root = NSTemporaryDirectory().trimEnd('/') + "/dzlog-worker-$token"
        val suite = "dzlog.capture.worker.$token"
        val defaults = NSUserDefaults(suiteName = suite)
        try {
            block(IosDurableCaptureStagingStore(root),
                IosCaptureWorkspaceJournal(defaults, "pending"), FakeMedia(), initialWorkspace())
        } finally {
            if (NSFileManager.defaultManager.fileExistsAtPath(root)) {
                NSFileManager.defaultManager.removeItemAtPath(root, error = null)
            }
            defaults.removePersistentDomainForName(suite)
        }
    }

    private fun <T> runSynchronously(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<T>) { completed = result }
        })
        return checkNotNull(completed) { "Fixture must not suspend asynchronously." }.getOrThrow()
    }
}
