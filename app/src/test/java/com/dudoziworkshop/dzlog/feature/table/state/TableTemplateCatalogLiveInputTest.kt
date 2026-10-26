package com.dudoziworkshop.dzlog.feature.table.state

import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.feature.table.policy.updateQuickCellValues
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.junit.Assert.assertEquals
import org.junit.Test

class TableTemplateCatalogLiveInputTest {
    @Test fun pendingSaveDoesNotRollBackNewerLiveInput() {
        val initial = newBlankTableTemplateState(rows = 1, cols = 1)
        val item = createSavedTableTemplate(styleState = TableStyleState(), name = "A", templateState = initial, id = "a")
        val model = TableTemplateCatalogViewModel()
        model.restoreCatalog(listOf(item), "a")
        val saved = updateQuickCellValues(initial, mapOf(initial.cells.single().cellId to "first"))
        val live = updateQuickCellValues(saved, mapOf(initial.cells.single().cellId to "first\nsecond"))
        val startRevision = model.stateRevision
        model.updateStateOnly(live)
        assertEquals(live, model.tableTemplateState)
        assertEquals(live, model.templates.single().templateState)
        model.applyPersistedCatalog(listOf(item.copy(templateState = saved)), "a", saved, startRevision)
        assertEquals(live, model.tableTemplateState)
        assertEquals(live, model.templates.single().templateState)
        model.applyPersistedCatalog(listOf(item.copy(templateState = live)), "a", live, model.stateRevision)
        assertEquals(live, model.templates.single().templateState)
    }

    @Test fun pendingSaveDoesNotSwitchOrOverwriteAnotherActiveTemplate() {
        val state = newBlankTableTemplateState(rows = 1, cols = 1)
        val a = createSavedTableTemplate(styleState = TableStyleState(), name = "A", templateState = state, id = "a")
        val b = createSavedTableTemplate(styleState = TableStyleState(), name = "B", templateState = newBlankTableTemplateState(rows = 2, cols = 2), id = "b")
        val model = TableTemplateCatalogViewModel()
        model.restoreCatalog(listOf(a, b), "a")
        model.activate("b")
        model.applyPersistedCatalog(listOf(a), "a", state, model.stateRevision)
        assertEquals("b", model.activeTemplateId)
        assertEquals(b.templateState, model.tableTemplateState)
        assertEquals(2, model.templates.size)
    }
    @Test fun completedCameraCaptureUpdatesEditorPhrasePosition() {
        val initial = newBlankTableTemplateState(1, 1)
        val item = createSavedTableTemplate(name = "A", templateState = initial, styleState = TableStyleState(), id = "a")
        val model = TableTemplateCatalogViewModel()
        model.restoreCatalog(listOf(item), "a")
        val revision = model.stateRevision
        val progressed = initial.copy(cells = initial.cells.map { it.copy(phraseIndex = 1) })
        model.applyPersistedCatalog(listOf(item.copy(templateState = progressed)), "a", progressed, revision)
        assertEquals(progressed, model.tableTemplateState)
        assertEquals(progressed, model.templates.single().templateState)
    }

}
