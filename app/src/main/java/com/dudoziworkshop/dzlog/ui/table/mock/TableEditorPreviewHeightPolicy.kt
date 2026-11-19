package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * B layout: always reserve a readable table-preview viewport on normal
 * portrait screens, even for a one-row / very wide table.
 *
 * In a constrained window (landscape, split screen or IME), the editor panel
 * and its fixed controls take priority. The preview uses the available room
 * instead of forcing a large minimum that could overlap those controls.
 *
 * The canvas continues to preserve the actual table aspect ratio and manage
 * its own scrolling/fitting; only the enclosing card's height is computed here.
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
    val naturalTableHeight = usableWidth / tableWidthToHeight.coerceAtLeast(0.05f)
    val naturalCardHeight = naturalTableHeight + canvasGutters + headerAndVerticalPadding

    // A larger minimum is affordable only while the keyboard is closed.
    // Tall screens get a little more room, without reserving empty space
    // proportional to the full tablet height.
    val minimumByWindow = if (keyboardVisible) {
        (availableHeight * 0.32f).coerceIn(120.dp, 150.dp)
    } else {
        (availableHeight * 0.36f).coerceIn(220.dp, 260.dp)
    }

    // The help caption is hidden while the keyboard is open.
    val guideAndSpacing = if (keyboardVisible) 12.dp else 44.dp
    // When the keyboard leaves less room than even a minimal preview,
    // collapse the viewport rather than overlapping the input controls.
    val maximumBySpace =
        (availableHeight - reservedEditorHeight - guideAndSpacing).coerceAtLeast(0.dp)
    val maximumByWindow =
        (availableHeight * if (keyboardVisible) 0.36f else 0.55f).coerceAtLeast(0.dp)
    val maximum = minOf(320.dp, maximumBySpace, maximumByWindow)

    // Short landscape viewports may be smaller than the preferred minimum:
    // avoid clipping the editor panel by adapting the preview instead.
    val minimum = minOf(minimumByWindow, maximum)
    return naturalCardHeight.coerceIn(minimum, maximum)
}
