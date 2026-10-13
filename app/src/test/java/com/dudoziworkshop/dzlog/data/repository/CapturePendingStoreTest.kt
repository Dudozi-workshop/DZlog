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
                    tableBgAlpha = 80, valueScale = 100,
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

    @Test fun snapshotRoundTripPreservesSavePolicyAndProgress() {
        val before = snapshot(9)
        val after = CaptureJobSnapshotCodec.decode(CaptureJobSnapshotCodec.encode(before))
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
}
