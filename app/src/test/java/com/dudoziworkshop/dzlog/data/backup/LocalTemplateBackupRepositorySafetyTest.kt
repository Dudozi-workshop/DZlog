package com.dudoziworkshop.dzlog.data.backup

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.dudoziworkshop.dzlog.data.preferences.KEY_ACTIVE_TABLE_TEMPLATE_ID
import com.dudoziworkshop.dzlog.data.preferences.KEY_COUNTER_DIGITS
import com.dudoziworkshop.dzlog.data.preferences.KEY_SAVE_MODE
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATE_JSON
import com.dudoziworkshop.dzlog.data.preferences.KEY_TABLE_TEMPLATES_JSON
import com.dudoziworkshop.dzlog.data.preferences.dataStore
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.data.template.createSavedTableTemplate
import com.dudoziworkshop.dzlog.data.template.savedTableTemplatesToJson
import com.dudoziworkshop.dzlog.data.template.toJsonString
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.feature.table.policy.decodeCatalogWithoutDataLoss
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalTemplateBackupRepositorySafetyTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()
    private val state get() = requireNotNull(OnboardingStarterCatalog.buildState("basic"))

    private suspend fun clearKeys() {
        context.dataStore.edit {
            it.remove(KEY_TABLE_TEMPLATES_JSON)
            it.remove(KEY_TABLE_TEMPLATE_JSON)
            it.remove(KEY_ACTIVE_TABLE_TEMPLATE_ID)
            it.remove(KEY_COUNTER_DIGITS)
            it.remove(KEY_SAVE_MODE)
        }
    }

    @Test fun realDataStoreExportAndImportNeverReplaceOriginalActiveOrSettings() = runBlocking {
        clearKeys()
        val original = createSavedTableTemplate(
            name = "기존 기록", templateState = state, styleState = TableStyleState(),
            modifiedAt = 100L, id = "existing-template"
        )
        val originalJson = savedTableTemplatesToJson(listOf(original))
        val legacyJson = original.templateState.toJsonString()
        try {
            context.dataStore.edit {
                it[KEY_TABLE_TEMPLATES_JSON] = originalJson
                it[KEY_ACTIVE_TABLE_TEMPLATE_ID] = original.id
                it[KEY_TABLE_TEMPLATE_JSON] = legacyJson
                it[KEY_COUNTER_DIGITS] = 5
                it[KEY_SAVE_MODE] = 2
            }

            val repo = LocalTemplateBackupRepository(context)
            val archive = LocalTemplateBackupCodec.decode(repo.exportArchive())
            assertEquals(original.id, archive.activeTemplateId)
            assertEquals(1, archive.templates.size)
            assertEquals(1, repo.preview(archive).renamedCount)

            val merged = repo.importConfirmed(archive)
            assertEquals(2, merged.templates.size)
            assertEquals(original.id, merged.activeTemplateId)
            assertEquals(original.id, merged.templates.first().id)
            assertEquals(original.name, merged.templates.first().name)
            assertEquals("기존 기록 (가져옴)", merged.templates.last().name)
            assertNotEquals(original.id, merged.templates.last().id)

            val persisted = context.dataStore.data.first()
            assertEquals(original.id, persisted[KEY_ACTIVE_TABLE_TEMPLATE_ID])
            assertEquals(legacyJson, persisted[KEY_TABLE_TEMPLATE_JSON])
            assertEquals(5, persisted[KEY_COUNTER_DIGITS])
            assertEquals(2, persisted[KEY_SAVE_MODE])
            val restored = requireNotNull(decodeCatalogWithoutDataLoss(
                requireNotNull(persisted[KEY_TABLE_TEMPLATES_JSON])
            ))
            assertEquals(listOf(original.id, merged.templates.last().id), restored.map { it.id })
            assertEquals(original.name, restored.first().name)

            val exportedAgain = LocalTemplateBackupCodec.decode(
                LocalTemplateBackupRepository(context).exportArchive()
            )
            assertEquals(2, exportedAgain.templates.size)
            assertEquals(original.id, exportedAgain.activeTemplateId)
        } finally {
            clearKeys()
        }
    }

    @Test fun corruptedCurrentCatalogBlocksBackupPreviewAndImportWithoutChangingData() = runBlocking {
        clearKeys()
        val source = createSavedTableTemplate(
            name = "백업 데이터", templateState = state, styleState = TableStyleState(),
            id = "backup-template"
        )
        val archive = LocalTemplateBackupCodec.decode(
            LocalTemplateBackupCodec.encode(listOf(source), source.id, 100L)
        )
        val corrupted = "[{}, {\"bad\":\"entry\"}]"
        try {
            context.dataStore.edit {
                it[KEY_TABLE_TEMPLATES_JSON] = corrupted
                it[KEY_ACTIVE_TABLE_TEMPLATE_ID] = "original-active"
                it[KEY_COUNTER_DIGITS] = 4
            }
            val repo = LocalTemplateBackupRepository(context)
            assertNotNull(runCatching { repo.preview(archive) }.exceptionOrNull())
            assertNotNull(runCatching { repo.importConfirmed(archive) }.exceptionOrNull())
            assertNotNull(runCatching { repo.exportArchive() }.exceptionOrNull())
            val after = context.dataStore.data.first()
            assertEquals(corrupted, after[KEY_TABLE_TEMPLATES_JSON])
            assertEquals("original-active", after[KEY_ACTIVE_TABLE_TEMPLATE_ID])
            assertEquals(4, after[KEY_COUNTER_DIGITS])
        } finally {
            clearKeys()
        }
    }

    @Test fun portableBackupResetsCounterSeedWithoutMutatingCurrentTemplate() {
        val original = state.copy(cells = state.cells.map { cell ->
            if (cell.dataType == TableCellDataType.COUNTER) {
                cell.copy(typedValue = CellValue.CounterSeed(47))
            } else cell
        })
        val saved = createSavedTableTemplate(
            name = "번호 기록", templateState = original, styleState = TableStyleState(),
            id = "counter-template"
        )
        val decoded = LocalTemplateBackupCodec.decode(
            LocalTemplateBackupCodec.encode(listOf(saved), saved.id, 100L)
        )
        val backupCounter = decoded.templates.single().templateState.cells
            .first { it.dataType == TableCellDataType.COUNTER }
        val currentCounter = saved.templateState.cells
            .first { it.dataType == TableCellDataType.COUNTER }
        assertEquals(CellValue.CounterSeed(1), backupCounter.typedValue)
        assertEquals(CellValue.CounterSeed(47), currentCounter.typedValue)
        assertTrue(decoded.activeTemplateId == saved.id)
    }
}
