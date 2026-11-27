package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.data.media.SavedGalleryEntryPolicy

object GalleryFolderMovePolicy {
    fun movedPath(sourceFolder: String, destinationFolder: String): String {
        validateMove(sourceFolder, destinationFolder)
        return destinationFolder.trimEnd('/') + "/" +
            sourceFolder.trimEnd('/').substringAfterLast('/') + "/"
    }

    /** Use the same directory checks while browsing and immediately before committing the move. */
    fun checkedMovedPath(sourceFolder: String, destinationFolder: String, knownFolders: Collection<String>): String {
        require(sourceFolder in knownFolders && destinationFolder in knownFolders) { "선택한 폴더를 찾을 수 없습니다." }
        require(!GalleryFolderNamePolicy.isOriginalFolder(destinationFolder)) { "원본사진 폴더는 이동 위치로 선택할 수 없습니다." }
        val target = movedPath(sourceFolder, destinationFolder)
        require(knownFolders.none { it.equals(target, ignoreCase = true) }) { "대상 위치에 같은 이름의 폴더가 있습니다." }
        return target
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
