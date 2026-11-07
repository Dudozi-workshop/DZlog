package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType

/** Informational markers in the quick-edit table only; never part of saved photo pixels. */
internal data class QuickPreviewCellBadgePlan(
    val sizePx: Float,
    val type: Boolean,
    val fileName: Boolean,
    val path: Boolean,
    val locked: Boolean,
)

internal fun quickCellTypeLabel(type: TableCellDataType): String = when (type) {
    TableCellDataType.TEXT -> "텍스트"
    TableCellDataType.NUMBER -> "숫자"
    TableCellDataType.DATE -> "날짜"
    TableCellDataType.TIME -> "시간"
    TableCellDataType.COUNTER -> "자동번호"
    TableCellDataType.ROTATING_TEXT -> "순환문구"
}

/**
 * Prioritize the type marker and filename/path references over the old lock icon.
 * Do not hide all badges merely because large text leaves little blank space
 * above it. For extremely tiny cells, the zoom button reveals extra markers.
 */
internal fun quickPreviewCellBadgePlan(
    widthPx: Float,
    heightPx: Float,
    idealSizePx: Float,
    minimumSizePx: Float,
    insetPx: Float,
    links: QuickCellLinks,
    editable: Boolean,
): QuickPreviewCellBadgePlan {
    val inset = insetPx.coerceAtLeast(0f)
    val size = minOf(idealSizePx.coerceAtLeast(0f), (heightPx - 2f * inset).coerceAtLeast(0f))
    val capacity = if (size >= minimumSizePx && size > 0f) {
        ((widthPx - 2f * inset + inset).coerceAtLeast(0f) / (size + inset)).toInt()
    } else 0
    if (capacity < 1) return QuickPreviewCellBadgePlan(size, false, false, false, false)

    var remaining = capacity - 1 // Type is always the first visible marker.
    val fileName = links.fileName && remaining > 0
    if (fileName) remaining--
    val path = links.path && remaining > 0
    if (path) remaining--
    val locked = !editable && remaining > 0
    return QuickPreviewCellBadgePlan(size, true, fileName, path, locked)
}
