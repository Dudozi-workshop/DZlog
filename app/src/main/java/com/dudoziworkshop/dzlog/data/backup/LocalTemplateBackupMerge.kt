package com.dudoziworkshop.dzlog.data.backup

import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import java.util.UUID

data class LocalTemplateMergePreview(
    val incomingCount: Int,
    val renamedCount: Int,
    val existingCount: Int,
)

data class LocalTemplateMergeResult(
    val templates: List<SavedTableTemplate>,
    val activeTemplateId: String?,
    val preview: LocalTemplateMergePreview,
)

/**
 * Pure preview + merge. P4 will perform user confirmation then persist in one transaction.
 * Existing IDs, active selection and templates are never replaced.
 */
object LocalTemplateBackupMerge {
    fun preview(existing: List<SavedTableTemplate>, archive: LocalTemplateBackup): LocalTemplateMergePreview {
        val used = existing.map { it.name }.toMutableSet()
        var renamed = 0
        archive.templates.forEach { item ->
            val finalName = uniqueName(item.name, used)
            if (finalName != item.name) renamed++
            used.add(finalName)
        }
        return LocalTemplateMergePreview(archive.templates.size, renamed, existing.size)
    }

    fun merge(existing: List<SavedTableTemplate>, activeId: String?, archive: LocalTemplateBackup): LocalTemplateMergeResult {
        require(activeId == null || existing.any { it.id == activeId }) {
            "기존 활성 템플릿 정보가 잘못되었습니다."
        }
        val usedNames = existing.map { it.name }.toMutableSet()
        val usedIds = existing.map { it.id }.toMutableSet()
        val newIds = mutableMapOf<String, String>()
        var renamed = 0
        val imported = archive.templates.map { item ->
            require(item.id !in newIds) { "백업 템플릿 ID가 중복되었습니다." }
            val newId = generateSequence { UUID.randomUUID().toString() }.first { usedIds.add(it) }
            newIds[item.id] = newId
            val name = uniqueName(item.name, usedNames)
            if (name != item.name) renamed++
            usedNames.add(name)
            item.copy(id = newId, name = name)
        }
        val selectedId = activeId ?: if (existing.isEmpty()) {
            archive.activeTemplateId?.let(newIds::get) ?: imported.firstOrNull()?.id
        } else null
        return LocalTemplateMergeResult(
            templates = existing + imported,
            activeTemplateId = selectedId,
            preview = LocalTemplateMergePreview(imported.size, renamed, existing.size),
        )
    }

    private fun uniqueName(name: String, used: Set<String>): String {
        if (name !in used) return name
        val suffix = "${name} (가져옴)"
        if (suffix !in used) return suffix
        var index = 2
        while ("${name} (가져옴 ${index})" in used) index++
        return "${name} (가져옴 ${index})"
    }
}
