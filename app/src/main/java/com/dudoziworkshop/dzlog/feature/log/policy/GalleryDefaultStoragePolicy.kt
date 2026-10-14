package com.dudoziworkshop.dzlog.feature.log.policy

/** The "basic save" gallery entry is virtual: never reparent or rename actual files. */
object GalleryDefaultStoragePolicy {
    fun isDirectRootPhoto(relativePath: String): Boolean =
        relativePath.trimEnd('/') == GalleryFolderIndexPolicy.ROOT.trimEnd('/')
}
