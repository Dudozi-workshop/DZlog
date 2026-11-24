package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummary
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderSummaryPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GallerySummaryImage

/** Sort copies only. Photos identifiers remain the selection/navigation identity. */
enum class SavedGalleryPhotoSort(val label: String) {
    NEWEST("최신순"), OLDEST("오래된순"), NAME("이름순");

    fun sorted(photos: List<SavedCaptureMedia>): List<SavedCaptureMedia> {
        val byId = compareByDescending<SavedCaptureMedia> { it.asset.assetId }
        val comparator = when (this) {
            // Old index entries can lack Photos metadata. Unknown dates belong
            // at the end in both directions, never masquerading as recent photos.
            NEWEST -> compareBy<SavedCaptureMedia> { it.createdAtMillis == null }
                .thenByDescending { it.createdAtMillis }.then(byId)
            OLDEST -> compareBy<SavedCaptureMedia> { it.createdAtMillis == null }
                .thenBy { it.createdAtMillis }.thenBy { it.asset.assetId }
            NAME -> compareBy<SavedCaptureMedia> { it.asset.target.displayName.lowercase() }
                .thenBy { it.asset.target.displayName }.then(byId)
        }
        return photos.sortedWith(comparator)
    }
}

enum class SavedGalleryFolderFilter(val label: String) {
    ALL("전체"), WITH_PHOTOS("사진 있음"), EMPTY("빈 폴더")
}

enum class SavedGalleryFolderSort(val label: String) {
    NAME("이름순"), RECENT("최근 사진순")
}

data class SavedGalleryFolderSummary(
    val path: String,
    val counts: GalleryFolderSummary,
    val cover: SavedCaptureMedia?,
) {
    val photoCount get() = counts.totalPhotoCount
    val latestPhotoMillis get() = counts.latestPhotoEpochMillis
}

/** Counts indexed photos in the whole subtree, independently of favorite view. */
fun savedGalleryChildFolders(
    parent: String,
    folders: List<String>,
    photos: List<SavedCaptureMedia>,
    filter: SavedGalleryFolderFilter,
    sort: SavedGalleryFolderSort,
): List<SavedGalleryFolderSummary> {
    require(parent.startsWith("Pictures/DZlog/") && parent.endsWith('/'))
    fun childOf(path: String): String? {
        if (path == parent || !path.startsWith(parent)) return null
        return parent + path.removePrefix(parent).substringBefore('/') + "/"
    }
    val photosByChild = photos.groupBy { childOf(it.asset.target.relativePath) }
    val pathsByChild = (folders + photos.map { it.asset.target.relativePath }).distinct().groupBy(::childOf)
    val summaries = pathsByChild.keys.filterNotNull().map { child ->
        val entries = photosByChild[child].orEmpty()
        val images = entries.map { entry ->
            GallerySummaryImage(entry.asset.assetId, entry.asset.target.relativePath,
                entry.asset.target.role == CaptureMediaRole.ORIGINAL, capturedAtMillis = entry.createdAtMillis,
                availableForCover = entry.availability == CaptureMediaAvailability.PRESENT)
        }
        val directChildren = pathsByChild.getValue(child).filter { it != child }.map { path ->
            child + path.removePrefix(child).substringBefore('/') + "/"
        }.distinct()
        val counts = GalleryFolderSummaryPolicy.summarize(child, images, directChildren)
        SavedGalleryFolderSummary(child, counts, entries.firstOrNull { it.asset.assetId == counts.coverImageId })
    }.filter {
        when (filter) {
            SavedGalleryFolderFilter.ALL -> true
            SavedGalleryFolderFilter.WITH_PHOTOS -> it.photoCount > 0
            SavedGalleryFolderFilter.EMPTY -> it.photoCount == 0
        }
    }
    val byName = compareBy<SavedGalleryFolderSummary> { it.path.lowercase() }.thenBy { it.path }
    return summaries.sortedWith(when (sort) {
        SavedGalleryFolderSort.NAME -> byName
        SavedGalleryFolderSort.RECENT -> compareBy<SavedGalleryFolderSummary> { it.latestPhotoMillis == null }
            .thenByDescending { it.latestPhotoMillis }.then(byName)
    })
}
