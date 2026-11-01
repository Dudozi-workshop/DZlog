package com.dudoziworkshop.dzlog.domain.project

/**
 * Physical project deletion is by exact, recorded media URI. No folder or
 * filename-prefix deletion, and no conversion into ordinary captures.
 */
internal data class ProjectMediaDeleteTargets(
    val targetUris: List<String>,
    val unresolvedUris: List<String>,
    val captureCount: Int,
) {
    val fileCount: Int get() = targetUris.size
    val allFilesAccountedFor: Boolean get() = unresolvedUris.isEmpty()
}

internal object ProjectMediaDeletePolicy {
    fun plan(
        projectId: String,
        registry: CaptureProjectRegistry,
        records: List<CaptureRecord>,
        availableUris: Set<String>,
    ): ProjectMediaDeleteTargets {
        val project = requireNotNull(registry.find(projectId)) { "Unknown project" }
        require(records.all { it.projectId == projectId }) {
            "Another project's captures cannot enter this deletion"
        }
        val recordedUris = (
            records.flatMap { it.mediaUris } +
                project.captureUriGroups.flatten()
        ).map(String::trim).filter(String::isNotBlank).distinct()
        return ProjectMediaDeleteTargets(
            targetUris = recordedUris.filter(availableUris::contains),
            unresolvedUris = recordedUris.filterNot(availableUris::contains),
            captureCount = records.size,
        )
    }

    /** Full deletion can finish only after verified absence, not user confirmation. */
    fun canFinalize(
        expectedUris: List<String>,
        verifiedAbsentUris: Set<String>,
        newCaptureIdsDetected: Boolean,
    ): Boolean = !newCaptureIdsDetected &&
        expectedUris.all(verifiedAbsentUris::contains)
}
