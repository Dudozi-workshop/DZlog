package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType

/** Informational markers in the quick-edit table only; never part of saved photo pixels. */
internal data class QuickPreviewCellBadgePlan(
    val sizePx: Float,
    val type: Boolean,
    val fileName: Boolean,
    val path: Boolean,
    val locked: Boolean,
    val compact: Boolean,
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
 * Never omit cell metadata because a cell is narrow or short.
 * When all badges do not fit, the UI uses one compact, unbounded strip.
 * The user can zoom the table for detailed reading without losing indicators.
 */
internal fun quickPreviewCellBadgePlan(
    widthPx: Float,
    idealSizePx: Float,
    minimumSizePx: Float,
    insetPx: Float,
    links: QuickCellLinks,
    editable: Boolean,
): QuickPreviewCellBadgePlan {
    val size = idealSizePx.coerceAtLeast(minimumSizePx).coerceAtLeast(1f)
    val inset = insetPx.coerceAtLeast(0f)
    val count = 1 + links.count + if (editable) 0 else 1
    val required = count * size + (count - 1) * inset + 2f * inset
    return QuickPreviewCellBadgePlan(
        sizePx = size,
        type = true,
        fileName = links.fileName,
        path = links.path,
        locked = !editable,
        compact = widthPx < required,
    )
}
