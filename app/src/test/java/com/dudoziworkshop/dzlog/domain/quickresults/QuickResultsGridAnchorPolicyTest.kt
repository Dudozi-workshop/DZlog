package com.dudoziworkshop.dzlog.domain.quickresults

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickResultsGridAnchorPolicyTest {
    private fun capture(i: Int, folder: String) = QuickResultCapture(
        captureIndex = i,
        folderPath = folder,
        result = QuickResultPhoto("r$i", "R_$i.jpg", folder),
        original = null,
    )

    @Test fun preservesVisibleCaptureWithinSegmentOnThreeToTwoPinch() {
        val sections = QuickResultsSessionPolicy.sections(
            (12 downTo 0).map { capture(it, "Pictures/DZlog/A/") }
        )
        // header=0; third 3-column row=3, anchored capture #6.
        assertEquals(
            QuickResultsGridAnchorPolicy.Position(4, 12),
            QuickResultsGridAnchorPolicy.remap(sections, emptySet(), 3, 12, 3, 2),
        )
    }

    @Test fun countsCollapsedHeadersWithoutInventingPhotoRows() {
        val a = "Pictures/DZlog/A/"
        val b = "Pictures/DZlog/B/"
        val sections = QuickResultsSessionPolicy.sections(
            listOf(capture(8, a), capture(7, a), capture(6, b), capture(5, a),
                capture(4, a), capture(3, a), capture(2, a))
        )
        val collapsed = setOf(sections.first().key)
        // header A = 0 (collapsed), header B = 1, photo B = 2,
        // header A = 3, row A = 4.
        assertEquals(
            QuickResultsGridAnchorPolicy.Position(4, 5),
            QuickResultsGridAnchorPolicy.remap(sections, collapsed, 4, 5, 3, 2),
        )
    }

    @Test fun headerPositionIsStableWhenColumnsChange() {
        val sections = QuickResultsSessionPolicy.sections(
            listOf(capture(2, "Pictures/DZlog/A/"), capture(1, "Pictures/DZlog/B/"))
        )
        assertEquals(
            QuickResultsGridAnchorPolicy.Position(2, 0),
            QuickResultsGridAnchorPolicy.remap(sections, emptySet(), 2, 0, 3, 5),
        )
    }
}
