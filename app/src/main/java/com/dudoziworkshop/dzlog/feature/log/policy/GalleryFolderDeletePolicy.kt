package com.dudoziworkshop.dzlog.feature.log.policy

data class GalleryFolderDeleteEntry(val documentId: String, val path: String, val mimeType: String) {
    val directory: Boolean get() = mimeType == "vnd.android.document/directory"
    val photo: Boolean get() = !directory && mimeType.startsWith("image/")
    val original: Boolean get() = path.split('/').dropLast(1).any { it.equals("original", true) }
}

data class GalleryFolderDeletePlan(val source: String, val entries: List<GalleryFolderDeleteEntry>) {
    val fileCount: Int get() = entries.count { !it.directory }
    val originalCount: Int get() = entries.count { it.photo && it.original }
    val resultCount: Int get() = entries.count { it.photo && !it.original }
    val otherCount: Int get() = fileCount - originalCount - resultCount
    val folderCount: Int get() = entries.count { it.directory && it.path != source && !it.original && !it.path.trimEnd('/').endsWith("/original", true) }
}

/** Exact subtree boundaries and immutable confirmation inventory, independent of Android I/O. */
object GalleryFolderDeletePolicy {
    fun source(path: String): String {
        val normalized = path.trimEnd('/') + "/"
        require(normalized.startsWith(GalleryFolderIndexPolicy.ROOT) && normalized != GalleryFolderIndexPolicy.ROOT) {
            "DZlog 내부의 하위 폴더만 삭제할 수 있습니다."
        }
        require(normalized.split('/').dropLast(1).none { it.isBlank() || it == "." || it == ".." || it.equals("original", true) }) {
            "삭제할 일반 폴더 경로를 확인해 주세요."
        }
        return normalized
    }

    fun plan(path: String, entries: List<GalleryFolderDeleteEntry>): GalleryFolderDeletePlan {
        val root = source(path)
        require(entries.size <= 5000) { "폴더 내용이 너무 많습니다. 하위 폴더를 나누어 삭제해 주세요." }
        require(entries.all { it.documentId.isNotBlank() && it.path.startsWith(root) }) { "삭제 대상이 폴더 범위를 벗어났습니다." }
        require(entries.map { it.documentId }.distinct().size == entries.size && entries.map { it.path }.distinct().size == entries.size) {
            "삭제 대상 파일 또는 폴더가 중복됐습니다."
        }
        require(entries.isEmpty() || entries.count { it.directory && it.path == root } == 1) { "삭제할 폴더를 확인하지 못했습니다." }
        return GalleryFolderDeletePlan(root, entries.toList())
    }

    fun requireUnchanged(confirmed: GalleryFolderDeletePlan, current: GalleryFolderDeletePlan) {
        require(confirmed.source == current.source && confirmed.entries.toSet() == current.entries.toSet()) {
            "폴더 내용이 변경됐습니다. 삭제 범위를 다시 확인해 주세요."
        }
    }
}
