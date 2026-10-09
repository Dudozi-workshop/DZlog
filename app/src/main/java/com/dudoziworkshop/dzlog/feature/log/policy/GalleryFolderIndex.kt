package com.dudoziworkshop.dzlog.feature.log.policy

/**
 * Folder hierarchy is derived from actual relative paths, never from G1/G2 labels.
 * An empty folder can be supplied by the storage-directory enumerator even when it has no images.
 */
data class GalleryChildFolder(
    val name: String,
    val relativePath: String,
    val totalImageCount: Int,
)

data class GalleryFolderIndex(
    val relativePath: String,
    val directImageCount: Int,
    val directOriginalCount: Int,
    val children: List<GalleryChildFolder>,
)

object GalleryFolderIndexPolicy {
    const val ROOT = "Pictures/DZlog/"

    fun index(
        currentRelativePath: String,
        imageRelativePaths: List<String>,
        existingFolderPaths: List<String> = emptyList(),
    ): GalleryFolderIndex {
        val current = normalize(currentRelativePath)
        require(current == ROOT || current.startsWith(ROOT)) {
            "Gallery folder must be inside Pictures/DZlog/"
        }

        val folderTotals = linkedMapOf<String, Int>()
        var directCount = 0
        var directOriginal = 0
        val originals = current + "original/"

        for (imagePath in imageRelativePaths) {
            val path = normalize(imagePath)
            if (!path.startsWith(current)) continue
            val tail = path.removePrefix(current)
            if (tail.isEmpty()) {
                directCount++
                continue
            }
            if (tail == "original/") {
                directOriginal++
                continue
            }
            val child = tail.substringBefore('/')
            if (child.isBlank() || child.equals("original", ignoreCase = true)) continue
            folderTotals[child] = (folderTotals[child] ?: 0) + 1
        }

        for (folderPath in existingFolderPaths) {
            val path = normalize(folderPath)
            if (!path.startsWith(current) || path == current) continue
            val child = path.removePrefix(current).substringBefore('/')
            if (child.isNotBlank() && !child.equals("original", ignoreCase = true)) {
                folderTotals.putIfAbsent(child, 0)
            }
        }

        return GalleryFolderIndex(
            relativePath = current,
            directImageCount = directCount,
            directOriginalCount = directOriginal,
            children = folderTotals.map { (name, count) ->
                GalleryChildFolder(
                    name = name,
                    relativePath = current + name + "/",
                    totalImageCount = count,
                )
            }.sortedBy { it.name.lowercase() },
        )
    }

    private fun normalize(path: String): String {
        val trimmed = path.trim().trimStart('/').replace('\\', '/')
        return trimmed.trimEnd('/') + "/"
    }
}
