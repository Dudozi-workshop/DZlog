package com.dudoziworkshop.dzlog.domain.gallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryCounterBadgePolicyTest {
    @Test
    fun knownCounterDisplaysWithoutFilenamePadding() {
        assertEquals("7", GalleryCounterBadgePolicy.labelForVerifiedCounter(7, enabled = true))
        assertEquals("105", GalleryCounterBadgePolicy.labelForVerifiedCounter(105, enabled = true))
    }

    @Test
    fun badgeSettingHidesEvenVerifiedCounter() {
        assertNull(GalleryCounterBadgePolicy.labelForVerifiedCounter(28, enabled = false))
    }

    @Test
    fun unverifiedOrInvalidCounterRemainsUnlabeled() {
        assertNull(GalleryCounterBadgePolicy.labelForVerifiedCounter(null, enabled = true))
        assertNull(GalleryCounterBadgePolicy.labelForVerifiedCounter(0, enabled = true))
        assertNull(GalleryCounterBadgePolicy.labelForVerifiedCounter(-1, enabled = true))
    }

    @Test
    fun badgePolicyDoesNotAttemptToParseAmbiguousFilenameDigits() {
        // Only a verified integer is accepted; filenames such as
        // Sample_20261016.jpg must not be interpreted here.
        assertNull(GalleryCounterBadgePolicy.labelForVerifiedCounter(null, enabled = true))
    }
    @Test
    fun generatedDzlogNamesDisplayUnpaddedCounters() {
        assertEquals("28", GalleryCounterBadgePolicy.labelFromDzlogFilename(
            "Blueberry_A_0028.jpg", "Pictures/DZlog/Plants/", true
        ))
        assertEquals("3", GalleryCounterBadgePolicy.labelFromDzlogFilename(
            "Sample_3.jpeg", "Pictures/DZlog/original/", true
        ))
    }

    @Test
    fun nonDzlogAndNonCounterNamesDoNotGetBadges() {
        assertNull(GalleryCounterBadgePolicy.labelFromDzlogFilename(
            "Sample_28.jpg", "Pictures/Other/", true
        ))
        assertNull(GalleryCounterBadgePolicy.labelFromDzlogFilename(
            "Sample_202610.jpg", "Pictures/DZlog/", true
        ))
        assertNull(GalleryCounterBadgePolicy.labelFromDzlogFilename(
            "Sample_2026_10_16.jpg", "Pictures/DZlog/", true
        ))
        assertNull(GalleryCounterBadgePolicy.labelFromDzlogFilename(
            "Blueberry_003.jpg", "Pictures/DZlog/", false
        ))
    }

}
