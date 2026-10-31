package com.dudoziworkshop.dzlog.domain.project

/**
 * A named, persistent capture session. Media URI groups are retained for
 * backwards compatibility with the original DZQ-021 prototype; new capture
 * records will live in an independent indexed repository.
 */
data class CaptureProject(
    val id: String,
    val title: String,
    val createdAt: Long,
    val completedAt: Long? = null,
    val captureUriGroups: List<List<String>> = emptyList(),
    val lastTemplateId: String? = null,
) {
    /** Open/completed status is separate from the currently selected project. */
    val isActive: Boolean get() = completedAt == null
    val captureCount: Int get() = captureUriGroups.size
}

data class CaptureProjectRegistry(
    val projects: List<CaptureProject> = emptyList(),
    val activeId: String? = null,
    val generalLastTemplateId: String? = null,
) {
    init {
        require(projects.map { it.id }.distinct().size == projects.size) { "Duplicate project IDs" }
        require(activeId == null || projects.any { it.id == activeId && it.isActive }) {
            "Selected project must exist and remain open"
        }
        require(generalLastTemplateId == null || generalLastTemplateId.isNotBlank())
        require(projects.all { it.lastTemplateId == null || it.lastTemplateId.isNotBlank() })
    }

    val active: CaptureProject? get() = projects.firstOrNull { it.id == activeId }

    /** Creation alone never changes the current capture target. */
    fun createProject(
        projectId: String,
        name: String,
        timestamp: Long,
        initialTemplateId: String? = null,
    ): CaptureProjectRegistry {
        val normalizedName = name.trim().take(80)
        require(projectId.isNotBlank() && projects.none { it.id == projectId }) {
            "Project ID must be unique and nonblank"
        }
        require(normalizedName.isNotBlank()) { "A project name is required" }
        require(timestamp >= 0L)
        require(initialTemplateId == null || initialTemplateId.isNotBlank())
        return copy(projects = projects + CaptureProject(
            id = projectId,
            title = normalizedName,
            createdAt = timestamp,
            lastTemplateId = initialTemplateId,
        ))
    }

    /** A null selection explicitly returns to general shooting. */
    fun selectProject(projectId: String?): CaptureProjectRegistry {
        require(projectId == null || projects.any { it.id == projectId && it.isActive }) {
            "Only an open project can be selected"
        }
        return if (activeId == projectId) this else copy(activeId = projectId)
    }

    /** Templates are shared. Only the last selected template ID is remembered. */
    fun rememberTemplate(templateId: String): CaptureProjectRegistry {
        require(templateId.isNotBlank())
        val selected = active ?: return if (generalLastTemplateId == templateId) this
        else copy(generalLastTemplateId = templateId)
        if (selected.lastTemplateId == templateId) return this
        return copy(projects = projects.map { item ->
            if (item.id == selected.id) item.copy(lastTemplateId = templateId) else item
        })
    }

    /**
     * Legacy URI receipt path until CaptureRecord repository integration.
     * General shooting must not implicitly create or select a project.
     */
    fun appendCommittedCapture(savedUris: List<String>): CaptureProjectRegistry {
        val selected = active ?: return this
        val group = savedUris.map(String::trim).filter(String::isNotEmpty).distinct()
        if (group.isEmpty() || selected.captureUriGroups.any { it == group }) return this
        return copy(projects = projects.map { item ->
            if (item.id == selected.id) item.copy(
                captureUriGroups = item.captureUriGroups + listOf(group),
            ) else item
        })
    }

    fun completeActive(timestamp: Long): CaptureProjectRegistry =
        activeId?.let { completeProject(it, timestamp) } ?: this

    fun completeProject(projectId: String, timestamp: Long): CaptureProjectRegistry {
        require(timestamp >= 0L)
        val project = requireNotNull(find(projectId)) { "Unknown project" }
        if (!project.isActive) return this
        return copy(
            projects = projects.map { item ->
                if (item.id == projectId) item.copy(completedAt = timestamp) else item
            },
            activeId = activeId.takeUnless { it == projectId },
        )
    }

    /** Reopening never activates the project or changes the active template. */
    fun reopenProject(projectId: String): CaptureProjectRegistry {
        val project = requireNotNull(find(projectId)) { "Unknown project" }
        if (project.isActive) return this
        return copy(projects = projects.map { item ->
            if (item.id == projectId) item.copy(completedAt = null) else item
        })
    }

    fun rename(projectId: String, title: String): CaptureProjectRegistry {
        val normalized = title.trim().take(80)
        if (normalized.isBlank()) return this
        return copy(projects = projects.map { project ->
            if (project.id == projectId) project.copy(title = normalized) else project
        })
    }

    /** Browsing historical projects must never revive a camera Undo stack. */
    fun find(projectId: String): CaptureProject? = projects.firstOrNull { it.id == projectId }
}
