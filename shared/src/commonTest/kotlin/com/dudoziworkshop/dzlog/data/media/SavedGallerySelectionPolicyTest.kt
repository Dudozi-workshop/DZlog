package com.dudoziworkshop.dzlog.data.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SavedGallerySelectionPolicyTest {
    @Test fun firstLongPressSelectsOnlyItsTarget() {
        assertEquals(setOf("result"), SavedGallerySelectionPolicy.onLongPress(
            emptySet(), "result", setOf("result", "original"), false))
    }

    @Test fun subsequentLongPressAddsWithoutTogglingExistingSelection() {
        val available = setOf("one", "two", "three")
        assertEquals(setOf("one", "two"), SavedGallerySelectionPolicy.onLongPress(
            setOf("one"), "two", available, false))
        assertEquals(setOf("one", "two"), SavedGallerySelectionPolicy.onLongPress(
            setOf("one", "two"), "two", available, false))
    }

    @Test fun lockedOrUnavailableTargetDoesNotEnterSelection() {
        assertNull(SavedGallerySelectionPolicy.onLongPress(emptySet(), "one", setOf("one"), true))
        assertNull(SavedGallerySelectionPolicy.onLongPress(emptySet(), "missing", setOf("one"), false))
        assertNull(SavedGallerySelectionPolicy.onLongPress(emptySet(), "one", emptySet(), false))
    }

    @Test fun staleSelectionIsPrunedWithoutMutatingCaller() {
        val selected = setOf("one", "removed")
        assertEquals(setOf("one", "two"), SavedGallerySelectionPolicy.onLongPress(
            selected, "two", setOf("one", "two"), false))
        assertEquals(setOf("one", "removed"), selected)
    }
}
