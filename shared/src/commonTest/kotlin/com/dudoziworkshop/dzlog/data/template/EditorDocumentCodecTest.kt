package com.dudoziworkshop.dzlog.data.template

import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.*

class EditorDocumentCodecTest {
    private fun sample() = EditorDocument(
        SavedTableTemplate("existing-id", "보존할 표",
            TableTemplateState(1, 1, listOf(TableCellState(cellId = "existing-cell", rowIndex = 0, colIndex = 0,
                dataType = TableCellDataType.COUNTER, rawText = "27", typedValue = CellValue.CounterSeed(27)))),
            TableStyleState(bgStyle = 2, textAlign = 2), modifiedAt = 123L),
        includePathInScope = false, includeFilenameInScope = true, saveMode = SaveMode.BOTH,
        counterPadding = 4, nextCounter = 28, usesAutoNext = false,
    )

    @Test fun preservesLocalCounterProgressIdsStyleAndScopeWithoutBackupReset() {
        val value = sample()
        val restored = EditorDocumentCodec.decode(EditorDocumentCodec.encode(value))
        assertEquals(value, restored)
        assertEquals(CellValue.CounterSeed(27), restored.template.templateState.cells.single().typedValue)
    }

    @Test fun rejectsCorruptAndUnsupportedDocumentsInsteadOfSilentlyResetting() {
        assertFails { EditorDocumentCodec.decode("broken") }
        assertFails { EditorDocumentCodec.decode(EditorDocumentCodec.encode(sample()).replace("\"schema\":1", "\"schema\":2")) }
    }
}
