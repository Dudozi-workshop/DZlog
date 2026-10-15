package com.dudoziworkshop.dzlog.feature.log.policy

/**
 * An original/ grid is a drill-down only when its folder has result photos.
 * With originals only, returning from original/ skips the empty result grid.
 * The root's "default storage" entry is virtual; the actual paths never change.
 */
object GalleryOriginalNavigationPolicy {
    fun returnFolderPath(originalPath: String, parentHasResults: Boolean): String {
        require(originalPath.trimEnd('/').substringAfterLast('/').equals("original", ignoreCase = true)) {
            "Expected an original/ gallery folder"
        }
        val parent = requireNotNull(GalleryFolderIndexPolicy.parentOf(originalPath))
        return if (parentHasResults) parent
        else GalleryFolderIndexPolicy.parentOf(parent) ?: GalleryFolderIndexPolicy.ROOT
    }

    fun returnToDefaultStorage(originalPath: String, parentHasResults: Boolean): Boolean =
        parentHasResults &&
            GalleryFolderIndexPolicy.parentOf(originalPath) == GalleryFolderIndexPolicy.ROOT
}
