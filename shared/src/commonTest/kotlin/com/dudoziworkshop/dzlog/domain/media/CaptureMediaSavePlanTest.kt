package com.dudoziworkshop.dzlog.domain.media

import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class CaptureMediaSavePlanTest {
    private val root = "Pictures/DZlog/T1/"
    private val savedPatch = TablePatch(mapOf("counter" to "13"))

    private fun preview(mode: SaveMode, base: String = root) = FinalCapturePreview(
        resolvedCells = emptyList(),
        tablePatch = savedPatch,
        displayName = "Draper_0012.jpg",
        usedCounter = 12,
        nextPhraseProgressCursor = 0,
        counterScope = CounterScope(buildCounterPath(base, mode), "Draper_", 12, false),
        relativePathPreview = base,
    )

    @Test fun saveModesKeepAndroidRolesNamesAndLogicalPaths() {
        val original = buildCaptureMediaSavePlan(preview(SaveMode.ORIGINAL_ONLY), SaveMode.ORIGINAL_ONLY, "shot-1")
        assertEquals(listOf(CaptureMediaRole.ORIGINAL), original.targets.map { it.role })
        assertEquals(listOf("Pictures/DZlog/T1/original/"), original.targets.map { it.relativePath })

        val result = buildCaptureMediaSavePlan(preview(SaveMode.WATERMARK_ONLY), SaveMode.WATERMARK_ONLY, "shot-2")
        assertEquals(listOf(CaptureMediaRole.RESULT), result.targets.map { it.role })
        assertEquals(listOf(root), result.targets.map { it.relativePath })

        val both = buildCaptureMediaSavePlan(preview(SaveMode.BOTH), SaveMode.BOTH, "shot-3")
        assertEquals(listOf(CaptureMediaRole.RESULT, CaptureMediaRole.ORIGINAL), both.targets.map { it.role })
        assertEquals(listOf(root, "Pictures/DZlog/T1/original/"), both.targets.map { it.relativePath })
        assertEquals(listOf("Draper_0012.jpg", "Draper_0012.jpg"), both.targets.map { it.displayName })
        assertSame(savedPatch, both.patchAfterSuccess)
        assertEquals(12, both.usedCounter)
        assertEquals("shot-3", both.captureId)
    }

    @Test fun refuseMissingEmptyOrExtraImagePayloadsBeforeSaving() {
        val plan = buildCaptureMediaSavePlan(preview(SaveMode.BOTH), SaveMode.BOTH, "shot-4")
        val one = mapOf(CaptureMediaRole.RESULT to byteArrayOf(1))
        assertFailsWith<IllegalArgumentException> { plan.validatePayloads(one) }
        assertFailsWith<IllegalArgumentException> {
            plan.validatePayloads(mapOf(CaptureMediaRole.RESULT to byteArrayOf(),
                CaptureMediaRole.ORIGINAL to byteArrayOf(2)))
        }
        plan.validatePayloads(mapOf(CaptureMediaRole.RESULT to byteArrayOf(1),
            CaptureMediaRole.ORIGINAL to byteArrayOf(2)))
    }

    @Test fun commitPatchRequiresAllMatchingStableAssetIds() {
        val plan = buildCaptureMediaSavePlan(preview(SaveMode.BOTH), SaveMode.BOTH, "shot-5")
        val result = PersistedCaptureAsset(plan.targets[0], "ph://result")
        val original = PersistedCaptureAsset(plan.targets[1], "ph://original")
        assertFailsWith<IllegalArgumentException> { plan.confirm(listOf(result)) }
        assertFailsWith<IllegalArgumentException> { plan.confirm(listOf(result, result)) }
        assertFailsWith<IllegalArgumentException> {
            plan.confirm(listOf(result, original.copy(assetId = result.assetId)))
        }
        assertFailsWith<IllegalArgumentException> {
            plan.confirm(listOf(result, PersistedCaptureAsset(
                original.target.copy(displayName = "different.jpg"), original.assetId)))
        }
        val complete = plan.confirm(listOf(result, original))
        assertSame(savedPatch, complete.patchAfterSuccess)
        assertEquals(setOf("ph://result", "ph://original"), complete.assets.map { it.assetId }.toSet())
    }

    @Test fun rejectCounterScopeMismatchAndCollidingOriginalFolder() {
        assertFailsWith<IllegalArgumentException> {
            buildCaptureMediaSavePlan(preview(SaveMode.ORIGINAL_ONLY), SaveMode.BOTH, "shot")
        }
        // The legacy path builder treats an existing final "original" segment as the original
        // folder: in BOTH mode that would overwrite the result. Reject instead of silently losing it.
        assertFailsWith<IllegalArgumentException> {
            buildCaptureMediaSavePlan(preview(SaveMode.BOTH, "Pictures/DZlog/original/"), SaveMode.BOTH, "shot")
        }
    }
}
