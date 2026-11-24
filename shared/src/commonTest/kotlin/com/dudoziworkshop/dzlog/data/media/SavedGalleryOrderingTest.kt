package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.PersistedCaptureAsset
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedGalleryOrderingTest {
    private val root = "Pictures/DZlog/"
    private fun photo(id: String, name: String, date: Long?, path: String = root,
        role: CaptureMediaRole = CaptureMediaRole.ORIGINAL) = SavedCaptureMedia(
        "capture-$id", PersistedCaptureAsset(CaptureMediaTarget(role, path, name), id),
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

    @Test fun coversPreferReadableDirectResultsThenNestedResultsThenOriginals() {
        val parent = root + "a/"
        val nested = parent + "child/"
        val direct = photo("direct", "direct.jpg", 10, parent, CaptureMediaRole.RESULT)
        val newer = photo("nested", "nested.jpg", 20, nested, CaptureMediaRole.RESULT)
        val original = photo("original", "original.jpg", 30, parent + "original/")
        val photos = listOf(original, newer, direct, direct)
        fun folder(entries: List<SavedCaptureMedia>) = savedGalleryChildFolders(root,
            listOf(nested, parent + "empty/deep/", parent + "original/"), entries,
            SavedGalleryFolderFilter.ALL, SavedGalleryFolderSort.NAME).single()

        val summary = folder(photos)
        assertEquals(3, summary.photoCount)
        assertEquals(1, summary.counts.originalPhotoCount)
        assertEquals(2, summary.counts.directChildFolderCount)
        assertEquals(direct, summary.cover)
        val unavailable = direct.copy(availability = CaptureMediaAvailability.INACCESSIBLE)
        assertEquals(newer, folder(listOf(unavailable, newer, original)).cover)
        assertEquals(original, folder(listOf(unavailable, original)).cover)
        assertEquals(null, folder(listOf(unavailable)).cover)
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

    @Test fun browserPinsOnlyDirectOriginalsBeforeRegularFoldersRegardlessOfControls() {
        val parent = root + "project/"
        val original = parent + "original/"
        val populated = parent + "aaa/"
        val empty = parent + "zzz/"
        val photos = listOf(
            photo("result", "Result.jpg", 10, parent, CaptureMediaRole.RESULT),
            photo("direct", "Original.jpg", 1, original),
            photo("child", "Later.jpg", 99, populated, CaptureMediaRole.RESULT),
            photo("elsewhere", "Elsewhere.jpg", 100, root + "project-other/original/"),
        )
        SavedGalleryFolderSort.entries.forEach { sort ->
            SavedGalleryFolderFilter.entries.forEach { filter ->
                val listing = savedGalleryFolderListing(parent, listOf(populated, empty), photos, filter, sort)
                assertEquals(SavedGalleryOriginalFolder(original, 1), listing.pinnedOriginal)
                assertEquals(false, listing.showsOriginalsDirectly)
                assertEquals(when (filter) {
                    SavedGalleryFolderFilter.ALL -> listOf(populated, empty)
                    SavedGalleryFolderFilter.WITH_PHOTOS -> listOf(populated)
                    SavedGalleryFolderFilter.EMPTY -> listOf(empty)
                }, listing.childFolders.map { it.path })
            }
        }
        val originals = savedGalleryFolderListing(original, emptyList(), photos,
            SavedGalleryFolderFilter.ALL, SavedGalleryFolderSort.NAME)
        assertEquals(null, originals.pinnedOriginal)
        assertEquals(false, originals.showsOriginalsDirectly)
    }

    @Test fun browserKeepsInlineOriginalsAndLegacyDescendantsWithoutAnEmptyReservedRow() {
        val parent = root + "project/"
        val original = parent + "original/"
        val legacy = original + "legacy/"
        val direct = photo("direct", "Original.jpg", 1, original)
        fun listing(base: String, folders: List<String>, photos: List<SavedCaptureMedia>) =
            savedGalleryFolderListing(base, folders, photos, SavedGalleryFolderFilter.EMPTY, SavedGalleryFolderSort.RECENT)

        val inline = listing(parent, listOf(original), listOf(direct))
        assertEquals(true, inline.showsOriginalsDirectly)
        assertEquals(null, inline.pinnedOriginal)
        assertEquals(emptyList(), inline.childFolders)
        val empty = listing(parent, listOf(original), emptyList())
        assertEquals(false, empty.showsOriginalsDirectly)
        assertEquals(null, empty.pinnedOriginal)
        assertEquals(emptyList(), empty.childFolders)
        assertEquals(SavedGalleryOriginalFolder(original, 1),
            listing(parent, listOf(original, legacy), listOf(direct)).pinnedOriginal)
        val legacyPhoto = photo("legacy", "Legacy.jpg", 2, legacy)
        assertEquals(SavedGalleryOriginalFolder(original, 0),
            listing(parent, emptyList(), listOf(legacyPhoto)).pinnedOriginal)
        assertEquals(listOf(legacy), listing(original, listOf(legacy), emptyList()).childFolders.map { it.path })
        val legacyCase = parent + "Original/"
        assertEquals(listOf(legacyCase), listing(parent, listOf(legacyCase), emptyList()).childFolders.map { it.path })
        assertEquals(SavedGalleryOriginalFolder(root + "original/", 1),
            listing(root, emptyList(), listOf(photo("root", "Root.jpg", 1, root + "original/"))).pinnedOriginal)
    }
}
