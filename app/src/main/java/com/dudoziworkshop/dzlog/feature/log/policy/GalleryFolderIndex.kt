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

data class GalleryBreadcrumb(val label: String, val relativePath: String)

object GalleryFolderIndexPolicy {
    const val ROOT = "Pictures/DZlog/"

    /** Folder navigation is based on the actual relative path. Root has no parent. */
    fun parentOf(relativePath: String): String? {
        val current = normalize(relativePath)
        require(current == ROOT || current.startsWith(ROOT)) {
            "Gallery folder must be inside Pictures/DZlog/"
        }
        if (current == ROOT) return null
        val tail = current.removePrefix(ROOT).trimEnd('/')
        val parent = tail.substringBeforeLast('/', missingDelimiterValue = "")
        return if (parent.isBlank()) ROOT else ROOT + parent + "/"
    }

    /** Includes a clickable root followed by every folder ancestor and current folder. */
    fun breadcrumbs(relativePath: String): List<GalleryBreadcrumb> {
        val path = normalize(relativePath)
        require(path == ROOT || path.startsWith(ROOT)) {
            "Gallery folder must be inside Pictures/DZlog/"
        }
        val segments = path.removePrefix(ROOT).trimEnd('/')
            .split('/').filter { it.isNotBlank() }
        val result = mutableListOf(GalleryBreadcrumb("DZlog", ROOT))
        var prefix = ROOT
        segments.forEach { segment ->
            prefix += "$segment/"
            result += GalleryBreadcrumb(segment, prefix)
        }
        return result
    }

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
