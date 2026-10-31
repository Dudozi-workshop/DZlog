package com.dudoziworkshop.dzlog.data.repository

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.data.template.defaultTableTemplateState
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.PhotoQualityMode
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CapturePendingStoreTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun snapshot(counter: Int): CaptureJobSnapshot {
        val template = defaultTableTemplateState()
        return CaptureJobSnapshot(
            request = CaptureRequest(
                relativePath = "Pictures/DZlog/Test/",
                group1 = "T1", group2 = "S1",
                displayName = "T1_$counter.jpg",
                resolvedCells = emptyList(),
                watermarkCells = listOf(WatermarkBuilder.WatermarkCell("snapshot")),
                saveMode = SaveMode.BOTH,
                captureAspect = CaptureAspect.R3_4,
                photoQualityMode = PhotoQualityMode.BALANCED,
                tableTemplate = template,
                watermark = WatermarkConfig(
                    anchor = WatermarkTableAnchor.BOTTOM_RIGHT,
                    offsetXRatio = 0, offsetYRatio = 0,
                    tableHeightRatio = 20, tableWidthRatio = 40,
                    tableBgAlpha = 80, bgStyle = 0, valueScale = 100,
                    textColorMode = 0, manualTextColor = 0, textAlign = 0
                ),
            ),
            tablePatch = TablePatch(mapOf("cell" to (counter + 1).toString())),
        )
    }

    @Test fun reservationAndStagedJpegSurviveReopening() {
        val scope = CaptureReservationScope("Pictures/DZlog/Test/", "T1_")
        val store = CapturePendingStore(context)
        val first = store.reserveNext(scope, 4, ::snapshot)
        val second = CapturePendingStore(context).reserveNext(scope, 4, ::snapshot)
        assertEquals(4, first.reservation.counter)
        assertEquals(5, second.reservation.counter)
        val source = File(context.cacheDir, "${first.reservation.id}-input.jpg")
        source.writeBytes(byteArrayOf(1, 2, 3, 4))
        try {
            val ready = store.stageJpeg(first.reservation.id, source)
            assertEquals(CapturePendingStore.Status.READY, ready.status)
            assertEquals(4, ready.snapshot.request.displayName.substringAfter("_").substringBefore(".").toInt())
            val afterRestart = CapturePendingStore(context).inspectRecovery()
            assertTrue(afterRestart.ready.any { it.reservation.id == first.reservation.id })
            assertTrue(afterRestart.needsReview.any { it.reservation.id == second.reservation.id })
        } finally {
            source.delete()
            val folder = File(context.filesDir, "capture-pending-v2")
            listOf(first.reservation.id, second.reservation.id).forEach { id ->
                folder.listFiles().orEmpty().filter { it.name.startsWith(id) }.forEach(File::delete)
            }
        }
    }

    @Test fun cameraJpegInPrivateDirectoryIsPromotedWithoutDuplicateCopy() {
        val store = CapturePendingStore(context)
        val pending = store.reserveNext(
            CaptureReservationScope("Pictures/DZlog/Test/", "FAST_"),
            1, ::snapshot,
        )
        val id = pending.reservation.id
        val folder = File(context.filesDir, "capture-pending-v2")
        val incoming = File(folder, "$id.camera.jpg.part")
        val expectedBytes = byteArrayOf(9, 8, 7, 6, 5)
        incoming.writeBytes(expectedBytes)
        try {
            val staged = store.stageJpeg(id, incoming)
            assertEquals(CapturePendingStore.Status.READY, staged.status)
            assertFalse("The CameraX source must be consumed by the rename", incoming.exists())
            assertTrue(staged.imageFile.isFile)
            assertTrue(staged.imageFile.readBytes().contentEquals(expectedBytes))
            val restarted = CapturePendingStore(context)
            assertTrue(restarted.inspectRecovery().ready.any { it.reservation.id == id })
            assertEquals(CapturePendingStore.Status.PROCESSING, restarted.markProcessing(id).status)
        } finally {
            folder.listFiles().orEmpty()
                .filter { it.name.startsWith(id) }.forEach(File::delete)
        }
    }

        @Test fun activeReservationIsNotMistakenForCrashedCapture() {
        val store = CapturePendingStore(context)
        val pending = store.reserveNext(
            CaptureReservationScope("Pictures/DZlog/Test/", "LIVE_"),
            1, ::snapshot,
        )
        val id = pending.reservation.id
        try {
            val inventory = store.inspectRecovery()
            assertFalse(CaptureRecoveryStatusPolicy.needsManualReview(inventory, setOf(id)))
            assertTrue(CaptureRecoveryStatusPolicy.needsManualReview(inventory, emptySet()))
            assertTrue(CaptureRecoveryStatusPolicy.resumableReadyIds(inventory, emptySet()).isEmpty())
        } finally {
            File(context.filesDir, "capture-pending-v2").listFiles().orEmpty()
                .filter { it.name.startsWith(id) }.forEach(File::delete)
        }
    }

    @Test fun onlyUnclaimedVerifiedReadyImagesCanBeConsideredForResume() {
        val store = CapturePendingStore(context)
        val pending = store.reserveNext(
            CaptureReservationScope("Pictures/DZlog/Test/", "RECOVER_"),
            1, ::snapshot,
        )
        val id = pending.reservation.id
        val source = File(context.cacheDir, "$id-staged.jpg")
        source.writeBytes(byteArrayOf(1, 2, 3, 4))
        try {
            store.stageJpeg(id, source)
            val inventory = store.inspectRecovery()
            assertFalse(CaptureRecoveryStatusPolicy.needsManualReview(inventory, setOf(id)))
            assertTrue(CaptureRecoveryStatusPolicy.needsManualReview(inventory, emptySet()))
            assertTrue(CaptureRecoveryStatusPolicy.resumableReadyIds(inventory, setOf(id)).isEmpty())
            assertEquals(
                listOf(id),
                CaptureRecoveryStatusPolicy.resumableReadyIds(inventory, emptySet()),
            )
        } finally {
            source.delete()
            File(context.filesDir, "capture-pending-v2").listFiles().orEmpty()
                .filter { it.name.startsWith(id) }.forEach(File::delete)
        }
    }

    @Test fun snapshotRoundTripPreservesSavePolicyAndProgress() {
        val before = snapshot(9).copy(templateId = "template-9", projectId = "project-A")
        val after = CaptureJobSnapshotCodec.decode(CaptureJobSnapshotCodec.encode(before))
        assertEquals("template-9", after.templateId)
        assertEquals("project-A", after.projectId)
        assertEquals("project-A", CaptureJobSnapshotCodec.decode(
            CaptureJobSnapshotCodec.encode(before)
        ).projectId)
        // Existing version-1 staging files have no templateId and must stay readable.
        val oldJson = CaptureJobSnapshotCodec.encode(before).apply { remove("templateId") }
        assertEquals(null, CaptureJobSnapshotCodec.decode(oldJson).templateId)
        // A v1 journal without projectId belongs to ordinary shooting.
        oldJson.remove("projectId")
        assertEquals(null, CaptureJobSnapshotCodec.decode(oldJson).projectId)
        assertEquals(before.request.displayName, after.request.displayName)
        assertEquals(before.request.saveMode, after.request.saveMode)
        assertEquals(before.request.watermark, after.request.watermark)
        // The template codec normalizes filename slots to FILE_NAME_SLOT_COUNT.
        // The factory's default table currently has only four explicit slots.
        assertEquals(before.request.tableTemplate.rows, after.request.tableTemplate.rows)
        assertEquals(before.request.tableTemplate.cols, after.request.tableTemplate.cols)
        assertEquals(before.request.tableTemplate.cells, after.request.tableTemplate.cells)
        assertEquals(
            before.request.tableTemplate.fileNameSlotDrafts,
            after.request.tableTemplate.fileNameSlotDrafts.take(
                before.request.tableTemplate.fileNameSlotDrafts.size
            )
        )
        assertEquals(before.tablePatch, after.tablePatch)
    }

    @Test fun interruptedJournalBlocksFurtherReservationsWithoutLosingBytes() {
        val store = CapturePendingStore(context)
        val folder = File(context.filesDir, "capture-pending-v2")
        val interrupted = File(folder, "${java.util.UUID.randomUUID()}.json.new")
        interrupted.writeText("incomplete")
        try {
            val report = store.inspectRecovery()
            assertTrue(report.corruptJournals.contains(interrupted.name))
            var failed = false
            try {
                store.reserveNext(
                    CaptureReservationScope("Pictures/DZlog/Test/", "T1_"), 1, ::snapshot
                )
            } catch (_: IllegalStateException) {
                failed = true
            }
            assertTrue(failed)
            assertEquals("incomplete", interrupted.readText())
        } finally {
            interrupted.delete()
        }
    }

    @Test fun activeCameraPartIsNotReportedAsOrphanWhileReservationExists() {
        val store = CapturePendingStore(context)
        val pending = store.reserveNext(
            CaptureReservationScope("Pictures/DZlog/Test/", "PART_"), 1, ::snapshot
        )
        val folder = File(context.filesDir, "capture-pending-v2")
        val activePart = File(folder, "${pending.reservation.id}.camera.jpg.part")
        activePart.writeBytes(byteArrayOf(1, 2, 3))
        val untrackedPart = File(folder, "${java.util.UUID.randomUUID()}.camera.jpg.part")
        untrackedPart.writeBytes(byteArrayOf(4))
        try {
            val report = store.inspectRecovery()
            assertTrue(activePart.name !in report.orphanFiles)
            assertTrue(untrackedPart.name in report.orphanFiles)
        } finally {
            activePart.delete()
            untrackedPart.delete()
            folder.listFiles().orEmpty().filter { it.name.startsWith(pending.reservation.id) }
                .forEach(File::delete)
        }
    }

    @Test fun stagedJpegCorruptionBlocksProcessingButPreservesFile() {
        val store = CapturePendingStore(context)
        val pending = store.reserveNext(
            CaptureReservationScope("Pictures/DZlog/Test/", "HASH_"), 1, ::snapshot
        )
        val source = File(context.cacheDir, "${pending.reservation.id}-source.jpg")
        source.writeBytes(byteArrayOf(1, 2, 3))
        try {
            val staged = store.stageJpeg(pending.reservation.id, source)
            staged.imageFile.appendBytes(byteArrayOf(4))
            var rejected = false
            try {
                store.markProcessing(pending.reservation.id)
            } catch (_: IllegalStateException) {
                rejected = true
            }
            assertTrue(rejected)
            assertTrue(staged.imageFile.exists())
            assertTrue(store.inspectRecovery().needsReview.any {
                it.reservation.id == pending.reservation.id
            })
        } finally {
            source.delete()
            val folder = File(context.filesDir, "capture-pending-v2")
            folder.listFiles().orEmpty()
                .filter { it.name.startsWith(pending.reservation.id) }
                .forEach(File::delete)
        }
    }

    @Test fun committedReceiptRemainsUntilTemplateAcknowledged() {
        val store = CapturePendingStore(context)
        val pending = store.reserveNext(
            CaptureReservationScope("Pictures/DZlog/receipt/", "ACK_"), 3, ::snapshot
        )
        val id = pending.reservation.id
        val source = File(context.cacheDir, "$id-receipt.jpg")
        source.writeBytes(byteArrayOf(1, 2, 3, 4))
        try {
            store.stageJpeg(id, source)
            store.markProcessing(id)
            store.markMediaSaved(id, listOf(
                android.net.Uri.parse("content://test/receipt-a"),
                android.net.Uri.parse("content://test/receipt-b"),
            ))
            store.markCommitted(id)
            store.pruneCommitted(id)
            assertTrue(store.inspectRecovery().completedAwaitingApply.any {
                it.reservation.id == id
            })
            store.acknowledgeTemplateApplied(id)
            assertTrue(store.inspectRecovery().completedAwaitingApply.none {
                it.reservation.id == id
            })
        } finally {
            source.delete()
            File(context.filesDir, "capture-pending-v2").listFiles().orEmpty()
                .filter { it.name.startsWith(id) }.forEach(File::delete)
        }
    }

    @Test fun committedReceiptSurvivesCleanupUntilUiAcknowledgesIt() {
        val store = CapturePendingStore(context)
        val pending = store.reserveNext(
            CaptureReservationScope("Pictures/DZlog/receipt/${System.nanoTime()}/", "T1_"),
            7,
            ::snapshot,
        )
        val id = pending.reservation.id
        val source = File(context.cacheDir, "$id-receipt.jpg")
        source.writeBytes(byteArrayOf(1, 2, 3, 4))
        try {
            store.stageJpeg(id, source)
            store.markProcessing(id)
            store.markMediaSaved(
                id, listOf(Uri.parse("content://dzlog/one"), Uri.parse("content://dzlog/two"))
            )
            store.markCommitted(id)
            store.pruneCommitted(id)
            assertTrue(store.inspectRecovery().completedAwaitingApply.any {
                it.reservation.id == id
            })
            store.acknowledgeTemplateApplied(id)
            assertTrue(store.inspectRecovery().completedAwaitingApply.none {
                it.reservation.id == id
            })
        } finally {
            source.delete()
            val folder = File(context.filesDir, "capture-pending-v2")
            folder.listFiles().orEmpty().filter { it.name.startsWith(id) }.forEach(File::delete)
        }
    }

    @Test fun restartDuringProcessingDoesNotRetryAnUncertainMediaSave() {
        val scope = CaptureReservationScope(
            "Pictures/DZlog/restart-processing/${System.nanoTime()}/", "CRASH_"
        )
        val store = CapturePendingStore(context)
        val reserved = store.reserveNext(scope, 1, ::snapshot)
        val id = reserved.reservation.id
        val source = File(context.cacheDir, "$id-restart.jpg")
        source.writeBytes(byteArrayOf(10, 20, 30, 40))
        val directory = File(context.filesDir, "capture-pending-v2")
        var nextId: String? = null
        try {
            store.stageJpeg(id, source)
            store.markProcessing(id)
            // Simulate process death after the disk journal entered PROCESSING.
            val restarted = CapturePendingStore(context)
            val inventory = restarted.inspectRecovery()
            assertTrue(inventory.ready.none { it.reservation.id == id })
            assertTrue(inventory.needsReview.any {
                it.reservation.id == id &&
                    it.status == CapturePendingStore.Status.PROCESSING
            })
            assertTrue(
                CaptureRecoveryStatusPolicy.resumableReadyIds(inventory, emptySet()).isEmpty()
            )
            assertTrue(CaptureRecoveryStatusPolicy.needsManualReview(inventory, emptySet()))
            assertTrue(File(directory, "$id.jpg").exists())

            // A new capture must not reuse the unfinished capture's number.
            val next = restarted.reserveNext(scope, 1, ::snapshot)
            nextId = next.reservation.id
            assertEquals(reserved.reservation.counter + 1, next.reservation.counter)
        } finally {
            source.delete()
            listOfNotNull(id, nextId).forEach { candidate ->
                directory.listFiles().orEmpty()
                    .filter { it.name.startsWith(candidate) }.forEach(File::delete)
            }
        }
    }

    @Test fun restartAfterBothMediaOutputsBeforeCommitPreservesEvidence() {
        val store = CapturePendingStore(context)
        val reserved = store.reserveNext(
            CaptureReservationScope(
                "Pictures/DZlog/restart-saved/${System.nanoTime()}/", "SAVED_"
            ), 8, ::snapshot
        )
        val id = reserved.reservation.id
        val source = File(context.cacheDir, "$id-saved.jpg")
        source.writeBytes(byteArrayOf(7, 8, 9, 10))
        val expectedUris = listOf(
            Uri.parse("content://media/external/images/media/101"),
            Uri.parse("content://media/external/images/media/102"),
        )
        try {
            store.stageJpeg(id, source)
            store.markProcessing(id)
            store.markMediaSaved(id, expectedUris)

            // Crash before committing the counter/rotating phrase: no blind replay.
            val recovered = CapturePendingStore(context).inspectRecovery()
            assertTrue(recovered.ready.none { it.reservation.id == id })
            val ambiguous = recovered.needsReview.single { it.reservation.id == id }
            assertEquals(CapturePendingStore.Status.SAVED_UNCOMMITTED, ambiguous.status)
            assertEquals(expectedUris.map(Uri::toString), ambiguous.outputUris)
            assertTrue(
                CaptureRecoveryStatusPolicy.resumableReadyIds(recovered, emptySet()).isEmpty()
            )

            val quarantined = CapturePendingStore(context).markNeedsReview(id)
            assertEquals(CapturePendingStore.Status.NEEDS_REVIEW, quarantined.status)
            assertEquals(expectedUris.map(Uri::toString), quarantined.outputUris)
        } finally {
            source.delete()
            File(context.filesDir, "capture-pending-v2").listFiles().orEmpty()
                .filter { it.name.startsWith(id) }.forEach(File::delete)
        }
    }
}
