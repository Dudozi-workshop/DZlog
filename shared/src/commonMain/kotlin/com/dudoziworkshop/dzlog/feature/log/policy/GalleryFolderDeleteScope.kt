package com.dudoziworkshop.dzlog.feature.log.policy

import com.dudoziworkshop.dzlog.data.media.SavedGalleryEntryPolicy

object GalleryFolderDeleteScope {
    const val MAX_ENTRIES = 5000

    fun source(path: String): String {
        val normalized = path.trimEnd('/') + "/"
        require(normalized.startsWith(SavedGalleryEntryPolicy.ROOT) && normalized != SavedGalleryEntryPolicy.ROOT) {
            "DZlog 내부의 하위 폴더만 삭제할 수 있습니다."
        }
        require(normalized.split('/').dropLast(1).none { it.isBlank() || it == "." || it == ".." || it.equals("original", true) }) {
            "삭제할 일반 폴더 경로를 확인해 주세요."
        }
        return normalized
    }
}
