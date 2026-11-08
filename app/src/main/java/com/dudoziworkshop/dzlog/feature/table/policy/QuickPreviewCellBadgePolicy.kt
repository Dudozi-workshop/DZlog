package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.domain.model.TableCellDataType

/** Informational markers in the quick-edit table only; never saved as photo pixels. */
internal data class QuickPreviewCellBadgePlan(
    val sizePx: Float,
    val type: Boolean,
    val fileName: Boolean,
    val path: Boolean,
    val stackLinks: Boolean,
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
 * Always show type at top-left and applicable file/path links at top-right.
 * Only the two right-hand links may stack when the cell is narrow.
 * No lock badge: counter eligibility is a separate quick-edit policy.
 */
internal fun quickPreviewCellBadgePlan(
    widthPx: Float,
    idealSizePx: Float,
    minimumSizePx: Float,
    insetPx: Float,
    links: QuickCellLinks,
): QuickPreviewCellBadgePlan {
    val size = idealSizePx.coerceAtLeast(minimumSizePx).coerceAtLeast(1f)
    val inset = insetPx.coerceAtLeast(0f)
    val fullRowWidth = (1 + links.count) * size + (links.count * inset) + 2f * inset
    return QuickPreviewCellBadgePlan(
        sizePx = size,
        type = true,
        fileName = links.fileName,
        path = links.path,
        stackLinks = links.fileName && links.path && widthPx < fullRowWidth,
    )
}
