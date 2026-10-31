package com.dudoziworkshop.dzlog.data.project

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.domain.project.CaptureProject
import com.dudoziworkshop.dzlog.domain.project.CaptureProjectRegistry
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val PROJECT_REGISTRY_KEY = stringPreferencesKey("dzlog_capture_projects_v1")

/**
 * Transitional project metadata store. Preserve version-1 URI references from
 * prototype installs; an indexed CaptureRecord store will be added separately.
 * Neither this store nor project selection moves or deletes any media files.
 */
class CaptureProjectStore(private val context: Context) {
    val changes: Flow<CaptureProjectRegistry> = context.dataStore.data.map { prefs ->
        decodeProjects(prefs[PROJECT_REGISTRY_KEY])
    }

    suspend fun load(): CaptureProjectRegistry = changes.first()

    suspend fun createProject(name: String, initialTemplateId: String? = null): CaptureProjectRegistry {
        val projectId = UUID.randomUUID().toString()
        val createdAt = System.currentTimeMillis()
        return updateRegistry { it.createProject(projectId, name, createdAt, initialTemplateId) }
    }

    suspend fun selectProject(projectId: String?): CaptureProjectRegistry =
        updateRegistry { it.selectProject(projectId) }

    suspend fun rememberTemplate(templateId: String): CaptureProjectRegistry =
        updateRegistry { it.rememberTemplate(templateId) }

    /** Legacy callback; only an explicitly selected project may receive it. */
    suspend fun appendCommittedCapture(savedUris: List<String>): CaptureProjectRegistry =
        updateRegistry { it.appendCommittedCapture(savedUris) }

    suspend fun completeActive(): CaptureProjectRegistry {
        val timestamp = System.currentTimeMillis()
        return updateRegistry { it.completeActive(timestamp) }
    }

    suspend fun completeProject(projectId: String): CaptureProjectRegistry {
        val timestamp = System.currentTimeMillis()
        return updateRegistry { it.completeProject(projectId, timestamp) }
    }

    suspend fun reopenProject(projectId: String): CaptureProjectRegistry =
        updateRegistry { it.reopenProject(projectId) }

    suspend fun rename(projectId: String, name: String): CaptureProjectRegistry =
        updateRegistry { it.rename(projectId, name) }

    private suspend fun updateRegistry(
        transform: (CaptureProjectRegistry) -> CaptureProjectRegistry,
    ): CaptureProjectRegistry {
        var updated: CaptureProjectRegistry? = null
        context.dataStore.edit { prefs ->
            val current = decodeProjects(prefs[PROJECT_REGISTRY_KEY])
            val next = transform(current)
            if (next != current) prefs[PROJECT_REGISTRY_KEY] = encodeProjects(next)
            updated = next
        }
        return requireNotNull(updated)
    }
}

/** Invalid saved data throws instead of silently resetting existing projects. */
private fun decodeProjects(raw: String?): CaptureProjectRegistry {
    if (raw == null) return CaptureProjectRegistry()
    val root = JSONObject(raw)
    require(root.getInt("version") == 1) { "Unsupported capture project version" }
    val records = root.getJSONArray("projects")
    val projects = (0 until records.length()).map { index ->
        val entry = records.getJSONObject(index)
        val groups = entry.getJSONArray("captures")
        CaptureProject(
            id = entry.getString("id"),
            title = entry.getString("title"),
            createdAt = entry.getLong("createdAt"),
            completedAt = if (entry.isNull("completedAt")) null else entry.getLong("completedAt"),
            captureUriGroups = (0 until groups.length()).map { groupIndex ->
                val group = groups.getJSONArray(groupIndex)
                (0 until group.length()).map(group::getString)
            },
            lastTemplateId = entry.optionalString("lastTemplateId"),
        )
    }
    return CaptureProjectRegistry(
        projects = projects,
        activeId = root.optionalString("activeId"),
        generalLastTemplateId = root.optionalString("generalLastTemplateId"),
    )
}

private fun JSONObject.optionalString(key: String): String? =
    if (!has(key) || isNull(key)) null else getString(key)

private fun encodeProjects(value: CaptureProjectRegistry): String =
    JSONObject().apply {
        put("version", 1)
        put("activeId", value.activeId ?: JSONObject.NULL)
        put("generalLastTemplateId", value.generalLastTemplateId ?: JSONObject.NULL)
        put("projects", JSONArray().apply {
            value.projects.forEach { project ->
                put(JSONObject().apply {
                    put("id", project.id)
                    put("title", project.title)
                    put("createdAt", project.createdAt)
                    put("completedAt", project.completedAt ?: JSONObject.NULL)
                    put("lastTemplateId", project.lastTemplateId ?: JSONObject.NULL)
                    put("captures", JSONArray().apply {
                        project.captureUriGroups.forEach { uris ->
                            put(JSONArray().apply { uris.forEach { uri -> put(uri) } })
                        }
                    })
                })
            }
        })
    }.toString()
