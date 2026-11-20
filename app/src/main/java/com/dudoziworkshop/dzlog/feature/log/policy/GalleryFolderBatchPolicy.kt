package com.dudoziworkshop.dzlog.feature.log.policy

import java.util.Locale

data class GalleryFolderRenameItem(val oldPath: String, val newName: String)

/** Conservatively reject overlap, descendants, virtual originals, and target collisions. */
object GalleryFolderBatchPolicy {
    private val root = GalleryFolderIndexPolicy.ROOT

    fun moveTargets(sources: List<String>, destination: String, allKnownPaths: Collection<String>): Map<String, String> {
        val selected = sources.map { it.trimEnd('/') + "/" }.distinct()
        require(selected.isNotEmpty() && selected.size <= 100) { "한 번에 1~100개 폴더를 선택해 주세요." }
        require(selected.none { source -> selected.any { other -> other != source && source.startsWith(other) } }) {
            "상위 폴더와 하위 폴더를 동시에 선택할 수 없습니다."
        }
        val targets = selected.associateWith { GalleryFolderOperationPolicy.movedPath(it, destination) }
        require(targets.values.map { it.lowercase(Locale.ROOT) }.distinct().size == targets.size) {
            "이동 후 폴더 이름이 중복됩니다."
        }
        val known = allKnownPaths.map { it.trimEnd('/') + "/" }
        require(targets.values.none { target -> known.any { existing -> existing.equals(target, true) } }) {
            "대상 위치에 같은 이름의 폴더가 있습니다."
        }
        return targets
    }

    fun renameTargets(sources: List<String>, knownSiblings: Collection<String>,
                      find: String, replacement: String): List<GalleryFolderRenameItem> {
        require(sources.isNotEmpty() && sources.size <= 100) { "한 번에 1~100개 폴더를 선택해 주세요." }
        val paths = sources.map { it.trimEnd('/') + "/" }.distinct()
        require(paths.size == sources.size && paths.all { it.startsWith(root) && it != root }) {
            "이름을 변경할 수 없는 폴더가 포함되어 있습니다."
        }
        val parentPaths = paths.map { GalleryFolderIndexPolicy.parentOf(it) }.distinct()
        require(parentPaths.size == 1) { "같은 위치의 폴더만 함께 이름을 변경할 수 있습니다." }
        val changes = paths.mapNotNull { path ->
            val current = path.trimEnd('/').substringAfterLast('/')
            val renamed = GalleryBatchRenamePolicy.renamedName(current, find, replacement, photo = false)
            if (current == renamed) null else GalleryFolderRenameItem(path, renamed)
        }
        require(changes.isNotEmpty()) { "변경할 문구가 포함된 폴더가 없습니다." }
        GalleryBatchRenamePolicy.requireNoCollisions(
            changes.map { it.oldPath.trimEnd('/').substringAfterLast('/') to it.newName },
            knownSiblings.map { it.trimEnd('/').substringAfterLast('/') },
        )
        return changes
    }
}
