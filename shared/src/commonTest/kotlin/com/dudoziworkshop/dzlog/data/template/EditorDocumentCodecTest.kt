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

    @Test fun persistsConfiguredPlacementAlongsideAllExistingDocumentFields() {
        val placement = EditorCapturePlacement(
            anchor = EditorCaptureAnchor.CUSTOM, offsetXPercent = 100,
            offsetYPercent = 0, widthRatio = 40, heightRatio = 100, rotationCwDeg = 359,
        )
        val document = sample().copy(capturePlacement = placement)
        assertEquals(document, EditorDocumentCodec.decode(EditorDocumentCodec.encode(document)))
    }

    @Test fun legacyDocumentsWithoutPlacementRemainReadable() {
        val document = sample()
        val encoded = EditorDocumentCodec.encode(document)
        // JSON object key order is not guaranteed on JVM or Native.
        val legacy = encoded.replace(Regex("""(,"placementV1":null|"placementV1":null,)"""), "")
        assertNotEquals(encoded, legacy)
        assertEquals(document, EditorDocumentCodec.decode(legacy))
        assertNull(EditorDocumentCodec.decode(legacy).capturePlacement)
    }

    @Test fun placementAcceptsAnchorAndBoundaryCombinations() {
        EditorCaptureAnchor.values().forEach { anchor ->
            listOf(0, 100).forEach { offset ->
                listOf(10, 100).forEach { size ->
                    listOf(0, 359).forEach { rotation ->
                        val placement = EditorCapturePlacement(
                            anchor, offset, offset, size, size, rotation,
                        )
                        assertEquals(anchor, placement.anchor)
                        assertEquals(rotation, placement.rotationCwDeg)
                    }
                }
            }
        }
    }

    @Test fun placementRejectsInvalidBounds() {
        val anchor = EditorCaptureAnchor.CUSTOM
        assertFailsWith<IllegalArgumentException> { EditorCapturePlacement(anchor, -1, 0, 40, 40) }
        assertFailsWith<IllegalArgumentException> { EditorCapturePlacement(anchor, 0, 101, 40, 40) }
        assertFailsWith<IllegalArgumentException> { EditorCapturePlacement(anchor, 0, 0, 9, 40) }
        assertFailsWith<IllegalArgumentException> { EditorCapturePlacement(anchor, 0, 0, 40, 101) }
        assertFailsWith<IllegalArgumentException> { EditorCapturePlacement(anchor, 0, 0, 40, 40, 360) }
    }

    @Test fun rejectsCorruptAndUnsupportedDocumentsInsteadOfSilentlyResetting() {
        assertFails { EditorDocumentCodec.decode("broken") }
        assertFails { EditorDocumentCodec.decode(EditorDocumentCodec.encode(sample()).replace("\"schema\":1", "\"schema\":2")) }
    }
}
