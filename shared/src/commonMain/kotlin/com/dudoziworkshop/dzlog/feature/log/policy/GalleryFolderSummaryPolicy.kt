package com.dudoziworkshop.dzlog.feature.log.policy

/**
 * Platform-independent image metadata for gallery folder summaries.
 * Paths refer to actual directory locations, not UI folder labels.
 */
data class GallerySummaryImage(
    val stableId: String,
    val directoryPath: String,
    val isOriginal: Boolean,
    val capturedAtMillis: Long? = null,
    val addedAtMillis: Long? = null,
    val availableForCover: Boolean = true,
)

data class GalleryFolderSummary(
    val totalPhotoCount: Int,
    val directPhotoCount: Int,
    val originalPhotoCount: Int,
    val directChildFolderCount: Int,
    val latestPhotoEpochMillis: Long?,
    val coverImageId: String?,
)

/**
 * Computes folder metadata without Android/UI dependencies.
 * Include storage-backed empty folder paths to count empty child folders.
 */
object GalleryFolderSummaryPolicy {
    fun summarize(
        folderPath: String,
        images: List<GallerySummaryImage>,
        folderPaths: Collection<String>,
    ): GalleryFolderSummary {
        val target = normalizeFolderPath(folderPath)
        val inScope = images.asSequence()
            .filter { belongsTo(target, normalizeFolderPath(it.directoryPath)) }
            .distinctBy { it.stableId }
            .toList()

        val directChildren = folderPaths.asSequence()
            .map(::normalizeFolderPath)
            .filter { isDirectChild(target, it) }
            .filterNot { it.substringAfterLast('/').equals("original", ignoreCase = true) }
            .distinct()
            .count()

        // Prefer this folder's results, then descendant results, then originals.
        // The summary already holds the complete snapshot; no extra storage query is needed.
        val cover = inScope.asSequence().filter { it.availableForCover }.minWithOrNull(
            compareBy<GallerySummaryImage> {
                when {
                    it.isOriginal -> 2
                    normalizeFolderPath(it.directoryPath) == target -> 0
                    else -> 1
                }
            }.thenByDescending { effectiveDate(it) ?: Long.MIN_VALUE }
                .thenBy { it.stableId }
        )?.stableId

        return GalleryFolderSummary(
            totalPhotoCount = inScope.size,
            directPhotoCount = inScope.count { normalizeFolderPath(it.directoryPath) == target },
            originalPhotoCount = inScope.count { it.isOriginal },
            directChildFolderCount = directChildren,
            latestPhotoEpochMillis = inScope.mapNotNull(::effectiveDate).maxOrNull(),
            coverImageId = cover,
        )
    }

    /** Capture time is preferred when available; otherwise use MediaStore added time. */
    private fun effectiveDate(image: GallerySummaryImage): Long? =
        image.capturedAtMillis?.takeIf { it > 0L }
            ?: image.addedAtMillis?.takeIf { it > 0L }

    fun normalizeFolderPath(path: String): String =
        path.replace('\\', '/').trim('/')
            .split('/').filter { it.isNotBlank() }.joinToString("/")

    private fun belongsTo(parent: String, child: String): Boolean =
        parent.isEmpty() || child == parent || child.startsWith("$parent/")

    private fun isDirectChild(parent: String, child: String): Boolean {
        if (child == parent || !belongsTo(parent, child)) return false
        val remainder = if (parent.isEmpty()) child else child.removePrefix("$parent/")
        return remainder.isNotBlank() && !remainder.contains('/')
    }
}
