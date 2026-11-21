package com.dudoziworkshop.dzlog.data.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SavedGalleryShareBudgetTest {
    @Test fun countBoundsRejectEmptyAndOversizedBatches() {
        SavedGalleryShareBudget.validateCount(1)
        SavedGalleryShareBudget.validateCount(50)
        assertFailsWith<IllegalArgumentException> { SavedGalleryShareBudget.validateCount(0) }
        assertFailsWith<IllegalArgumentException> { SavedGalleryShareBudget.validateCount(51) }
    }
    @Test fun byteBudgetAcceptsBoundaryAndRejectsOverflowOrEmptyData() {
        val limit = SavedGalleryShareBudget.MAX_BYTES
        assertEquals(limit, SavedGalleryShareBudget.appendBytes(limit - 1, 1))
        assertFailsWith<IllegalArgumentException> { SavedGalleryShareBudget.appendBytes(limit, 1) }
        assertFailsWith<IllegalArgumentException> { SavedGalleryShareBudget.appendBytes(0, 0) }
        assertFailsWith<IllegalArgumentException> { SavedGalleryShareBudget.appendBytes(Long.MAX_VALUE, Int.MAX_VALUE) }
        assertFailsWith<IllegalArgumentException> { SavedGalleryShareBudget.appendBytes(-1, 1) }
    }
}
