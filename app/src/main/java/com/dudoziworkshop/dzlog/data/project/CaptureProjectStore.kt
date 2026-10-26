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
 * Store only references to already committed MediaStore photos. Do not move or
 * delete photos here; session Undo remains separate and transient.
 */
class CaptureProjectStore(private val context: Context) {
    val changes: Flow<CaptureProjectRegistry> = context.dataStore.data.map { prefs ->
        decodeProjects(prefs[PROJECT_REGISTRY_KEY])
    }

    suspend fun load(): CaptureProjectRegistry = changes.first()

    suspend fun appendCommittedCapture(savedUris: List<String>): CaptureProjectRegistry {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        var committed: CaptureProjectRegistry? = null
        context.dataStore.edit { prefs ->
            val current = decodeProjects(prefs[PROJECT_REGISTRY_KEY])
            val next = current.appendCommittedCapture(savedUris, id, now)
            if (next != current) prefs[PROJECT_REGISTRY_KEY] = encodeProjects(next)
            committed = next
        }
        return requireNotNull(committed)
    }

    suspend fun completeActive(): CaptureProjectRegistry {
        val now = System.currentTimeMillis()
        var committed: CaptureProjectRegistry? = null
        context.dataStore.edit { prefs ->
            val current = decodeProjects(prefs[PROJECT_REGISTRY_KEY])
            val next = current.completeActive(now)
            if (next != current) prefs[PROJECT_REGISTRY_KEY] = encodeProjects(next)
            committed = next
        }
        return requireNotNull(committed)
    }

    suspend fun rename(projectId: String, name: String): CaptureProjectRegistry {
        var committed: CaptureProjectRegistry? = null
        context.dataStore.edit { prefs ->
            val current = decodeProjects(prefs[PROJECT_REGISTRY_KEY])
            val next = current.rename(projectId, name)
            if (next != current) prefs[PROJECT_REGISTRY_KEY] = encodeProjects(next)
            committed = next
        }
        return requireNotNull(committed)
    }
}

/** Reject malformed saved project data rather than silently erasing it. */
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
        )
    }
    val activeId = if (root.isNull("activeId")) null else root.getString("activeId")
    return CaptureProjectRegistry(projects, activeId)
}

private fun encodeProjects(value: CaptureProjectRegistry): String =
    JSONObject().apply {
        put("version", 1)
        put("activeId", value.activeId ?: JSONObject.NULL)
        put("projects", JSONArray().apply {
            value.projects.forEach { project ->
                put(JSONObject().apply {
                    put("id", project.id)
                    put("title", project.title)
                    put("createdAt", project.createdAt)
                    put("completedAt", project.completedAt ?: JSONObject.NULL)
                    put("captures", JSONArray().apply {
                        project.captureUriGroups.forEach { uris ->
                            put(JSONArray().apply { uris.forEach { uri -> put(uri) } })
                        }
                    })
                })
            }
        })
    }.toString()
