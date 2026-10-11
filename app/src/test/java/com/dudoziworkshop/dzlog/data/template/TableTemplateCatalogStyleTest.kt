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
    }    @Test
    fun `usage metadata survives serialization and old catalogs sort below used templates`() {
        val base = createSavedTableTemplate("B", newBlankTableTemplateState(), TableStyleState(), modifiedAt = 10L, id = "b")
        val used = base.copy(id = "a", name = "A", modifiedAt = 1L, lastUsedAt = 50L)
        val restored = savedTableTemplatesFromJson(savedTableTemplatesToJson(listOf(base, used)))!!
        assertEquals(50L, restored.last().lastUsedAt)
        assertEquals(listOf("a", "b"), sortTableTemplates(restored, TableTemplateSort.USED).map { it.id })
        assertEquals(listOf("b", "a"), sortTableTemplates(restored, TableTemplateSort.MODIFIED).map { it.id })
        assertEquals(listOf("a", "b"), sortTableTemplates(restored, TableTemplateSort.NAME).map { it.id })
        val legacy = org.json.JSONArray(savedTableTemplatesToJson(listOf(base)))
        legacy.getJSONObject(0).remove("lastUsedAt")
        assertEquals(0L, savedTableTemplatesFromJson(legacy.toString())!!.single().lastUsedAt)
    }
    @Test
    fun `capture usage is retained when a stale editor snapshot is persisted`() = kotlinx.coroutines.runBlocking {
        val context: android.content.Context = org.robolectric.RuntimeEnvironment.getApplication()
        val base = createSavedTableTemplate("Capture", newBlankTableTemplateState(), TableStyleState(), modifiedAt = 10L, id = "capture-used")
        com.dudoziworkshop.dzlog.feature.table.policy.persistTableTemplateCatalog(context, listOf(base), base.id)
        com.dudoziworkshop.dzlog.feature.table.policy.markSavedTableTemplateUsed(context, base.id, 50L)
        com.dudoziworkshop.dzlog.feature.table.policy.persistTableTemplateCatalog(context, listOf(base.copy(name = "Edited")), base.id)
        val restored = com.dudoziworkshop.dzlog.feature.table.policy.loadOrMigrateTableTemplateCatalog(context).items.single()
        assertEquals(50L, restored.lastUsedAt)
        assertEquals(10L, restored.modifiedAt)
        assertEquals("Edited", restored.name)
        com.dudoziworkshop.dzlog.feature.table.policy.markSavedTableTemplateUsed(context, "missing", 100L)
        assertEquals(restored, com.dudoziworkshop.dzlog.feature.table.policy.loadOrMigrateTableTemplateCatalog(context).items.single())
    }

}
