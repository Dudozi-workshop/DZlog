package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryDefaultStoragePolicyTest {
    @Test
    fun exactlyRootPhotosAppearInDefaultStorageEntry() {
        assertTrue(GalleryDefaultStoragePolicy.isDirectRootPhoto("Pictures/DZlog/"))
        assertTrue(GalleryDefaultStoragePolicy.isDirectRootPhoto("Pictures/DZlog"))
    }

    @Test
    fun nestedAndOriginalPhotosAreNotDuplicatedInDefaultEntry() {
        assertFalse(GalleryDefaultStoragePolicy.isDirectRootPhoto("Pictures/DZlog/original/"))
        assertFalse(GalleryDefaultStoragePolicy.isDirectRootPhoto("Pictures/DZlog/experiment/"))
        assertFalse(GalleryDefaultStoragePolicy.isDirectRootPhoto("Pictures/DZlog/experiment/second/"))
        assertFalse(GalleryDefaultStoragePolicy.isDirectRootPhoto("Pictures/Other/"))
    }
}
