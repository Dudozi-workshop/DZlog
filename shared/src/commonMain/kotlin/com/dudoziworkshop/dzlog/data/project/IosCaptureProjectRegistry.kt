package com.dudoziworkshop.dzlog.data.project

import com.dudoziworkshop.dzlog.data.json.CompatJsonArray
import com.dudoziworkshop.dzlog.data.json.CompatJsonObject

/**
 * Platform-neutral iOS project metadata.
 *
 * Android keeps its project registry in the app module. This separate model lets the
 * iOS migration match that behavior without changing Android storage or runtime code.
 */
data class IosCaptureProject(
    val id: String,
    val title: String,
    val createdAt: Long,
    val completedAt: Long? = null,
    val lastTemplateId: String? = null,
    val captureIds: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank())
        require(title.isNotBlank() && title.length <= 80)
        require(createdAt >= 0L)
        require(completedAt == null || completedAt >= 0L)
        require(lastTemplateId == null || lastTemplateId.isNotBlank())
        require(captureIds.none(String::isBlank) && captureIds.distinct().size == captureIds.size)
    }

    val isOpen: Boolean get() = completedAt == null
}

data class IosCaptureProjectRegistry(
    val projects: List<IosCaptureProject> = emptyList(),
    val activeId: String? = null,
    val generalLastTemplateId: String? = null,
) {
    init {
        require(projects.map { it.id }.distinct().size == projects.size)
        require(activeId == null || projects.any { it.id == activeId && it.isOpen })
        require(generalLastTemplateId == null || generalLastTemplateId.isNotBlank())
    }

    val active: IosCaptureProject? get() = projects.firstOrNull { it.id == activeId }

    fun find(projectId: String): IosCaptureProject? = projects.firstOrNull { it.id == projectId }

    fun create(projectId: String, title: String, timestamp: Long, templateId: String?): IosCaptureProjectRegistry {
        val normalized = title.trim().take(80)
        require(projectId.isNotBlank() && projects.none { it.id == projectId })
        require(normalized.isNotBlank())
        require(timestamp >= 0L)
        require(templateId == null || templateId.isNotBlank())
        return copy(projects = projects + IosCaptureProject(
            id = projectId,
            title = normalized,
            createdAt = timestamp,
            lastTemplateId = templateId,
        ))
    }

    /** Creation and browsing do not implicitly change the current capture target. */
    fun select(projectId: String?): IosCaptureProjectRegistry {
        require(projectId == null || projects.any { it.id == projectId && it.isOpen })
        return if (projectId == activeId) this else copy(activeId = projectId)
    }

    fun rememberTemplate(templateId: String): IosCaptureProjectRegistry {
        require(templateId.isNotBlank())
        val selected = active
        return if (selected == null) {
            if (generalLastTemplateId == templateId) this else copy(generalLastTemplateId = templateId)
        } else if (selected.lastTemplateId == templateId) {
            this
        } else {
            copy(projects = projects.map { project ->
                if (project.id == selected.id) project.copy(lastTemplateId = templateId) else project
            })
        }
    }

    fun appendCapture(captureId: String): IosCaptureProjectRegistry {
        require(captureId.isNotBlank())
        val selected = active ?: return this
        if (captureId in selected.captureIds) return this
        return copy(projects = projects.map { project ->
            if (project.id == selected.id) project.copy(captureIds = project.captureIds + captureId) else project
        })
    }

    fun removeCapture(captureId: String): IosCaptureProjectRegistry {
        require(captureId.isNotBlank())
        if (projects.none { captureId in it.captureIds }) return this
        return copy(projects = projects.map { project ->
            if (captureId in project.captureIds) {
                project.copy(captureIds = project.captureIds.filterNot { it == captureId })
            } else project
        })
    }

    fun complete(projectId: String, timestamp: Long): IosCaptureProjectRegistry {
        require(timestamp >= 0L)
        val project = requireNotNull(find(projectId))
        if (!project.isOpen) return this
        return copy(
            projects = projects.map { item ->
                if (item.id == projectId) item.copy(completedAt = timestamp) else item
            },
            activeId = activeId.takeUnless { it == projectId },
        )
    }

    fun reopen(projectId: String): IosCaptureProjectRegistry {
        val project = requireNotNull(find(projectId))
        if (project.isOpen) return this
        return copy(projects = projects.map { item ->
            if (item.id == projectId) item.copy(completedAt = null) else item
        })
    }

    fun rename(projectId: String, title: String): IosCaptureProjectRegistry {
        requireNotNull(find(projectId))
        val normalized = title.trim().take(80)
        if (normalized.isBlank()) return this
        return copy(projects = projects.map { item ->
            if (item.id == projectId) item.copy(title = normalized) else item
        })
    }

    /** Project metadata deletion never deletes or reclassifies Photos assets. */
    fun delete(projectId: String): IosCaptureProjectRegistry {
        requireNotNull(find(projectId))
        return copy(
            projects = projects.filterNot { it.id == projectId },
            activeId = activeId.takeUnless { it == projectId },
        )
    }
}

object IosCaptureProjectRegistryCodec {
    private const val SCHEMA = 1

    fun encode(value: IosCaptureProjectRegistry): String {
        val projects = CompatJsonArray()
        value.projects.forEach { project ->
            val captures = CompatJsonArray()
            project.captureIds.forEach { captureId -> captures.put(captureId) }
            projects.put(CompatJsonObject()
                .put("id", project.id)
                .put("title", project.title)
                .put("createdAt", project.createdAt)
                .put("completedAt", project.completedAt ?: CompatJsonObject.NULL)
                .put("lastTemplateId", project.lastTemplateId ?: CompatJsonObject.NULL)
                .put("captureIds", captures))
        }
        return CompatJsonObject()
            .put("schema", SCHEMA)
            .put("projects", projects)
            .put("activeId", value.activeId ?: CompatJsonObject.NULL)
            .put("generalLastTemplateId", value.generalLastTemplateId ?: CompatJsonObject.NULL)
            .toString()
    }

    fun decode(json: String): IosCaptureProjectRegistry {
        val root = CompatJsonObject(json)
        require(root.getInt("schema") == SCHEMA)
        val entries = root.getJSONArray("projects")
        val projects = (0 until entries.length()).map { index ->
            val item = entries.getJSONObject(index)
            val captures = item.getJSONArray("captureIds")
            IosCaptureProject(
                id = item.getString("id"),
                title = item.getString("title"),
                createdAt = item.optLong("createdAt", -1L),
                completedAt = item.nullableLong("completedAt"),
                lastTemplateId = item.nullableString("lastTemplateId"),
                captureIds = (0 until captures.length()).map(captures::getString),
            )
        }
        return IosCaptureProjectRegistry(
            projects = projects,
            activeId = root.nullableString("activeId"),
            generalLastTemplateId = root.nullableString("generalLastTemplateId"),
        )
    }

    private fun CompatJsonObject.nullableString(key: String): String? =
        if (opt(key) == null || opt(key) == CompatJsonObject.NULL) null else getString(key)

    private fun CompatJsonObject.nullableLong(key: String): Long? =
        if (opt(key) == null || opt(key) == CompatJsonObject.NULL) null else optLong(key, -1L)
}
