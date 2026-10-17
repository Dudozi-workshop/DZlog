package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.domain.model.PATH_SLOT_UI_MAX_COUNT
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.naming.NamingFormatDefaults
import com.dudoziworkshop.dzlog.domain.naming.resolvePathDraftToken
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import java.util.Date

/**
 * Compare actual save folders while excluding only the segments contributed
 * by an automatically rotating phrase. Other path changes still require consent.
 */
object CaptureUndoPathWarningPolicy {
    fun logicalFolder(path: String?): String? {
        val trimmed = path?.trim()?.replace('\\', '/')?.trimEnd('/')?.takeIf(String::isNotBlank)
            ?: return null
        val logical = trimmed.removeSuffix("/original").trimEnd('/')
        return logical.takeIf(String::isNotBlank)?.plus("/")
    }

    /**
     * Resolve the path-slot *positions*, but take literal path segments from the
     * saved path. Thus DATE/TIME keep their captured value even if midnight passes.
     */
    fun comparisonKey(
        path: String?,
        drafts: List<TableEditorSlotDraft?>,
        resolvedCells: List<ResolvedCell>,
        at: Date,
    ): String? {
        val normalized = logicalFolder(path) ?: return null
        val prefix = "Pictures/DZlog/"
        if (!normalized.startsWith(prefix)) return normalized
        val segments = normalized.removePrefix(prefix).trimEnd('/')
            .takeIf(String::isNotBlank)?.split('/') ?: emptyList()
        val nonempty = drafts.take(PATH_SLOT_UI_MAX_COUNT).mapIndexedNotNull { index, draft ->
            if (draft == null) return@mapIndexedNotNull null
            val value = resolvePathDraftToken(
                draft, resolvedCells, at,
                NamingFormatDefaults.DATE_FORMAT_DEFAULT,
                NamingFormatDefaults.TIME_FORMAT_CAPTURE_DEFAULT,
            )
            if (value.isBlank()) null else index to draft
        }
        // Never ignore an unknown or inconsistent folder shape.
        if (nonempty.size != segments.size) return normalized
        val byId = resolvedCells.associateBy { it.id }
        val keyed = segments.mapIndexed { segmentIndex, value ->
            val (slotIndex, draft) = nonempty[segmentIndex]
            val rotates = when (draft.kind.uppercase()) {
                "FORMAT" -> draft.formatType.equals("ROTATING_TEXT", ignoreCase = true)
                "CELL" -> byId[draft.cellId]?.type == TableCellDataType.ROTATING_TEXT
                else -> false
            }
            if (rotates) "{rotating:$slotIndex}" else value
        }
        return prefix + keyed.joinToString("/") + if (keyed.isEmpty()) "" else "/"
    }

    fun requiresConfirmation(
        capturedPath: String?,
        currentPath: String?,
        capturedKey: String? = null,
        currentKey: String? = null,
    ): Boolean {
        val captured = logicalFolder(capturedPath) ?: return false
        val current = logicalFolder(currentPath) ?: return false
        if (captured == current) return false
        return (capturedKey ?: captured) != (currentKey ?: current)
    }
}
