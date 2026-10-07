package com.dudoziworkshop.dzlog.feature.table.policy

import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.newBlankTableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.junit.Assert.assertEquals
import org.junit.Test

class TableTemplateCatalogMutationsTest {

    @Test
    fun template_only_update_preserves_catalog_style_ssot() {
        val style = TableStyleState(bgStyle = 2, valueScale = 135, gridEnabled = false)
        val original = createSavedTableTemplate(
            name = "A",
            templateState = newBlankTableTemplateState(rows = 1, cols = 1),
            styleState = style,
            modifiedAt = 1L,
            id = "a",
        )
        val changedTemplate = newBlankTableTemplateState(rows = 2, cols = 2)

        val next = replaceActiveSavedTableTemplate(
            items = listOf(original),
            activeTemplateId = "a",
            templateState = changedTemplate,
            modifiedAt = 2L,
        )

        assertEquals(changedTemplate, next.single().templateState)
        assertEquals(style, next.single().styleState)
    }

    @Test
    fun explicit_style_update_replaces_catalog_style() {
        val original = createSavedTableTemplate(
            name = "A",
            templateState = newBlankTableTemplateState(rows = 1, cols = 1),
            styleState = TableStyleState(),
            modifiedAt = 1L,
            id = "a",
        )
        val nextStyle = TableStyleState(bgStyle = 1, valueScale = 120)

        val next = replaceActiveSavedTableTemplate(
            items = listOf(original),
            activeTemplateId = "a",
            templateState = original.templateState,
            styleState = nextStyle,
            modifiedAt = 2L,
        )

        assertEquals(nextStyle, activeSavedTableStyle(next, "a"))
    }
}
