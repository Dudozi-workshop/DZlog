package com.dudoziworkshop.dzlog.feature.table.preview

import com.dudoziworkshop.dzlog.domain.watermark.WatermarkBuilder.WatermarkCell

/**
 * Editing-only placeholders must not change the table's content-driven geometry.
 * The editor draws placeholders separately, while camera/photo layout receives
 * the actual empty values.
 */
internal fun cellsForPreviewLayout(
    displayCells: List<WatermarkCell>,
    placeholderCellIndexes: Set<Int>,
): List<WatermarkCell> {
    if (placeholderCellIndexes.isEmpty()) return displayCells
    return displayCells.mapIndexed { index, cell ->
        if (index in placeholderCellIndexes) cell.copy(valueText = "") else cell
    }
}
