package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TableTemplateCatalogStyleTest {

    @Test
    fun `V2 style state survives catalog serialization`() {
        val style = TableStyleState(
            bgStyle = 2,
            gridEnabled = false,
            textColorMode = 1,
            manualTextColor = 0,
            valueScale = 135,
            textAlign = 2,
            bgAlpha = 144,
        )
        val item = createSavedTableTemplate(
            name = "V2",
            templateState = newBlankTableTemplateState(),
            styleState = style,
            modifiedAt = 1L,
            id = "template-v2",
        )

        val restored = savedTableTemplatesFromJson(
            savedTableTemplatesToJson(listOf(item))
        )!!.single()

        assertEquals(style, restored.styleState)
    }
}
