package com.dudoziworkshop.dzlog.ui.table.mock

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TableEditorPreviewHeightPolicyTest {
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
