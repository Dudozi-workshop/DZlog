package com.dudoziworkshop.dzlog.data.media

import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.feature.log.policy.GalleryFolderNamePolicy

object SavedGalleryMovePolicy {
    /** Capture identities survive independent moves and changes to display names. */
    fun additionalOriginals(photos: List<SavedCaptureMedia>, selectedIds: Set<String>): List<SavedCaptureMedia> {
        val captures = photos.filter { it.asset.assetId in selectedIds && it.asset.target.role == CaptureMediaRole.RESULT }
            .map { it.captureId }.toSet()
        return photos.filter { it.captureId in captures && it.asset.target.role == CaptureMediaRole.ORIGINAL &&
            it.asset.assetId !in selectedIds && it.availability != CaptureMediaAvailability.MISSING }
    }

    fun destinations(
        photos: List<SavedCaptureMedia>, selectedIds: Set<String>, folder: String, includeOriginals: Boolean,
    ): Map<String, String> {
        require(folder.startsWith(SavedGalleryEntryPolicy.ROOT) && folder.endsWith('/') &&
            !GalleryFolderNamePolicy.isOriginalFolder(folder)) { "DZlog의 일반 폴더를 선택해 주세요." }
        val selected = photos.filter { it.asset.assetId in selectedIds }
        require(selectedIds.isNotEmpty() && selected.map { it.asset.assetId }.toSet() == selectedIds) {
            "선택한 사진을 다시 확인해 주세요."
        }
        val moving = if (includeOriginals) selected + additionalOriginals(photos, selectedIds) else selected
        require(moving.size <= 100) { "원본을 포함해 한 번에 최대 100장까지 이동할 수 있습니다." }
        require(moving.all { it.availability == CaptureMediaAvailability.PRESENT }) { "이동할 사진의 접근 권한을 확인해 주세요." }
        val paths = moving.associate { photo ->
            photo.asset.assetId to if (photo.asset.target.role == CaptureMediaRole.ORIGINAL) folder + "original/" else folder
        }
        require(moving.none { it.asset.target.relativePath == paths[it.asset.assetId] }) { "현재 폴더와 다른 위치를 선택해 주세요." }
        val names = moving.map { paths.getValue(it.asset.assetId) to it.asset.target.displayName }.toSet()
        require(names.size == moving.size && photos.none { it.asset.assetId !in paths &&
            (it.asset.target.relativePath to it.asset.target.displayName) in names }) { "대상 폴더에 같은 이름의 사진이 있습니다." }
        return paths
    }
}
