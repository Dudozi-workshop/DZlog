package com.dudoziworkshop.dzlog.data.media

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
    val photoCount: Int,
    val latestPhotoMillis: Long?,
)

/** Counts indexed photos in the whole subtree, independently of favorite view. */
fun savedGalleryChildFolders(
    parent: String,
    folders: List<String>,
    photos: List<SavedCaptureMedia>,
    filter: SavedGalleryFolderFilter,
    sort: SavedGalleryFolderSort,
): List<SavedGalleryFolderSummary> {
    require(parent.startsWith("Pictures/DZlog/") && parent.endsWith('/'))
    val children = (folders + photos.map { it.asset.target.relativePath }).mapNotNull { path ->
        if (!path.startsWith(parent) || path == parent) null
        else parent + path.removePrefix(parent).substringBefore('/') + "/"
    }.distinct()
    val summaries = children.map { child ->
        val entries = photos.filter { it.asset.target.relativePath.startsWith(child) }
        SavedGalleryFolderSummary(child, entries.size, entries.mapNotNull { it.createdAtMillis }.maxOrNull())
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
