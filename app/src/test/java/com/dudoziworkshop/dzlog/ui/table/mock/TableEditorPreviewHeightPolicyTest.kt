package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorPreviewHeightPolicyTest {
    @Test fun wideOneRowTableKeepsReadablePreviewWithoutKeyboard() {
        val h = editorPreviewCardHeight(
            availableWidth = 390.dp, availableHeight = 680.dp,
            tableWidthToHeight = 8f, reservedEditorHeight = 168.dp,
            keyboardVisible = false,
        )
        // The old natural-height-only rule could shrink a one-row table
        // preview to approximately 200dp; keep it proportional to the window.
        assertTrue(h >= 240.dp)
        assertTrue(h <= 320.dp)
    }

    @Test fun tallPortraitRetainsMoreRoomForWideTable() {
        val h = editorPreviewCardHeight(
            availableWidth = 430.dp, availableHeight = 900.dp,
            tableWidthToHeight = 12f, reservedEditorHeight = 188.dp,
            keyboardVisible = false,
        )
        assertTrue(h >= 260.dp)
        assertTrue(h <= 320.dp)
    }

    @Test fun inputKeyboardKeepsEditorRoomAndBoundsPreview() {
        val height = 440.dp
        val editor = 168.dp
        val h = editorPreviewCardHeight(
            availableWidth = 390.dp, availableHeight = height,
            tableWidthToHeight = 10f, reservedEditorHeight = editor,
            keyboardVisible = true,
        )
        assertTrue(h <= height * 0.36f)
        assertTrue(h + editor + 12.dp <= height)
    }

    @Test fun shortLandscapeDoesNotForcePreferredMinimumOverEditor() {
        val height = 300.dp
        val editor = 124.dp
        val h = editorPreviewCardHeight(
            availableWidth = 760.dp, availableHeight = height,
            tableWidthToHeight = 20f, reservedEditorHeight = editor,
            keyboardVisible = false,
        )
        assertTrue(h <= 132.dp)
        assertTrue(h + editor + 44.dp <= height)
    }

    @Test fun splitScreenClampsWithoutOverlappingEditor() {
        val height = 260.dp
        val editor = 140.dp
        val h = editorPreviewCardHeight(
            availableWidth = 300.dp, availableHeight = height,
            tableWidthToHeight = 9f, reservedEditorHeight = editor,
            keyboardVisible = true,
        )
        assertTrue(h <= 108.dp)
        assertTrue(h + editor + 12.dp <= height)
    }

    @Test fun shortTableDoesNotFillAvailableHeight() {
        val h = editorPreviewCardHeight(
            availableWidth = 390.dp, availableHeight = 680.dp,
            tableWidthToHeight = 4.5f, reservedEditorHeight = 168.dp,
            keyboardVisible = false,
        )
        assertTrue(h < 320.dp)
        assertTrue(h < 680.dp * 0.55f)
    }

    @Test fun tallTableIsCappedAtMaximum() {
        val h = editorPreviewCardHeight(
            availableWidth = 390.dp, availableHeight = 900.dp,
            tableWidthToHeight = 0.4f, reservedEditorHeight = 168.dp,
            keyboardVisible = false,
        )
        assertEquals(320.dp, h)
    }

    @Test fun landscapeAndKeyboardRestrictPreview() {
        val landscape = editorPreviewCardHeight(
            availableWidth = 760.dp, availableHeight = 300.dp,
            tableWidthToHeight = 3f, reservedEditorHeight = 124.dp,
            keyboardVisible = false,
        )
        val ime = editorPreviewCardHeight(
            availableWidth = 390.dp, availableHeight = 440.dp,
            tableWidthToHeight = 2f, reservedEditorHeight = 168.dp,
            keyboardVisible = true,
        )
        assertTrue(landscape <= 165.dp)
        assertTrue(ime <= 440.dp * 0.36f)
    }

    @Test fun widerAndTallerViewsDoNotExceedCap() {
        val h = editorPreviewCardHeight(
            availableWidth = 1200.dp, availableHeight = 1500.dp,
            tableWidthToHeight = 1f, reservedEditorHeight = 210.dp,
            keyboardVisible = false,
        )
        assertEquals(320.dp, h)
    }
}
