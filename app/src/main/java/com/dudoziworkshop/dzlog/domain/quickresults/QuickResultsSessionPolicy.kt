package com.dudoziworkshop.dzlog.domain.quickresults

/**
 * Session captures are saved URI groups, oldest first. Each group represents
 * one shutter press, even if it produced both an original and an output.
 * This model intentionally does not depend on the separate gallery or Undo
 * counter policies.
 */
data class QuickResultPhoto(
    val uri: String,
    val displayName: String,
    val relativePath: String,
)

data class QuickResultCapture(
    val captureIndex: Int,
    val folderPath: String,
    val result: QuickResultPhoto?,
    val original: QuickResultPhoto?,
) {
    val displayedPhoto: QuickResultPhoto get() = result ?: requireNotNull(original)
    val isOriginalOnly: Boolean get() = result == null
}

data class QuickResultSection(
    val key: String,
    val folderPath: String,
    val captures: List<QuickResultCapture>,
)

object QuickResultsSessionPolicy {
    private fun isOriginal(path: String): Boolean =
        path.trimEnd('/').substringAfterLast('/').equals("original", ignoreCase = true)

    private fun parentFolder(path: String): String =
        if (isOriginal(path)) path.trimEnd('/').substringBeforeLast('/') + "/" else path

    /** Ignore missing/deleted URIs without inventing capture entries. */
    fun captures(
        savedUriGroups: List<List<String>>,
        availablePhotos: List<QuickResultPhoto>,
    ): List<QuickResultCapture> {
        val byUri = availablePhotos.associateBy { it.uri }
        return savedUriGroups.mapIndexedNotNull { index, group ->
            val found = group.distinct().mapNotNull(byUri::get)
            if (found.isEmpty()) return@mapIndexedNotNull null
            val result = found.firstOrNull { !isOriginal(it.relativePath) }
            val original = found.firstOrNull { isOriginal(it.relativePath) }
            val selected = result ?: original ?: return@mapIndexedNotNull null
            QuickResultCapture(
                captureIndex = index,
                folderPath = parentFolder(selected.relativePath),
                result = result,
                original = original,
            )
        }.asReversed()
    }

    /**
     * All-folders mode groups only adjacent captures with the same folder.
     * Filtered mode combines the matching captures into a single section.
     */
    fun sections(
        newestFirst: List<QuickResultCapture>,
        selectedFolder: String? = null,
    ): List<QuickResultSection> {
        if (selectedFolder != null) {
            val matches = newestFirst.filter { it.folderPath == selectedFolder }
            return if (matches.isEmpty()) emptyList() else listOf(
                QuickResultSection("filter:$selectedFolder", selectedFolder, matches)
            )
        }
        val sections = mutableListOf<QuickResultSection>()
        for (capture in newestFirst) {
            val previous = sections.lastOrNull()
            if (previous != null && previous.folderPath == capture.folderPath) {
                sections[sections.lastIndex] = previous.copy(captures = previous.captures + capture)
            } else {
                sections += QuickResultSection(
                    key = "segment:${capture.captureIndex}",
                    folderPath = capture.folderPath,
                    captures = listOf(capture),
                )
            }
        }
        return sections
    }

    fun folderChoices(newestFirst: List<QuickResultCapture>): List<String> =
        newestFirst.map { it.folderPath }.distinct()
}
