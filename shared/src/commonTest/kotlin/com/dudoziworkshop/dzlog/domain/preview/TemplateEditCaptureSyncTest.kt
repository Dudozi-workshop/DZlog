package com.dudoziworkshop.dzlog.domain.preview

import com.dudoziworkshop.dzlog.domain.model.RotatingPhraseSet
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellKind
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableEditorSlotDraft
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.Test
import com.dudoziworkshop.dzlog.domain.time.CaptureTime

/**
 * DZQ-011: a saved template edit must be the only input to camera table,
 * filename and folder previews. No template switch may be required.
 */
class TemplateEditCaptureSyncTest {
    private val now = CaptureTime(0L, "en-US", "UTC")

    private fun cellSlot(id: String) = TableEditorSlotDraft(
        kind = "CELL", label = id, cellId = id,
    )

    private fun template(
        text: String = "Draper",
        number: String = "5",
        firstPhrase: String = "Before",
        pathSlots: List<TableEditorSlotDraft?> = listOf(cellSlot("number"), cellSlot("phrase")),
    ) = TableTemplateState(
        rows = 1,
        cols = 3,
        cells = listOf(
            TableCellState(
                rowIndex = 0, colIndex = 0, cellId = "text",
                kind = TableCellKind.INPUT, dataType = TableCellDataType.TEXT,
                rawText = text,
            ),
            TableCellState(
                rowIndex = 0, colIndex = 1, cellId = "number",
                kind = TableCellKind.INPUT, dataType = TableCellDataType.NUMBER,
                rawText = number,
            ),
            TableCellState(
                rowIndex = 0, colIndex = 2, cellId = "phrase",
                kind = TableCellKind.INPUT, dataType = TableCellDataType.ROTATING_TEXT,
                phraseSetId = "set", phraseIndex = 0,
            ),
        ),
        phraseSets = listOf(RotatingPhraseSet("set", "Sequence", listOf(firstPhrase, "Next"))),
        fileNameSlotDrafts = listOf(cellSlot("text"), cellSlot("phrase")) +
            List(3) { null },
        pathSlotDrafts = pathSlots + List((5 - pathSlots.size).coerceAtLeast(0)) { null },
    )

    private fun editorPreview(state: TableTemplateState) = buildPreview(
        PreviewInput(
            templateState = state,
            captureNow = now,
            counterDigits = 3,
            dateFormat = "yyyyMMdd",
            timeFormat = "HHmm",
            fnDelim = "_",
            includePathInCounterScope = true,
            includeFilenameInCounterScope = true,
            saveMode = SaveMode.BOTH,
            scopeNextCounter = 7,
            phraseProgressCursor = 1,
        ),
    )

    private fun cameraCapturePreview(state: TableTemplateState): FinalCapturePreview {
        val scope = buildScope(
            CaptureScopeInput(
                templateState = state,
                captureNow = now,
                counterDigits = 3,
                dateFormat = "yyyyMMdd",
                timeFormat = "HHmm",
                fnDelim = "_",
                includeFilenameInCounterScope = true,
                saveMode = SaveMode.BOTH,
                phraseProgressCursor = 1,
            ),
        )
        return buildCapturePreview(
            scopeState = scope,
            input = FinalCapturePreviewInput(
                templateState = state,
                captureNow = now,
                counterDigits = 3,
                dateFormat = "yyyyMMdd",
                timeFormat = "HHmm",
                fnDelim = "_",
                includePathInCounterScope = true,
                includeFilenameInCounterScope = true,
                saveMode = SaveMode.BOTH,
                syncedCounter = 7,
                phraseProgressCursor = 1,
            ),
        )
    }

    private fun assertPreviewMatchesCapture(state: TableTemplateState, name: String, path: String) {
        val editor = editorPreview(state)
        val camera = cameraCapturePreview(state)
        assertEquals(name, editor.previewNaming.displayName)
        assertEquals(path, editor.previewNaming.relativePath)
        assertEquals(name, camera.displayName)
        assertEquals(path, camera.relativePathPreview)
        assertEquals(editor.plan.resolvedCells.map { it.resolvedText },
            camera.resolvedCells.map { it.resolvedText })
    }

    @Test fun changingTextAndNumberInSameTemplateRefreshesAllOutputs() {
        val savedBefore = template()
        val savedAfter = savedBefore.copy(cells = savedBefore.cells.map { cell ->
            when (cell.cellId) {
                "text" -> cell.copy(rawText = "Liberty")
                "number" -> cell.copy(rawText = "9")
                else -> cell
            }
        })

        assertEquals(savedBefore.cells.map { it.cellId }, savedAfter.cells.map { it.cellId })
        assertPreviewMatchesCapture(savedAfter, "Liberty_Before_007.jpg",
            "Pictures/DZlog/9/Before/")
        assertFalse(cameraCapturePreview(savedAfter).displayName.contains("Draper"))
    }

    @Test fun editingPhraseItemsWithoutSwitchingTemplateRefreshesNameAndPath() {
        val savedBefore = template(firstPhrase = "Before")
        val savedAfter = savedBefore.copy(phraseSets = listOf(
            savedBefore.phraseSets.single().copy(items = listOf("After", "Next"))
        ))

        assertEquals(savedBefore.cells, savedAfter.cells)
        assertPreviewMatchesCapture(savedBefore, "Draper_Before_007.jpg",
            "Pictures/DZlog/5/Before/")
        assertPreviewMatchesCapture(savedAfter, "Draper_After_007.jpg",
            "Pictures/DZlog/5/After/")
    }

    @Test fun editingFolderRuleWithoutChangingCellsRefreshesPathOnly() {
        val savedBefore = template()
        val savedAfter = savedBefore.copy(pathSlotDrafts = listOf(cellSlot("text")))

        assertEquals(savedBefore.cells, savedAfter.cells)
        assertPreviewMatchesCapture(savedAfter, "Draper_Before_007.jpg",
            "Pictures/DZlog/Draper/")
    }
}
