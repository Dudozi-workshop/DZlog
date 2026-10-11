package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryViewerInteractionPolicyTest {
    @Test
    fun centered_thumbnail_is_selected_with_symmetric_content_padding() {
        assertEquals(0, nearestFilmstripIndex(-170, 230, listOf(
            FilmstripItemBounds(0, 0, 60), FilmstripItemBounds(1, 68, 60))))
    }

    @Test
    fun selection_follows_viewport_center_during_drag_and_fling() {
        assertEquals(3, nearestFilmstripIndex(-170, 230, listOf(
            FilmstripItemBounds(2, -48, 60), FilmstripItemBounds(3, 20, 60),
            FilmstripItemBounds(4, 88, 60))))
        assertEquals(4, nearestFilmstripIndex(-170, 230, listOf(
            FilmstripItemBounds(3, -68, 60), FilmstripItemBounds(4, 0, 60))))
    }

    @Test
    fun empty_filmstrip_has_no_target() {
        assertNull(nearestFilmstripIndex(0, 400, emptyList()))
    }

    @Test
    fun upward_swipe_requires_distance_and_vertical_intent() {
        assertTrue(isPhotoInfoSwipe(12f, -80f, 64f))
        assertFalse(isPhotoInfoSwipe(0f, -30f, 64f))
        assertFalse(isPhotoInfoSwipe(0f, 80f, 64f))
        assertFalse(isPhotoInfoSwipe(100f, -80f, 64f))
        assertFalse(isPhotoInfoSwipe(80f, 0f, 64f))
    }
}
