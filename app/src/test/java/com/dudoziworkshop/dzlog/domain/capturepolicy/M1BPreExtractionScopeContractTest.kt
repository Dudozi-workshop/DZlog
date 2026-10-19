package com.dudoziworkshop.dzlog.domain.capturepolicy

import com.dudoziworkshop.dzlog.domain.counter.buildCounterScopeParts
import com.dudoziworkshop.dzlog.domain.model.FILE_NAME_SLOT_COUNT
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.util.Date

/**
 * M1-B B1: lock down all four scope combinations before extracting shared policy.
 * The scope key may change, but the generated physical name/path must not.
 */
class M1BPreExtractionScopeContractTest {
    @Test
    fun `all four scope combinations react only to enabled axis`() {
        val a = naming("A", "ONE")
        val changedPath = naming("B", "ONE")
        val changedName = naming("A", "TWO")

        for (pathOn in listOf(false, true)) {
            for (nameOn in listOf(false, true)) {
                fun scope(result: CaptureNamingPolicy.Result) =
                    buildCounterScopeParts(
                        result.counterScope.relativePathKey,
                        result.counterScope.streamPrefix,
                        includePathInScope = pathOn,
                        includeFilenameInScope = nameOn,
                    ).scopeKey

                if (pathOn) assertNotEquals(scope(a), scope(changedPath))
                else assertEquals(scope(a), scope(changedPath))

                if (nameOn) assertNotEquals(scope(a), scope(changedName))
                else assertEquals(scope(a), scope(changedName))
            }
        }
    }

    @Test
    fun `counter scope selection does not modify physical naming outputs`() {
        val result = naming("A", "ONE")
        for (pathOn in listOf(false, true)) {
            for (nameOn in listOf(false, true)) {
                buildCounterScopeParts(
                    result.counterScope.relativePathKey,
                    result.counterScope.streamPrefix,
                    includePathInScope = pathOn,
                    includeFilenameInScope = nameOn,
                )
                assertEquals("Pictures/DZlog/A/", result.relativePath)
                assertEquals(result.counterScope.relativePathKey, result.relativePath)
            }
        }
    }

    private fun naming(folder: String, title: String): CaptureNamingPolicy.Result =
        CaptureNamingPolicy.buildForCaptureWithCounter(
            captureContext = CaptureContext(
                resolvedCells = emptyList(),
                captureNow = Date(0),
                fileNameSlotDrafts = listOf(
                    TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = title),
                ) + List(FILE_NAME_SLOT_COUNT - 1) { null },
                pathSlotDrafts = listOf(
                    TableEditorSlotDraft(kind = "MANUAL", label = "직접입력", manualText = folder),
                ),
                fnDelim = "_",
                counterDigits = 4,
                dateFormat = "yyyyMMdd",
                timeFormat = "HHmm",
                includeFilenameInCounterScope = true,
                saveMode = SaveMode.WATERMARK_ONLY,
            ),
            usedCounter = 12,
        )
}
