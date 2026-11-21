package com.dudoziworkshop.dzlog.data.media

import kotlin.test.Test
import kotlin.test.assertEquals

class SavedGallerySwipePolicyTest {
    private fun action(x: Float, y: Float, scale: Float = 1f, multi: Boolean = false) =
        savedGallerySwipeAction(x, y, scale, multi, 64f)

    @Test fun fittedHorizontalSwipesNavigateInBothDirections() {
        assertEquals(SavedGallerySwipeAction.NEXT, action(-80f, 10f))
        assertEquals(SavedGallerySwipeAction.PREVIOUS, action(80f, 10f))
    }
    @Test fun upwardDominantSwipeShowsInfoButDownwardDoesNot() {
        assertEquals(SavedGallerySwipeAction.INFO, action(10f, -80f))
        assertEquals(SavedGallerySwipeAction.NONE, action(10f, 80f))
        assertEquals(SavedGallerySwipeAction.NONE, action(60f, -70f))
    }
    @Test fun zoomAndPinchNeverNavigateEvenIfEndingFitted() {
        assertEquals(SavedGallerySwipeAction.NONE, action(-200f, 0f, 2f))
        assertEquals(SavedGallerySwipeAction.NONE, action(-200f, 0f, multi = true))
        assertEquals(SavedGallerySwipeAction.NONE, action(0f, -200f, multi = true))
    }
    @Test fun smallAndInvalidGesturesDoNothing() {
        assertEquals(SavedGallerySwipeAction.NONE, action(10f, 0f))
        assertEquals(SavedGallerySwipeAction.NONE, action(Float.NaN, 0f))
        assertEquals(SavedGallerySwipeAction.NONE, action(0f, -100f, Float.NaN))
        assertEquals(SavedGallerySwipeAction.NONE, action(-100f, 0f, 0f))
        assertEquals(SavedGallerySwipeAction.NONE, savedGallerySwipeAction(100f, 0f, 1f, false, 0f))
    }
}
