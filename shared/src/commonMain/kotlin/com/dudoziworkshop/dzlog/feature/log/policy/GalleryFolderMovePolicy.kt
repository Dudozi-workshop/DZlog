package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.data.media.SavedGalleryEntryPolicy

object GalleryFolderMovePolicy {
    fun movedPath(sourceFolder: String, destinationFolder: String): String {
        validateMove(sourceFolder, destinationFolder)
        return destinationFolder.trimEnd('/') + "/" +
            sourceFolder.trimEnd('/').substringAfterLast('/') + "/"
    }

    fun validateMove(sourceFolder: String, destinationFolder: String) {
        val root = SavedGalleryEntryPolicy.ROOT
        val source = sourceFolder.trimEnd('/') + "/"
        val dest = destinationFolder.trimEnd('/') + "/"
        require(source.startsWith(root) && dest.startsWith(root) && source != root) {
            "DZlog 루트 폴더는 이동할 수 없습니다."
        }
        require(!GalleryFolderNamePolicy.isOriginalFolder(source)) {
            "원본사진 전용 폴더는 이동할 수 없습니다."
        }
        require(source.trimEnd('/').substringBeforeLast('/') + "/" != dest) {
            "이미 같은 위치에 있는 폴더입니다."
        }
        require(!dest.startsWith(source)) {
            "폴더를 자기 자신 또는 자신의 하위 폴더로 이동할 수 없습니다."
        }
    }
}
