package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedGalleryOrderingTest {
    private val root = "Pictures/DZlog/"
    private fun photo(id: String, name: String, date: Long?, path: String = root) = SavedCaptureMedia(
        "capture-$id", PersistedCaptureAsset(CaptureMediaTarget(CaptureMediaRole.ORIGINAL, path, name), id),
        CaptureMediaAvailability.PRESENT, date,
    )
    private fun List<SavedCaptureMedia>.ids() = map { it.asset.assetId }

    @Test fun dateSortsKeepUnknownDatesLastAndStableTieBreaks() {
        val input = listOf(photo("1", "A.jpg", 100), photo("2", "B.jpg", null),
            photo("3", "C.jpg", 200), photo("4", "D.jpg", 200))
        assertEquals(listOf("4", "3", "1", "2"), SavedGalleryPhotoSort.NEWEST.sorted(input).ids())
        assertEquals(listOf("1", "3", "4", "2"), SavedGalleryPhotoSort.OLDEST.sorted(input).ids())
        assertEquals(listOf("1", "2", "3", "4"), input.ids())
    }

    @Test fun nameSortingUsesInvariantCaseAndRetainsAssetIdentity() {
        val input = listOf(photo("1", "z.jpg", 1), photo("2", "I.jpg", 1),
            photo("3", "iA.jpg", 1), photo("4", "I.jpg", 1))
        assertEquals(listOf("4", "2", "3", "1"), SavedGalleryPhotoSort.NAME.sorted(input).ids())
        SavedGalleryPhotoSort.entries.forEach {
            assertEquals(emptyList(), it.sorted(emptyList()))
            assertEquals(listOf(input.first()), it.sorted(listOf(input.first())))
        }
    }

    @Test fun foldersIncludeNestedPhotosWithoutPrefixSiblingCollisions() {
        val photos = listOf(photo("1", "A.jpg", 100, root + "a/nested/"),
            photo("2", "B.jpg", 200, root + "ab/"), photo("3", "C.jpg", 300, root))
        val result = savedGalleryChildFolders(root, listOf(root + "empty/"), photos,
            SavedGalleryFolderFilter.ALL, SavedGalleryFolderSort.NAME)
        assertEquals(listOf("a/", "ab/", "empty/"), result.map { it.path.removePrefix(root) })
        assertEquals(listOf(1, 1, 0), result.map { it.photoCount })
        assertEquals(listOf(100L, 200L, null), result.map { it.latestPhotoMillis })
    }

    @Test fun folderFiltersAndRecentSortKeepEmptyAndUnknownDatesLast() {
        val folders = listOf(root + "empty/", root + "unknown/", root + "old/", root + "new/")
        val photos = listOf(photo("1", "A.jpg", null, root + "unknown/"),
            photo("2", "B.jpg", 10, root + "old/"), photo("3", "C.jpg", 20, root + "new/"))
        fun paths(filter: SavedGalleryFolderFilter, sort: SavedGalleryFolderSort) =
            savedGalleryChildFolders(root, folders, photos, filter, sort).map { it.path.removePrefix(root) }
        assertEquals(listOf("new/", "old/", "empty/", "unknown/"), paths(SavedGalleryFolderFilter.ALL, SavedGalleryFolderSort.RECENT))
        assertEquals(listOf("new/", "old/", "unknown/"), paths(SavedGalleryFolderFilter.WITH_PHOTOS, SavedGalleryFolderSort.RECENT))
        assertEquals(listOf("empty/"), paths(SavedGalleryFolderFilter.EMPTY, SavedGalleryFolderSort.NAME))
        assertEquals(emptyList(), savedGalleryChildFolders(root + "new/", folders, photos,
            SavedGalleryFolderFilter.ALL, SavedGalleryFolderSort.NAME))
    }
}
