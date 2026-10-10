package com.dudoziworkshop.dzlog.data.backup

import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesFromJson
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesToJson
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import java.security.MessageDigest
import org.json.JSONArray
import org.json.JSONObject

/**
 * Portable local template archive. No photos, gallery locations, account data or
 * running capture counters are exported. Writes are handled separately by SAF.
 */
data class LocalTemplateBackup(
    val templates: List<SavedTableTemplate>,
    val activeTemplateId: String?,
    val createdAtMillis: Long,
)

object LocalTemplateBackupCodec {
    const val FILE_EXTENSION = "dzlog"
    const val MAX_ARCHIVE_BYTES = 2 * 1024 * 1024
    private const val FORMAT = "dzlog-template-backup"
    private const val SCHEMA_VERSION = 1

    fun encode(
        items: List<SavedTableTemplate>,
        activeTemplateId: String?,
        createdAtMillis: Long = System.currentTimeMillis(),
    ): String {
        require(items.isNotEmpty()) { "백업할 템플릿이 없습니다." }
        require(createdAtMillis >= 0L) { "잘못된 백업 시각입니다." }

        // COUNTER seed is advanced during capture; export template configuration,
        // not a previous device's capture progress.
        val portableItems = items.map { item ->
            item.copy(templateState = item.templateState.copy(
                cells = item.templateState.cells.map { cell ->
                    if (cell.dataType == TableCellDataType.COUNTER) {
                        cell.copy(typedValue = CellValue.CounterSeed(1))
                    } else cell
                }
            ))
        }
        val payload = savedTableTemplatesToJson(portableItems)
        LocalTemplateBackupValidator.validate(JSONArray(payload))
        require(activeTemplateId == null || portableItems.any { it.id == activeTemplateId }) {
            "현재 템플릿 참조가 올바르지 않습니다."
        }
        val archive = JSONObject()
            .put("format", FORMAT)
            .put("schemaVersion", SCHEMA_VERSION)
            .put("createdAtMillis", createdAtMillis)
            .put("activeTemplateId", activeTemplateId ?: JSONObject.NULL)
            .put("payload", payload)
            .put("sha256", sha256(payload))
            .toString()
        require(archive.toByteArray(Charsets.UTF_8).size <= MAX_ARCHIVE_BYTES) {
            "백업 파일이 허용 크기를 초과했습니다."
        }
        return archive
    }

    fun decode(json: String): LocalTemplateBackup = try {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_ARCHIVE_BYTES) {
            "백업 파일이 허용 크기를 초과했습니다."
        }
        val root = JSONObject(json)
        require(root.optString("format") == FORMAT) { "DZlog 백업 파일이 아닙니다." }
        require(root.opt("schemaVersion") is Int) { "백업 파일 버전이 올바르지 않습니다." }
        val version = root.getInt("schemaVersion")
        require(version == SCHEMA_VERSION) {
            if (version > SCHEMA_VERSION) "더 최신 버전의 앱에서 만든 백업입니다."
            else "지원하지 않는 백업 버전입니다."
        }
        val createdAt = root.opt("createdAtMillis")
        require(createdAt is Long || createdAt is Int) { "백업 시각 정보가 올바르지 않습니다." }
        val createdAtMillis = (createdAt as Number).toLong()
        require(createdAtMillis >= 0L) { "백업 시각 정보가 올바르지 않습니다." }

        val payload = root.opt("payload")
        require(payload is String && payload.isNotBlank()) { "백업 데이터가 없습니다." }
        val digest = root.optString("sha256")
        require(digest.length == 64 && digest == sha256(payload)) {
            "백업 파일이 손상되었습니다."
        }
        val data = JSONArray(payload)
        LocalTemplateBackupValidator.validate(data)
        val items = requireNotNull(savedTableTemplatesFromJson(payload)) {
            "템플릿을 읽을 수 없습니다."
        }
        require(items.size == data.length()) { "일부 템플릿을 읽을 수 없습니다." }

        val activeValue = root.opt("activeTemplateId")
        val activeId = when (activeValue) {
            null, JSONObject.NULL -> null
            is String -> activeValue.takeIf { it.isNotBlank() }
            else -> error("현재 템플릿 정보가 올바르지 않습니다.")
        }
        require(activeId == null || items.any { it.id == activeId }) {
            "현재 템플릿 참조가 올바르지 않습니다."
        }
        LocalTemplateBackup(items, activeId, createdAtMillis)
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (e: Exception) {
        throw IllegalArgumentException("백업 파일을 읽을 수 없습니다.", e)
    }

    private fun sha256(payload: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }
}
