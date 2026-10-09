package com.dudoziworkshop.dzlog.feature.log.policy

import org.junit.Assert.assertEquals
import org.junit.Test

class GalleryViewerDeletePolicyTest {
    @Test fun deletingBeforeCurrentKeepsTheSamePhoto() {
        assertEquals(GalleryViewerDeleteUpdate(listOf(30, 20), 0),
            GalleryViewerDeletePolicy.reconcile(listOf(40, 30, 20), 1, setOf(40)))
    }
    @Test fun deletingCurrentShowsTheNextPhotoInViewerOrder() {
        assertEquals(GalleryViewerDeleteUpdate(listOf(40, 20), 1),
            GalleryViewerDeletePolicy.reconcile(listOf(40, 30, 20), 1, setOf(30)))
    }
    @Test fun deletingLastFallsBackToPrevious() {
        assertEquals(GalleryViewerDeleteUpdate(listOf(40, 30), 1),
            GalleryViewerDeletePolicy.reconcile(listOf(40, 30, 20), 2, setOf(20)))
    }
    @Test fun pairedOriginalDeletionSkipsEveryVerifiedFile() {
        assertEquals(GalleryViewerDeleteUpdate(listOf(50, 10), 1),
            GalleryViewerDeletePolicy.reconcile(listOf(50, 40, 30, 20, 10), 1, setOf(40, 30, 20)))
    }
    @Test fun failedOrCancelledDeletionPreservesCurrentAndOrder() {
        assertEquals(GalleryViewerDeleteUpdate(listOf(40, 30, 20), 1),
            GalleryViewerDeletePolicy.reconcile(listOf(40, 30, 20), 1, emptySet()))
    }
    @Test fun originalsOutsideViewerDoNotChangeItsSubset() {
        assertEquals(GalleryViewerDeleteUpdate(listOf(40, 20), 1),
            GalleryViewerDeletePolicy.reconcile(listOf(40, 20), 1, setOf(99)))
    }
    @Test fun allRemovedAndEmptyInputsHaveNoCurrentPhoto() {
        assertEquals(GalleryViewerDeleteUpdate(emptyList(), 0),
            GalleryViewerDeletePolicy.reconcile(listOf(40), 0, setOf(40)))
        assertEquals(GalleryViewerDeleteUpdate(emptyList(), 0),
            GalleryViewerDeletePolicy.reconcile(emptyList(), 0, emptySet()))
    }
    @Test fun staleIndicesAreClampedBeforeChoosingAnchor() {
        assertEquals(GalleryViewerDeleteUpdate(listOf(40, 30), 1),
            GalleryViewerDeletePolicy.reconcile(listOf(40, 30), 9, emptySet()))
        assertEquals(GalleryViewerDeleteUpdate(listOf(40, 30), 0),
            GalleryViewerDeletePolicy.reconcile(listOf(40, 30), -1, emptySet()))
    }
}
