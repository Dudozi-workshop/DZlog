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

    /** Validate virtual-folder collisions before changing storage, then remap the whole catalog subtree. */
    @Synchronized
    fun relocateFolder(sourcePath: String, targetPath: String, changeStorage: () -> String): String {
        val source = normalizedParent(sourcePath)
        val target = normalizedParent(targetPath)
        require(source != GalleryFolderIndexPolicy.ROOT && target != GalleryFolderIndexPolicy.ROOT) {
            "DZlog 루트 폴더는 변경할 수 없습니다."
        }
        if (source == target) return source
        require(!target.startsWith(source)) { "자신의 하위 폴더로 이동할 수 없습니다." }
        val recorded = readPaths()
        val affected = recorded.filter { it.startsWith(source) }.toSet()
        val remaining = recorded - affected
        require(remaining.none { it.equals(target, true) || it.startsWith(target, true) }) {
            "대상 위치에 같은 이름의 폴더가 이미 등록되어 있습니다."
        }
        val updated = remaining + affected.map { target + it.removePrefix(source) }
        val actual = normalizedParent(changeStorage())
        check(actual == target) {
            "저장소에서 변경된 경로가 예상과 다릅니다. 파일 관리자에서 확인해 주세요."
        }
        if (affected.isNotEmpty()) {
            check(prefs.edit().putStringSet(PATHS_KEY, updated).commit()) {
                "저장소 폴더는 변경됐지만 앱 폴더 정보를 저장하지 못했습니다. 파일 관리자에서 확인해 주세요."
            }
        }
        return actual
    }

    /** Keep the whole catalog subtree unless storage verification confirms complete removal. */
    @Synchronized
    fun deleteFolder(sourcePath: String, deleteStorage: () -> Boolean): Boolean {
        val source = com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderDeletePolicy.source(sourcePath)
        if (!deleteStorage()) return false
        val recorded = readPaths()
        val remaining = recorded.filterNot { it.startsWith(source) }.toSet()
        if (remaining != recorded) {
            check(prefs.edit().putStringSet(PATHS_KEY, remaining).commit()) {
                "저장소 폴더는 삭제됐지만 앱 폴더 정보를 저장하지 못했습니다. 다시 시도해 주세요."
            }
        }
        return true
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
