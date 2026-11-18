package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Content-sized preview viewport without consuming all remaining screen height.
 * The table renderer still handles fitting and scrolling within this viewport.
 */
internal fun editorPreviewCardHeight(
    availableWidth: Dp,
    availableHeight: Dp,
    tableWidthToHeight: Float,
    reservedEditorHeight: Dp,
    keyboardVisible: Boolean,
): Dp {
    val cardSidePadding = 32.dp + 24.dp
    val canvasGutters = 72.dp
    val headerAndVerticalPadding = 56.dp + 24.dp
    val usableWidth = (availableWidth - cardSidePadding - canvasGutters).coerceAtLeast(1.dp)
    val tableHeight = usableWidth / tableWidthToHeight.coerceAtLeast(0.05f)
    val contentHeight = tableHeight + canvasGutters + headerAndVerticalPadding
    val guideAndSpacing = 44.dp
    val maximumBySpace = (availableHeight - reservedEditorHeight - guideAndSpacing)
        .coerceAtLeast(96.dp)
    val maximumByWindow = availableHeight * (if (keyboardVisible) 0.36f else 0.55f)
    val maximum = minOf(320.dp, maximumBySpace, maximumByWindow.coerceAtLeast(96.dp))
    return contentHeight.coerceIn(minOf(128.dp, maximum), maximum)
}
