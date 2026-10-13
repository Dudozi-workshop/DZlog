package com.dudoziworkshop.dzlog.feature.table.policy

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATES_JSON
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesToJson
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TableTemplateCatalogUsageSafetyTest {
    private val state = requireNotNull(OnboardingStarterCatalog.buildState("basic"))
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun malformedEntryCannotBeDroppedByUsageMetadataUpdate() = runBlocking {
        val original = savedTableTemplatesToJson(
            listOf(createSavedTableTemplate(
                name = "Existing", templateState = state,
                styleState = TableStyleState(), id = "existing-1"
            ))
        )
        val partiallyCorrupt = original.dropLast(1) + ",{}]"
        context.dataStore.edit { it[KEY_TABLE_TEMPLATES_JSON] = partiallyCorrupt }
        try {
            assertTrue(decodeCatalogWithoutDataLoss(partiallyCorrupt) == null)
            val failed = runCatching {
                markSavedTableTemplateUsed(context, "existing-1", 100L)
            }.exceptionOrNull()
            assertTrue(failed is IllegalStateException)
            assertEquals(
                partiallyCorrupt,
                context.dataStore.data.first()[KEY_TABLE_TEMPLATES_JSON]
            )
        } finally {
            context.dataStore.edit { it.remove(KEY_TABLE_TEMPLATES_JSON) }
        }
    }

    @Test
    fun validUsageUpdateKeepsAllTemplatesAndMonotonicLastUsedAt() = runBlocking {
        val first = createSavedTableTemplate(
            name = "First", templateState = state,
            styleState = TableStyleState(), id = "id-1"
        )
        val second = createSavedTableTemplate(
            name = "Second", templateState = state,
            styleState = TableStyleState(), id = "id-2"
        )
        context.dataStore.edit {
            it[KEY_TABLE_TEMPLATES_JSON] = savedTableTemplatesToJson(listOf(first, second))
        }
        try {
            markSavedTableTemplateUsed(context, "id-1", 100L)
            markSavedTableTemplateUsed(context, "id-1", 20L)
            val raw = context.dataStore.data.first()[KEY_TABLE_TEMPLATES_JSON]
            val saved = decodeCatalogWithoutDataLoss(requireNotNull(raw))
            assertNotNull(saved)
            assertEquals(2, saved!!.size)
            val firstSaved = saved.first { it.id == "id-1" }
            val secondSaved = saved.first { it.id == "id-2" }
            assertEquals(100L, firstSaved.lastUsedAt)
            assertEquals(0L, secondSaved.lastUsedAt)
            assertEquals(first.name, firstSaved.name)
            assertEquals(second.name, secondSaved.name)
            assertEquals(first.styleState, firstSaved.styleState)
            assertEquals(second.styleState, secondSaved.styleState)
            assertEquals(first.templateState.rows, firstSaved.templateState.rows)
            assertEquals(second.templateState.cols, secondSaved.templateState.cols)
            assertEquals(first.templateState.cells.size, firstSaved.templateState.cells.size)
            assertEquals(second.templateState.cells.map { it.rawText }, secondSaved.templateState.cells.map { it.rawText })
        } finally {
            context.dataStore.edit { it.remove(KEY_TABLE_TEMPLATES_JSON) }
        }
    }
}
