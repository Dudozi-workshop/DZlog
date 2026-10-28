package com.dudoziworkshop.dzlog.data.repository

import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.CaptureAspect
import com.dudoziworkshop.dzlog.domain.model.CaptureRequest
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.model.WatermarkConfig
import com.dudoziworkshop.dzlog.domain.model.WatermarkTableAnchor
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.domain.table.applyPatch
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservation
import com.dudoziworkshop.dzlog.feature.capture.policy.CaptureReservationScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class CaptureReceiptRecoveryPolicyTest {
    private val base = TableTemplateState(
        rows = 1,
        cols = 1,
        cells = listOf(TableCellState(
            rowIndex = 0, colIndex = 0, cellId = "count",
            dataType = TableCellDataType.COUNTER,
            typedValue = CellValue.CounterSeed(1),
        ))
    )

    private fun receipt(id: String, millis: Long, before: TableTemplateState, next: Int):
        CapturePendingStore.PendingCapture {
        val snapshot = CaptureJobSnapshot(
            request = CaptureRequest(
                relativePath = "Pictures/DZlog/",
                displayName = "snap_$next.jpg",
                resolvedCells = emptyList(),
                watermarkCells = emptyList(),
                saveMode = SaveMode.ORIGINAL_ONLY,
                captureAspect = CaptureAspect.R3_4,
                tableTemplate = before,
                watermark = WatermarkConfig(
                    anchor = WatermarkTableAnchor.BOTTOM_RIGHT,
                    offsetXRatio = 0, offsetYRatio = 0,
                    tableHeightRatio = 20, tableWidthRatio = 40,
                    tableBgAlpha = 80, bgStyle = 0, valueScale = 100,
                    textColorMode = 0, manualTextColor = 0, textAlign = 0
                )
            ),
            tablePatch = TablePatch(mapOf("count" to next.toString())),
        )
        return CapturePendingStore.PendingCapture(
            reservation = CaptureReservation(
                id = id,
                scope = CaptureReservationScope("Pictures/DZlog/", "snap_"),
                counter = next - 1,
                createdAtMillis = millis
            ),
            status = CapturePendingStore.Status.COMMITTED,
            snapshot = snapshot,
            imageFile = File("/does-not-need-to-exist"),
            outputUris = listOf("content://example/$id")
        )
    }

    @Test fun recoversMultipleCommittedShotsInOrder() {
        val first = receipt("first", 1, base, 2)
        val afterFirst = base.applyPatch(first.snapshot.tablePatch)
        val second = receipt("second", 2, afterFirst, 3)
        val result = CaptureReceiptRecoveryPolicy.resolve(base, listOf(second, first))
        assertEquals(listOf("first", "second"), result!!.acknowledgedIds)
        assertEquals(3, (result.template.cells.first().typedValue as CellValue.CounterSeed).start)
    }

    @Test fun alreadyAppliedReceiptsCanBeAcknowledgedIdempotently() {
        val first = receipt("first", 1, base, 2)
        val second = receipt("second", 2, base.applyPatch(first.snapshot.tablePatch), 3)
        val finalTemplate = second.snapshot.request.tableTemplate.applyPatch(second.snapshot.tablePatch)
        assertEquals(finalTemplate,
            CaptureReceiptRecoveryPolicy.resolve(finalTemplate, listOf(first, second))!!.template)
    }

    @Test fun differentTemplateIsRejectedWithoutAnyPatch() {
        val first = receipt("first", 1, base, 2)
        val modified = base.copy(cells = listOf(base.cells.first().copy(rawText = "user edit")))
        assertNull(CaptureReceiptRecoveryPolicy.resolve(modified, listOf(first)))
    }
}
