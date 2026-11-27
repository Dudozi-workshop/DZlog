package com.dudoziworkshop.dzlog.data.media

/**
 * Read-only virtual gallery entries matching Android's recent/default-storage
 * scopes. Classification uses logical paths, not capture role: a gallery move
 * may place an ORIGINAL at the root without changing its role or Photos asset.
 */
object SavedGalleryEntryPolicy {
    const val ROOT = "Pictures/DZlog/"
    const val ROOT_ORIGINALS = "Pictures/DZlog/original/"
    const val RECENT_PREVIEW_LIMIT = 9

    fun isDirectRootPhoto(relativePath: String): Boolean =
        relativePath.trimEnd('/') == ROOT.trimEnd('/')

    fun isDirectRootOriginalPhoto(relativePath: String): Boolean =
        relativePath.trimEnd('/') == ROOT_ORIGINALS.trimEnd('/')

    /** Includes original-only captures and both roles of paired captures. */
    fun recentAll(photos: List<SavedCaptureMedia>): List<SavedCaptureMedia> =
        SavedGalleryPhotoSort.NEWEST.sorted(photos)

    fun recentPreview(photos: List<SavedCaptureMedia>): List<SavedCaptureMedia> =
        recentAll(photos).take(RECENT_PREVIEW_LIMIT)

    /** Do not include any descendants, including the separate original folder. */
    fun defaultPhotos(photos: List<SavedCaptureMedia>): List<SavedCaptureMedia> =
        photos.filter { isDirectRootPhoto(it.asset.target.relativePath) }

    /** Direct originals only; nested custom-folder originals stay in their folders. */
    fun rootOriginalPhotos(photos: List<SavedCaptureMedia>): List<SavedCaptureMedia> =
        photos.filter { isDirectRootOriginalPhoto(it.asset.target.relativePath) }
}
