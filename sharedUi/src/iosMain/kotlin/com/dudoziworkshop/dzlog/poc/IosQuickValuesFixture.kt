package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.*
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import platform.Foundation.NSProcessInfo

private const val QUICK_TEST_KEY = "dzlog.ios.quick-values.uitest.v1"

/** Synthetic sensor only; production quick-input UI, workspace, renderer and PhotoKit. */
@Composable
internal fun IosQuickValuesFixture() {
    val seed = remember {
        SavedTableTemplate("quick-values", "빠른 입력 검증", TableTemplateState(2, 3, listOf(
            TableCellState(0, 0, cellId = "heading", rawText = "HEADER", colSpan = 2, quickEditable = false),
            TableCellState(0, 1, cellId = "covered", rawText = "HIDDEN"),
            TableCellState(0, 2, cellId = "date", dataType = TableCellDataType.DATE),
            TableCellState(1, 0, cellId = "note"),
            TableCellState(1, 1, cellId = "number", rawText = "1.20", dataType = TableCellDataType.NUMBER,
                typedValue = CellValue.Number("1.20")),
            TableCellState(1, 2, cellId = "counter", dataType = TableCellDataType.COUNTER,
                typedValue = CellValue.CounterSeed(12))),
            fileNameSlotDrafts = listOf(TableEditorSlotDraft("CELL", "입력", cellId = "note"),
                TableEditorSlotDraft("CELL", "번호", cellId = "counter"))), TableStyleState(), 0L)
    }
    val store = remember { IosWorkspaceStore(QUICK_TEST_KEY) }
    val session = remember {
        if (NSProcessInfo.processInfo.arguments.contains("--reset-quick-values-fixture")) {
            check(!IosCameraCaptureSession.hasPending())
            check(store.save(EditorWorkspace(listOf(EditorDocument(seed, nextCounter = 12, usesAutoNext = false,
                capturePlacement = EditorCapturePlacement(EditorCaptureAnchor.BOTTOM_RIGHT, 50, 50, 60, 20))), seed.id)))
        }
        IosCameraCaptureSession(QUICK_TEST_KEY, store.load(seed))
    }
    var revision by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("대기") }
    val scope = rememberCoroutineScope()
    if (open) {
        val expected = remember { session.selectedDocument }
        IosQuickValuesScreen(expected, onBack = { open = false }) { values ->
            session.saveQuickValues(expected, values).also { saved ->
                if (saved) { revision++; open = false; status = "입력 저장 완료" }
            }
        }
        return
    }
    val doc = remember(revision) { session.selectedDocument }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("빠른 입력 검증")
        Text("상태: $status")
        Text("입력값: ${doc.template.templateState.cells.first { it.cellId == "note" }.rawText}")
        Text("숫자값: ${doc.template.templateState.cells.first { it.cellId == "number" }.rawText}")
        Text("다음 번호: ${doc.nextCounter}")
        Text("파일명: ${session.previewInfo().previewNaming.displayName}")
        Button(enabled = !busy && !session.hasPending(), onClick = { open = true }) { Text("빠른 입력 열기") }
        Button(enabled = !busy, onClick = {
            status = runCatching {
                check(iosQuickValueCells(doc).map { it.cellId } == listOf("note", "number"))
                val changed = updateIosQuickValues(doc, mapOf("number" to "-12.50"))
                check(changed.template.templateState.cells.first { it.cellId == "number" }.typedValue == CellValue.Number("-12.50"))
                check(changed.nextCounter == doc.nextCounter && changed.capturePlacement == doc.capturePlacement)
                for (id in listOf("covered", "heading", "date", "counter", "missing")) {
                    check(runCatching { updateIosQuickValues(doc, mapOf(id to "overwrite")) }.isFailure)
                }
                check(runCatching { updateIosQuickValues(doc, mapOf("number" to "1x")) }.isFailure)
                val changedCell = doc.template.templateState.cells.map { if (it.cellId == "heading") it.copy(colSpan = 4) else it }
                check(runCatching { iosQuickValueCells(doc.copy(template = doc.template.copy(
                    templateState = doc.template.templateState.copy(cells = changedCell)))) }.isFailure)
                val stale = IosCameraCaptureSession(QUICK_TEST_KEY, store.load(seed))
                check(session.saveQuickValues(doc, mapOf("number" to "-12.50")))
                check(!stale.saveQuickValues(doc, mapOf("note" to "stale")))
                check(session.saveQuickValues(session.selectedDocument, mapOf("number" to "1.20")))
                check(session.selectedDocument == doc)
                check(session.renderLiveOverlay(160, 240).isNotEmpty())
                revision++
                "편집 대상·숫자·충돌 차단 검증 완료"
            }.getOrElse { "실패: ${it.message}" }
        }) { Text("입력 정책 검증") }
        Button(enabled = !busy && !session.hasPending(), onClick = {
            busy = true
            scope.launch {
                try {
                    val jpeg = Surface.makeRasterN32Premul(160, 240).use { surface ->
                        surface.canvas.clear(0xFF4B805A.toInt())
                        surface.makeImageSnapshot().use { image ->
                            checkNotNull(image.encodeToData(EncodedImageFormat.JPEG, 90)).use { it.bytes }
                        }
                    }
                    check(session.saveConfiguredJpeg(jpeg))
                    revision++
                    status = "입력 반영 사진 저장 완료"
                } catch (error: Throwable) { status = "실패: ${error.message}" }
                finally { busy = false }
            }
        }) { Text("입력값으로 촬영 저장") }
    }
}
