package com.dudoziworkshop.dzlog.domain.gallery

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryPhotoRowAnchorPolicyTest {
    @Test fun preservesFirstPhotoAcrossThreeToTwoColumns() {
        assertEquals(
            GalleryPhotoRowAnchorPolicy.Position(12, 7),
            GalleryPhotoRowAnchorPolicy.remap(8, 7, 0, 100, 3, 2)
        )
    }
    @Test fun preservesHeaderPosition() {
        assertEquals(
            GalleryPhotoRowAnchorPolicy.Position(2, 5),
            GalleryPhotoRowAnchorPolicy.remap(2, 5, 6, 100, 3, 2)
        )
    }
    @Test fun accountsForHeaderRows() {
        assertEquals(
            GalleryPhotoRowAnchorPolicy.Position(14, 4),
            GalleryPhotoRowAnchorPolicy.remap(11, 4, 5, 100, 3, 2)
        )
    }
    @Test fun clampsLastPartialPhotoRow() {
        assertEquals(
            GalleryPhotoRowAnchorPolicy.Position(4, 0),
            GalleryPhotoRowAnchorPolicy.remap(3, 0, 1, 5, 3, 2)
        )
    }
    @Test fun emptyAndUnchangedAreStable() {
        assertEquals(
            GalleryPhotoRowAnchorPolicy.Position(7, 3),
            GalleryPhotoRowAnchorPolicy.remap(7, 3, 1, 0, 3, 2)
        )
        assertEquals(
            GalleryPhotoRowAnchorPolicy.Position(7, 3),
            GalleryPhotoRowAnchorPolicy.remap(7, 3, 1, 100, 3, 3)
        )
    }
}
