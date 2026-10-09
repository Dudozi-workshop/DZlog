package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.domain.model.MediaImageItem

data class GalleryPhotoMovePlan(
    val items: List<MediaImageItem>,
    val destinations: Map<Long, String>,
    val pairedOriginalCount: Int,
)

/**
 * A historical original is paired only when its exact filename occurs once in
 * both the capture directory and its immediate original/ sibling. Never guess based
 * on timestamps, suffixes or positions.
 */
object GalleryPhotoMovePolicy {
    private const val ROOT = GalleryFolderIndexPolicy.ROOT

    fun pairedOriginals(
        selected: List<MediaImageItem>,
        allPhotos: List<MediaImageItem>,
    ): List<MediaImageItem> {
        val byFolderAndName = allPhotos.groupBy {
            (it.relativePath.trimEnd('/') + "/") to it.displayName
        }
        return selected.asSequence()
            .filterNot { isOriginalPath(it.relativePath) }
            .mapNotNull { photo ->
                val captures = byFolderAndName[
                    (photo.relativePath.trimEnd('/') + "/") to photo.displayName
                ].orEmpty()
                if (captures.singleOrNull()?.id != photo.id) return@mapNotNull null
                val siblings = byFolderAndName[
                    (photo.relativePath.trimEnd('/') + "/original/") to photo.displayName
                ].orEmpty()
                siblings.singleOrNull()
            }
            .distinctBy { it.id }
            .toList()
    }

    fun plan(
        selected: List<MediaImageItem>,
        pairedOriginals: List<MediaImageItem>,
        destination: String,
        includeOriginals: Boolean,
    ): GalleryPhotoMovePlan {
        require(selected.isNotEmpty()) { "이동할 사진을 선택해 주세요." }
        val root = destination.trimEnd('/') + "/"
        require(root.startsWith(ROOT) && !isOriginalPath(root)) {
            "DZlog 내부의 일반 폴더를 선택해 주세요."
        }
        val extra = if (includeOriginals) pairedOriginals else emptyList()
        val all = (selected + extra).distinctBy { it.id }
        require(all.size <= 100) { "한 번에 최대 100장까지 이동할 수 있습니다." }
        require(all.all { it.relativePath.startsWith(ROOT) }) {
            "DZlog 외부 사진은 이동할 수 없습니다."
        }
        val locations = all.associate { item ->
            item.id to if (isOriginalPath(item.relativePath)) root + "original/" else root
        }
        require(all.none { it.relativePath.trimEnd('/') + "/" == locations[it.id] }) {
            "현재 폴더와 다른 위치를 선택해 주세요."
        }
        val duplicateNames = all.groupBy { (locations[it.id] ?: "") to it.displayName }
            .filterValues { it.size > 1 }
        require(duplicateNames.isEmpty()) {
            "선택한 사진에 대상 폴더에서 파일명이 겹치는 항목이 있습니다."
        }
        return GalleryPhotoMovePlan(all, locations, extra.count { original ->
            selected.none { it.id == original.id }
        })
    }

    fun isOriginalPath(path: String): Boolean =
        path.trimEnd('/').endsWith("/original", ignoreCase = true)
}
