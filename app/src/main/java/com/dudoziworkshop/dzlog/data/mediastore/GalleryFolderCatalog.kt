package com.dudoziworkshop.dzlog.data.mediastore

import android.content.Context
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderIndexPolicy
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderNamePolicy

/**
 * Persistent gallery folder entries independent of MediaStore.
 *
 * Android's photo provider does not expose empty directories. Newly created
 * entries are visible in DZlog immediately; a real Pictures/DZlog directory
 * is materialized by MediaStore when the first photo is saved there.
 */
internal class GalleryFolderCatalog(context: Context) {
    private val prefs = context.getSharedPreferences("dzlog_gallery_folder_catalog", Context.MODE_PRIVATE)

    @Synchronized
    fun listImmediatePaths(parentPath: String): List<String> {
        val parent = normalizedParent(parentPath)
        return readPaths().filter { path ->
            path.startsWith(parent) &&
                path != parent &&
                '/' !in path.removePrefix(parent).trimEnd('/')
        }.sorted()
    }

    /** Include empty descendants so a parent card can count its own child folders. */
    @Synchronized
    fun listDescendantPaths(parentPath: String): List<String> {
        val parent = normalizedParent(parentPath)
        return readPaths().filter { it.startsWith(parent) && it != parent }.sorted()
    }

    @Synchronized
    fun create(parentPath: String, rawName: String, visibleSiblingPaths: List<String>): String {
        val parent = normalizedParent(parentPath)
        val name = GalleryFolderNamePolicy.normalize(rawName)
        val destination = parent + name + "/"
        val recorded = readPaths()
        require((visibleSiblingPaths + recorded.toList()).none { it.equals(destination, ignoreCase = true) }) {
            "같은 이름의 폴더가 이미 존재합니다."
        }
        check(prefs.edit().putStringSet(PATHS_KEY, recorded + destination).commit()) {
            "폴더 정보를 저장하지 못했습니다."
        }
        return destination
    }

    private fun readPaths(): Set<String> =
        prefs.getStringSet(PATHS_KEY, emptySet()).orEmpty().toSet()

    private fun normalizedParent(path: String): String {
        val parent = path.trimEnd('/') + "/"
        require(parent == GalleryFolderIndexPolicy.ROOT ||
            parent.startsWith(GalleryFolderIndexPolicy.ROOT)) {
            "DZlog 폴더 밖에는 생성할 수 없습니다."
        }
        return parent
    }

    private companion object {
        const val PATHS_KEY = "created_gallery_folder_paths"
    }
}
