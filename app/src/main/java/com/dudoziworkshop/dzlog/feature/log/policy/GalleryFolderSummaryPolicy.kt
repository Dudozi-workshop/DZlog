package com.dudoziworkshop.dzlog.feature.log.policy

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

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

        val cover = inScope.asSequence()
            .filterNot { it.isOriginal }
            .sortedWith(
                compareByDescending<GallerySummaryImage> { effectiveDate(it) ?: Long.MIN_VALUE }
                    .thenBy { it.stableId }
            ).firstOrNull()?.stableId

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

    fun compactDate(
        epochMillis: Long?,
        referenceMillis: Long,
        timeZone: TimeZone,
    ): String {
        if (epochMillis == null || epochMillis <= 0L) return "—"
        val taken = Calendar.getInstance(timeZone).apply { timeInMillis = epochMillis }
        val reference = Calendar.getInstance(timeZone).apply { timeInMillis = referenceMillis }
        val pattern = if (taken.get(Calendar.YEAR) == reference.get(Calendar.YEAR)) "MM.dd" else "yy.MM.dd"
        return SimpleDateFormat(pattern, Locale.KOREA).apply { this.timeZone = timeZone }
            .format(Date(epochMillis))
    }
}
