package com.dudoziworkshop.dzlog.ui.table.mock

import org.junit.Assert.assertEquals
import org.junit.Test

class TableEditorNavigationPolicyTest {
    private fun action(
        tab: MockBottomTab = MockBottomTab.CONTENT,
        saving: Boolean = false,
        detail: Boolean = false,
        advanced: Boolean = false,
        selected: Boolean = false,
        dirty: Boolean = false,
    ) = resolveTableEditorBackAction(saving, detail, tab, advanced, selected, dirty)

    @Test fun savingPreventsExitEvenWithAnOpenDetail() {
        assertEquals(TableEditorBackAction.IGNORE, action(saving = true, detail = true, dirty = true))
    }

    @Test fun detailClosesBeforeSaveInspectorAndExitConfirmation() {
        assertEquals(TableEditorBackAction.CLOSE_DETAIL, action(tab = MockBottomTab.SAVE, detail = true, dirty = true))
    }

    @Test fun advancedStyleClosesBeforeStylePanel() {
        assertEquals(TableEditorBackAction.CLOSE_ADVANCED_STYLE, action(tab = MockBottomTab.STYLE, advanced = true, dirty = true))
        assertEquals(TableEditorBackAction.CLOSE_PANEL, action(tab = MockBottomTab.STYLE, dirty = true))
    }

    @Test fun eachInspectorClosesBeforeDirtyDraftExit() {
        listOf(MockBottomTab.STRUCTURE, MockBottomTab.STYLE, MockBottomTab.SAVE).forEach { tab ->
            assertEquals(TableEditorBackAction.CLOSE_PANEL, action(tab = tab, selected = true, dirty = true))
        }
    }

    @Test fun selectedCellClosesBeforeDirtyDraftExit() {
        assertEquals(TableEditorBackAction.CLOSE_CELL, action(selected = true, dirty = true))
        assertEquals(TableEditorBackAction.CONFIRM_EXIT, action(dirty = true))
    }

    @Test fun cleanEditorExitsOnlyWhenNoLayerRemains() {
        assertEquals(TableEditorBackAction.CLOSE_CELL, action(selected = true))
        assertEquals(TableEditorBackAction.EXIT, action())
    }

    @Test fun staleAdvancedStyleDoesNotInterceptAnotherTab() {
        assertEquals(TableEditorBackAction.CLOSE_PANEL, action(tab = MockBottomTab.SAVE, advanced = true))
        assertEquals(TableEditorBackAction.EXIT, action(advanced = true))
    }
}
