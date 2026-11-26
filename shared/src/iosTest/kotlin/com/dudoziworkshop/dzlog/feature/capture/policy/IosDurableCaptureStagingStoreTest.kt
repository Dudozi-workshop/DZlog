@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.prepareCaptureWorkspaceUpdate
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosDurableCaptureStagingStoreTest {
    private val files = NSFileManager.defaultManager

    @Test fun stagesJpegBeforeManifestAndRoundTripsVerifiedSnapshot() = withStore { store, root ->
        val jpeg = byteArrayOf(1, 2, 3, 4)
        val snapshot = fixture("capture-a", 1, jpeg)
        store.stage(snapshot, jpeg)

        assertEquals(snapshot, store.load("capture-a"))
        assertEquals(listOf(snapshot), store.listSnapshots())
        assertTrue(files.fileExistsAtPath("$root/capture-a/source.jpg"))
        assertTrue(files.fileExistsAtPath("$root/capture-a/manifest.json"))
    }

    @Test fun editorNamespaceIsStableAndFilesystemSafe() {
        val first = IosDurableCaptureStagingStore.stableNamespace("편집기/현장 A")
        assertEquals(first, IosDurableCaptureStagingStore.stableNamespace("편집기/현장 A"))
        assertEquals(64, first.length)
        assertTrue(first.all { it in '0'..'9' || it in 'a'..'f' })
        assertTrue(first != IosDurableCaptureStagingStore.stableNamespace("편집기/현장 B"))
    }

    @Test fun corruptedJpegNeverLoadsAsReadyWork() = withStore { store, _ ->
        val jpeg = byteArrayOf(1, 2, 3, 4)
        store.stage(fixture("capture-a", 1, jpeg), jpeg)
        byteArrayOf(9, 9).toNSData().writeToFile(store.paths("capture-a").sourceJpeg, atomically = true)

        assertFails { store.load("capture-a") }
        assertFails { store.listSnapshots() }
    }

    @Test fun orphanedJpegHasNoReadySnapshotAndCanBeRecovered() = withStore { store, _ ->
        val paths = store.paths("capture-a")
        assertTrue(files.createDirectoryAtPath(paths.directory,
            withIntermediateDirectories = true, attributes = null, error = null))
        byteArrayOf(1, 2, 3).toNSData().writeToFile(paths.sourceJpeg, atomically = true)

        assertNull(store.load("capture-a"))
        assertEquals(emptyList(), store.listSnapshots())
        assertTrue(store.discardIncomplete("capture-a"))
        assertFalse(files.fileExistsAtPath(paths.directory))
    }

    @Test fun stageTransitionsAreValidatedAndPersisted() = withStore { store, _ ->
        val jpeg = byteArrayOf(1, 2, 3, 4)
        store.stage(fixture("capture-a", 1, jpeg), jpeg)

        assertEquals(DurableCaptureStage.PROCESSING,
            store.transition("capture-a", DurableCaptureStage.PROCESSING).record.stage)
        assertEquals(DurableCaptureStage.MEDIA_SAVED,
            store.transition("capture-a", DurableCaptureStage.MEDIA_SAVED).record.stage)
        assertEquals(DurableCaptureStage.COMMITTED,
            store.transition("capture-a", DurableCaptureStage.COMMITTED).record.stage)
        assertFails { store.transition("capture-a", DurableCaptureStage.READY) }
        assertEquals(DurableCaptureStage.COMMITTED, store.load("capture-a")?.record?.stage)
        assertTrue(store.removeTerminal("capture-a"))
        assertNull(store.load("capture-a"))
    }

    @Test fun onlyPreSaveReviewCanBeResolvedAsCancelled() = withStore { store, _ ->
        val jpeg = byteArrayOf(1, 2, 3, 4)
        store.stage(fixture("capture-a", 1, jpeg), jpeg)
        val reviewed = store.markNeedsReview("capture-a",
            DurableCaptureReviewReason.WORKSPACE_CHANGED_BEFORE_SAVE)
        assertEquals(DurableCaptureReviewReason.WORKSPACE_CHANGED_BEFORE_SAVE,
            reviewed.record.reviewReason)
        assertEquals(DurableCaptureStage.CANCELLED,
            store.cancelReviewedBeforeSave("capture-a").record.stage)

        store.stage(fixture("capture-b", 2, jpeg), jpeg)
        store.transition("capture-b", DurableCaptureStage.PROCESSING)
        store.markNeedsReview("capture-b",
            DurableCaptureReviewReason.PROCESSING_RECEIPT_MISSING)
        assertFails { store.cancelReviewedBeforeSave("capture-b") }
    }

    @Test fun refusesOverwriteTraversalAndIncompleteRetry() = withStore { store, _ ->
        val jpeg = byteArrayOf(1, 2, 3, 4)
        val snapshot = fixture("capture-a", 1, jpeg)
        store.stage(snapshot, jpeg)
        assertFails { store.stage(snapshot, jpeg) }
        assertFails { store.load("../capture-a") }
        assertFails { store.discardIncomplete("capture-a") }
        assertFails { store.removeTerminal("capture-a") }
    }

    private fun fixture(captureId: String, order: Long, jpeg: ByteArray): DurableCaptureSnapshot {
        val template = SavedTableTemplate("template-a", "촬영",
            TableTemplateState(1, 1, listOf(TableCellState(
                cellId = "counter", rowIndex = 0, colIndex = 0,
                dataType = TableCellDataType.COUNTER, rawText = "7",
                typedValue = CellValue.CounterSeed(7),
            ))), TableStyleState(), 10L)
        val document = EditorDocument(template, saveMode = SaveMode.ORIGINAL_ONLY,
            nextCounter = 7, usesAutoNext = false)
        val before = EditorWorkspace(listOf(document), template.id)
        val scope = CounterScope("Pictures/DZlog/", "", 7, false)
        val patch = TablePatch(mapOf("counter" to "8"))
        val preview = FinalCapturePreview(
            listOf(ResolvedCell("counter", TableCellDataType.COUNTER,
                resolvedText = "0007", isEmpty = false)), patch, "0007.jpg", 7, 0,
            scope, "Pictures/DZlog/",
        )
        val plan = CaptureMediaSavePlan(captureId, 7, scope, patch, listOf(
            CaptureMediaTarget(CaptureMediaRole.ORIGINAL, "Pictures/DZlog/", "0007.jpg"),
        ))
        return DurableCaptureSnapshot(
            DurableCaptureRecord(captureId, order, template.id, null, 7, 0,
                DurableCaptureStage.READY),
            document, before, prepareCaptureWorkspaceUpdate(before, document, plan),
            FrozenCapturePreview.freeze(preview), plan,
            IosDurableCaptureStagingStore.sourceIdentity(jpeg),
        )
    }

    private fun withStore(block: (IosDurableCaptureStagingStore, String) -> Unit) {
        val root = NSTemporaryDirectory().trimEnd('/') + "/dzlog-stage-${NSUUID().UUIDString}"
        try {
            block(IosDurableCaptureStagingStore(root), root)
        } finally {
            if (files.fileExistsAtPath(root)) files.removeItemAtPath(root, error = null)
        }
    }
}

private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
}
