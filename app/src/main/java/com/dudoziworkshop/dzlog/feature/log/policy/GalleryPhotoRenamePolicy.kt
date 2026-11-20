package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.MediaImageItem

data class GalleryPhotoRenamePlan(
    val items: List<MediaImageItem>,
    val newNames: Map<Long, String>,
    val pairedCount: Int,
)

object GalleryPhotoRenamePolicy {
    /** Include exact original/result counterparts to preserve existing pairing. */
    fun plan(selected: List<MediaImageItem>, allPhotos: List<MediaImageItem>,
             find: String, replacement: String): GalleryPhotoRenamePlan {
        require(selected.isNotEmpty()) { "이름을 변경할 사진을 선택해 주세요." }
        val indexed = allPhotos.groupBy { (it.relativePath.trimEnd('/') + "/") to it.displayName }
        val chosen = selected.toMutableList()
        for (item in selected) {
            val path = item.relativePath.trimEnd('/') + "/"
            val otherPath = if (GalleryPhotoMovePolicy.isOriginalPath(path)) {
                GalleryFolderIndexPolicy.parentOf(path) ?: ""
            } else path + "original/"
            val same = indexed[path to item.displayName].orEmpty()
            val counterpart = indexed[otherPath to item.displayName].orEmpty()
            if (same.size == 1 && counterpart.size == 1) chosen += counterpart.single()
        }
        val items = chosen.distinctBy { it.id }
        require(items.size <= 100) { "원본 포함 100개 파일까지 일괄 변경할 수 있습니다." }
        require(items.all { it.relativePath.startsWith(GalleryFolderIndexPolicy.ROOT) }) {
            "DZlog 외부 사진은 변경할 수 없습니다."
        }
        val names = items.mapNotNull { photo ->
            val renamed = GalleryBatchRenamePolicy.renamedName(photo.displayName, find, replacement, photo = true)
            if (renamed == photo.displayName) null else photo to renamed
        }
        require(names.isNotEmpty()) { "변경할 문구가 포함된 사진이 없습니다." }
        for (path in names.map { it.first.relativePath }.distinct()) {
            val local = names.filter { it.first.relativePath == path }
            GalleryBatchRenamePolicy.requireNoCollisions(
                local.map { it.first.displayName to it.second },
                allPhotos.filter { it.relativePath == path }.map { it.displayName },
            )
        }
        return GalleryPhotoRenamePlan(names.map { it.first }, names.associate { it.first.id to it.second },
            names.count { entry -> selected.none { it.id == entry.first.id } })
    }
}
