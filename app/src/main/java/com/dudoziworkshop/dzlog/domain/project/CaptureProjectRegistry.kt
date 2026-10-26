package com.dudoziworkshop.dzlog.domain.project

/**
 * A project is a logical collection of committed shutter presses, not a
 * directory or an Undo history. Original+result URIs stay in one capture group.
 */
data class CaptureProject(
    val id: String,
    val title: String,
    val createdAt: Long,
    val completedAt: Long? = null,
    val captureUriGroups: List<List<String>> = emptyList(),
) {
    val isActive: Boolean get() = completedAt == null
    val captureCount: Int get() = captureUriGroups.size
}

data class CaptureProjectRegistry(
    val projects: List<CaptureProject> = emptyList(),
    val activeId: String? = null,
) {
    init {
        require(projects.map { it.id }.distinct().size == projects.size) { "Duplicate project IDs" }
        require(activeId == null || projects.any { it.id == activeId && it.isActive }) {
            "Active project must exist and remain open"
        }
    }

    val active: CaptureProject? get() = projects.firstOrNull { it.id == activeId }

    /** Call only for a durable capture receipt; never for pending photo writes. */
    fun appendCommittedCapture(
        savedUris: List<String>,
        newProjectId: String,
        timestamp: Long,
    ): CaptureProjectRegistry {
        val group = savedUris.map(String::trim).filter(String::isNotEmpty).distinct()
        if (group.isEmpty()) return this
        val current = active
        if (current != null && current.captureUriGroups.any { it == group }) return this

        if (current == null) {
            require(newProjectId.isNotBlank() && projects.none { it.id == newProjectId })
            val fresh = CaptureProject(
                id = newProjectId,
                title = "프로젝트 ${projects.size + 1}",
                createdAt = timestamp,
                captureUriGroups = listOf(group),
            )
            return copy(projects = projects + fresh, activeId = fresh.id)
        }
        return copy(projects = projects.map { project ->
            if (project.id == current.id) project.copy(
                captureUriGroups = project.captureUriGroups + listOf(group),
            ) else project
        })
    }

    /** Completes the current project without deleting its images or files. */
    fun completeActive(timestamp: Long): CaptureProjectRegistry {
        val id = activeId ?: return this
        return copy(
            projects = projects.map { item ->
                if (item.id == id) item.copy(completedAt = timestamp) else item
            },
            activeId = null,
        )
    }

    fun rename(projectId: String, title: String): CaptureProjectRegistry {
        val normalized = title.trim().take(80)
        if (normalized.isBlank()) return this
        return copy(projects = projects.map { project ->
            if (project.id == projectId) project.copy(title = normalized) else project
        })
    }

    /** Browsing completed projects must never revive a capture Undo stack. */
    fun find(projectId: String): CaptureProject? = projects.firstOrNull { it.id == projectId }
}
