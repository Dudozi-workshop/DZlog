package com.dudoziworkshop.dzlog.domain.media

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CaptureWorkspaceCommitTest {
    private val path = "Pictures/DZlog/Records/"
    private val original = EditorDocument(
        template = SavedTableTemplate(
            id = "active",
            name = "현장 기록",
            modifiedAt = 10L,
            templateState = TableTemplateState(
                1, 1,
                listOf(TableCellState(0, 0, cellId = "count",
                    dataType = TableCellDataType.COUNTER, typedValue = CellValue.CounterSeed(12))),
            ),
            styleState = TableStyleState(),
        ),
        saveMode = SaveMode.ORIGINAL_ONLY, nextCounter = 12,
    )
    private val other = original.copy(template = original.template.copy(id = "other"))
    private val current = EditorWorkspace(listOf(other, original), "active")

    private fun plan(mode: SaveMode = SaveMode.ORIGINAL_ONLY, id: String = "capture-1"): CaptureMediaSavePlan {
        val preview = FinalCapturePreview(
            resolvedCells = emptyList(),
            tablePatch = TablePatch(mapOf("count" to "13")),
            displayName = "record_0012.jpg",
            usedCounter = 12, nextPhraseProgressCursor = 0,
            counterScope = CounterScope(buildCounterPath(path, mode), "record_", 12, false),
            relativePathPreview = path,
        )
        return buildCaptureMediaSavePlan(preview, mode, id)
    }

    private fun saved(plan: CaptureMediaSavePlan): ConfirmedCaptureMediaSave =
        plan.confirm(plan.targets.mapIndexed { index, target ->
            PersistedCaptureAsset(target, "asset-${index + 1}")
        })

    @Test fun successfulOriginalSaveUpdatesOnlySelectedCounterAndTablePatch() {
        val prepared = plan()
        val next = commitConfirmedCaptureToWorkspace(current, original, prepared, saved(prepared))
        assertEquals(13, next.active?.nextCounter)
        assertEquals(CellValue.CounterSeed(13), next.active?.template?.templateState?.cells?.single()?.typedValue)
        assertEquals(other, next.documents.first())
        assertEquals("active", next.activeTemplateId)
        assertEquals(12, current.active?.nextCounter)
        assertEquals(CellValue.CounterSeed(12), original.template.templateState.cells.single().typedValue)
    }

    @Test fun duplicateCommitOrChangedEditorCannotApplyStaleCapture() {
        val prepared = plan()
        val next = commitConfirmedCaptureToWorkspace(current, original, prepared, saved(prepared))
        assertFailsWith<IllegalStateException> {
            commitConfirmedCaptureToWorkspace(next, original, prepared, saved(prepared))
        }
        val edited = current.copy(documents = listOf(other, original.copy(counterPadding = 3)))
        assertFailsWith<IllegalStateException> {
            commitConfirmedCaptureToWorkspace(edited, original, prepared, saved(prepared))
        }
        assertEquals(original, current.active)
    }

    @Test fun incompleteReceiptOrWrongCaptureIsRejectedBeforeStateChanges() {
        val prepared = plan()
        assertFailsWith<IllegalArgumentException> { prepared.confirm(emptyList()) }
        assertFailsWith<IllegalStateException> {
            commitConfirmedCaptureToWorkspace(current, original, prepared, saved(plan(id = "another")))
        }
        assertEquals(12, current.active?.nextCounter)
    }

    @Test fun unsynchronizedCounterAndSaveModeMismatchAreFailClosed() {
        val prepared = plan()
        val unknown = original.copy(nextCounter = null)
        val workspace = current.copy(documents = listOf(other, unknown))
        assertFailsWith<IllegalStateException> {
            commitConfirmedCaptureToWorkspace(workspace, unknown, prepared, saved(prepared))
        }
        val both = plan(SaveMode.BOTH)
        assertFailsWith<IllegalStateException> {
            commitConfirmedCaptureToWorkspace(current, original, both, saved(both))
        }
        assertEquals(12, current.active?.nextCounter)
    }

    @Test fun noCommitAfterActiveTemplateSwitch() {
        val prepared = plan()
        val switched = current.copy(activeTemplateId = "other")
        assertFailsWith<IllegalStateException> {
            commitConfirmedCaptureToWorkspace(switched, original, prepared, saved(prepared))
        }
        assertEquals(original, current.active)
    }
}
