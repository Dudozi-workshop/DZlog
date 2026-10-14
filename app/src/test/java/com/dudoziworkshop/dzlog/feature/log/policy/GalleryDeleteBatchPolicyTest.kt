package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryDeleteBatchPolicyTest {
    @Test
    fun largeSelectionIsSplitIntoOrderedBoundedBatches() {
        val selected = (1..251).toList()
        val chunks = mutableListOf<List<Int>>()
        var cursor = 0
        while (cursor < selected.size) {
            val batch = GalleryDeleteBatchPolicy.batchAt(selected, cursor)
            assertTrue(batch.isNotEmpty())
            assertTrue(batch.size <= GalleryDeleteBatchPolicy.MAX_BATCH_SIZE)
            chunks += batch
            cursor += batch.size
        }
        assertEquals(listOf(100, 100, 51), chunks.map { it.size })
        assertEquals(selected, chunks.flatten())
    }

    @Test
    fun emptyRemainingSelectionProducesNoRequest() {
        assertTrue(GalleryDeleteBatchPolicy.batchAt((1..3).toList(), 3).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun outOfRangeOffsetIsRejected() {
        GalleryDeleteBatchPolicy.batchAt(listOf(1, 2), 3)
    }
}
