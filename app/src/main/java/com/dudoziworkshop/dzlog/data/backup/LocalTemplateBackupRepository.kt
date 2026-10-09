package com.dudoziworkshop.dzlog.data.backup

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.datastore.preferences.core.edit
import com.dudoziworkshop.dzlog.data.preferences.KEY_ACTIVE_TABLE_TEMPLATE_ID
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATES_JSON
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_DETAIL_GRID_ENABLED
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_BG_ALPHA
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TABLE_BG_STYLE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_ALIGN
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MANUAL
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_TEXT_COLOR_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_WM_VALUE_SCALE
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesFromJson
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesToJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** SAF I/O + one DataStore transaction. The UI requests confirmation before import. */
class LocalTemplateBackupRepository(private val context: Context) {
    suspend fun exportArchive(): String = withContext(Dispatchers.IO) {
        val snapshot = currentCatalog()
        LocalTemplateBackupCodec.encode(snapshot.first, snapshot.second)
    }

    suspend fun writeTo(uri: Uri, archive: String): Unit = withContext(Dispatchers.IO) {
        require(archive.toByteArray(Charsets.UTF_8).size <= LocalTemplateBackupCodec.MAX_ARCHIVE_BYTES)
        val stream = requireNotNull(context.contentResolver.openOutputStream(uri, "w")) {
            "파일을 저장할 수 없습니다."
        }
        stream.use {
            it.write(archive.toByteArray(Charsets.UTF_8))
            it.flush()
        }
    }

    suspend fun readFrom(uri: Uri): LocalTemplateBackup = withContext(Dispatchers.IO) {
        val stream = requireNotNull(context.contentResolver.openInputStream(uri)) {
            "파일을 열 수 없습니다."
        }
        val bytes = stream.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8192)
            while (true) {
                val count = input.read(chunk)
                if (count < 0) break
                require(output.size() + count <= LocalTemplateBackupCodec.MAX_ARCHIVE_BYTES) {
                    "백업 파일이 허용 크기를 초과했습니다."
                }
                output.write(chunk, 0, count)
            }
            output.toByteArray()
        }
        LocalTemplateBackupCodec.decode(bytes.toString(Charsets.UTF_8))
    }

    fun nameFrom(uri: Uri): String {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "선택한 백업 파일"
    }

    suspend fun preview(archive: LocalTemplateBackup): LocalTemplateMergePreview {
        val (items, _) = currentCatalog()
        return LocalTemplateBackupMerge.preview(items, archive)
    }

    /**
     * Merge against the **current** persisted catalog inside the edit transaction.
     * Never persist stale preview data or mutate existing template IDs.
     */
    suspend fun importConfirmed(archive: LocalTemplateBackup): LocalTemplateMergeResult =
        withContext(Dispatchers.IO) {
            var imported: LocalTemplateMergeResult? = null
            context.dataStore.edit { prefs ->
                val stored = requireNotNull(prefs[KEY_TABLE_TEMPLATES_JSON]) {
                    "현재 템플릿 저장소를 찾을 수 없습니다."
                }
                val existing = requireNotNull(savedTableTemplatesFromJson(stored)) {
                    "현재 템플릿 저장소가 손상되었습니다."
                }
                val originalActive = prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID]
                    ?.takeIf { id -> existing.any { it.id == id } }
                    ?: existing.maxByOrNull { it.modifiedAt }?.id
                val result = LocalTemplateBackupMerge.merge(existing, originalActive, archive)
                val serialized = savedTableTemplatesToJson(result.templates)
                // Compute data first; only apply after the full merge has succeeded.
                prefs[KEY_TABLE_TEMPLATES_JSON] = serialized
                val newActive = result.templates.firstOrNull { it.id == result.activeTemplateId }
                if (newActive != null) {
                    prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID] = newActive.id
                    prefs[KEY_TABLE_TEMPLATE_JSON] = newActive.templateState.toJsonString()
                    if (existing.isEmpty()) {
                        val style = newActive.styleState
                        prefs[KEY_WM_TABLE_BG_STYLE] = style.bgStyle
                        prefs[KEY_WM_BG_ALPHA] = style.bgAlpha
                        prefs[KEY_WM_VALUE_SCALE] = style.valueScale
                        prefs[KEY_WM_TEXT_COLOR_MODE] = style.textColorMode
                        prefs[KEY_WM_TEXT_COLOR_MANUAL] = style.manualTextColor
                        prefs[KEY_WM_TEXT_ALIGN] = style.textAlign
                        prefs[KEY_TABLE_DETAIL_GRID_ENABLED] = style.gridEnabled
                    }
                }
                imported = result
            }
            requireNotNull(imported)
        }

    private suspend fun currentCatalog(): Pair<List<SavedTableTemplate>, String?> {
        val prefs = context.dataStore.data.first()
        val stored = requireNotNull(prefs[KEY_TABLE_TEMPLATES_JSON]) {
            "저장된 템플릿을 먼저 확인해 주세요."
        }
        val items = requireNotNull(savedTableTemplatesFromJson(stored)) {
            "현재 템플릿 저장소를 읽을 수 없습니다."
        }
        require(items.isNotEmpty()) { "백업할 템플릿이 없습니다." }
        val activeId = prefs[KEY_ACTIVE_TABLE_TEMPLATE_ID]
            ?.takeIf { id -> items.any { it.id == id } }
            ?: items.maxByOrNull { it.modifiedAt }?.id
        return items to activeId
    }
}
