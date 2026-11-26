package com.dudoziworkshop.dzlog.feature.capture.policy

import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaTarget
import com.dudoziworkshop.dzlog.domain.media.prepareCaptureWorkspaceUpdate
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.domain.phrase.CellPhraseProgress
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.ResolvedCell
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class DurableCaptureSnapshotTest {
    private fun fixture(): DurableCaptureSnapshot {
        val template = SavedTableTemplate(
            id = "template-a", name = "고정 촬영",
            templateState = TableTemplateState(1, 1, listOf(TableCellState(
                cellId = "counter", rowIndex = 0, colIndex = 0,
                dataType = TableCellDataType.COUNTER, rawText = "7",
                typedValue = CellValue.CounterSeed(7),
            ))),
            styleState = TableStyleState(), modifiedAt = 123L,
        )
        val document = EditorDocument(template, saveMode = SaveMode.BOTH,
            nextCounter = 7, usesAutoNext = false)
        val before = EditorWorkspace(listOf(document), template.id)
        val scope = CounterScope("Pictures/DZlog/시험/", "고정_", 7, false)
        val patch = TablePatch(
            updatesByCellId = mapOf("counter" to "8"),
            phraseProgressByCellId = mapOf("phrase" to CellPhraseProgress(1, 2)),
        )
        val preview = FinalCapturePreview(
            resolvedCells = listOf(ResolvedCell("counter", TableCellDataType.COUNTER,
                resolvedText = "0007", isEmpty = false, scopeToken = "0007")),
            tablePatch = patch, displayName = "고정_0007.jpg", usedCounter = 7,
            nextPhraseProgressCursor = 4, counterScope = scope,
            relativePathPreview = "Pictures/DZlog/시험/",
        )
        val plan = CaptureMediaSavePlan("capture-a", 7, scope, patch, listOf(
            CaptureMediaTarget(CaptureMediaRole.RESULT, "Pictures/DZlog/시험/", "고정_0007.jpg"),
            CaptureMediaTarget(CaptureMediaRole.ORIGINAL, "Pictures/DZlog/시험/원본/", "고정_0007.jpg"),
        ))
        val after = prepareCaptureWorkspaceUpdate(before, document, plan)
        return DurableCaptureSnapshot(
            record = DurableCaptureRecord("capture-a", 11, template.id, "project-a", 7, 4,
                DurableCaptureStage.READY),
            capturedDocument = document, workspaceBefore = before, workspaceAfter = after,
            preview = FrozenCapturePreview.freeze(preview), savePlan = plan,
            sourceJpeg = DurableCaptureSourceJpeg("source.jpg", 3,
                "a".repeat(64)),
        )
    }

    @Test fun roundTripPreservesEveryFrozenCaptureDecision() {
        val value = fixture()
        val restored = DurableCaptureSnapshotCodec.decode(DurableCaptureSnapshotCodec.encode(value))
        assertEquals(value, restored)
        assertEquals(value.preview.thaw(), restored.preview.thaw())
    }

    @Test fun rejectsSnapshotWhoseRecordAndPlanReferToDifferentCapture() {
        val value = fixture()
        assertFails { value.copy(record = value.record.copy(captureId = "capture-b")) }
    }

    @Test fun rejectsUnsafeSourceNamesAndMalformedDigests() {
        assertFails { DurableCaptureSourceJpeg("../source.jpg", 3, "a".repeat(64)) }
        assertFails { DurableCaptureSourceJpeg("source.jpg", 3, "A".repeat(64)) }
        assertFails { DurableCaptureSourceJpeg("source.jpg", 0, "a".repeat(64)) }
    }

    @Test fun decoderRejectsUnsupportedSchemaAndTamperedMediaIdentity() {
        val encoded = DurableCaptureSnapshotCodec.encode(fixture())
        assertFails { DurableCaptureSnapshotCodec.decode(encoded.replace("\"schema\":1", "\"schema\":2")) }
        assertFails { DurableCaptureSnapshotCodec.decode(encoded.replace("a".repeat(64), "A".repeat(64))) }
    }
}
