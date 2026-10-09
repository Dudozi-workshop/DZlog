package com.dudoziworkshop.dzlog.feature.log.policy

/**
 * Gallery filesystem operations never rewrite the camera's saved capture path.
 * The caller must require explicit user confirmation before proceeding when
 * the source is selected as (or is an ancestor of) a capture destination.
 */
object GalleryFolderOperationPolicy {
    fun affectsCapturePath(
        sourceFolder: String,
        configuredCapturePaths: List<String>,
    ): Boolean {
        val source = sourceFolder.trimEnd('/') + "/"
        return configuredCapturePaths.any { configured ->
            val target = configured.trimEnd('/') + "/"
            target == source || target.startsWith(source)
        }
    }

    fun renamedPath(sourceFolder: String, rawName: String): String {
        val root = GalleryFolderIndexPolicy.ROOT
        val source = sourceFolder.trimEnd('/') + "/"
        require(source.startsWith(root) && source != root) {
            "DZlog 루트 폴더는 이름을 변경할 수 없습니다."
        }
        require(!source.trimEnd('/').endsWith("/original", ignoreCase = true)) {
            "원본사진 전용 폴더는 이름을 변경할 수 없습니다."
        }
        val name = GalleryFolderNamePolicy.normalize(rawName)
        val parent = requireNotNull(GalleryFolderIndexPolicy.parentOf(source))
        return parent + name + "/"
    }

    fun movedPath(sourceFolder: String, destinationFolder: String): String {
        validateMove(sourceFolder, destinationFolder)
        return destinationFolder.trimEnd('/') + "/" +
            sourceFolder.trimEnd('/').substringAfterLast('/') + "/"
    }

    fun validateMove(sourceFolder: String, destinationFolder: String) {
        val root = GalleryFolderIndexPolicy.ROOT
        val source = sourceFolder.trimEnd('/') + "/"
        val dest = destinationFolder.trimEnd('/') + "/"
        require(source.startsWith(root) && dest.startsWith(root) && source != root) {
            "DZlog 루트 폴더는 이동할 수 없습니다."
        }
        require(!source.trimEnd('/').endsWith("/original", ignoreCase = true)) {
            "원본사진 전용 폴더는 이동할 수 없습니다."
        }
        require(GalleryFolderIndexPolicy.parentOf(source) != dest) {
            "이미 같은 위치에 있는 폴더입니다."
        }
        require(!dest.startsWith(source)) {
            "폴더를 자기 자신 또는 자신의 하위 폴더로 이동할 수 없습니다."
        }
    }
}
