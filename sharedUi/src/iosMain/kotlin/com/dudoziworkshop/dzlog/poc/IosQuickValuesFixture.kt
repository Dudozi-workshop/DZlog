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
        SavedTableTemplate("quick-values", "빠른 입력 검증", TableTemplateState(2, 4, listOf(
            TableCellState(0, 0, cellId = "heading", rawText = "HEADER", colSpan = 2, quickEditable = false),
            TableCellState(0, 1, cellId = "covered", rawText = "HIDDEN"),
            TableCellState(0, 2, cellId = "date", dataType = TableCellDataType.DATE, formatPattern = "yyyyMMdd"),
            TableCellState(0, 3, cellId = "phrase", dataType = TableCellDataType.ROTATING_TEXT,
                phraseSetId = "set-a", phraseIndex = 1, phraseCompletedShots = 2),
            TableCellState(1, 0, cellId = "note"),
            TableCellState(1, 1, cellId = "number", rawText = "1.20", dataType = TableCellDataType.NUMBER,
                typedValue = CellValue.Number("1.20")),
            TableCellState(1, 2, cellId = "counter", dataType = TableCellDataType.COUNTER,
                typedValue = CellValue.CounterSeed(12)),
            TableCellState(1, 3, cellId = "time", dataType = TableCellDataType.TIME)),
            phraseSets = listOf(
                RotatingPhraseSet("set-a", "과수원", listOf("A", "B")),
                RotatingPhraseSet("set-b", "온실", listOf("C", "D")),
            ),
            fileNameSlotDrafts = listOf(TableEditorSlotDraft("CELL", "입력", cellId = "note"),
                TableEditorSlotDraft("CELL", "번호", cellId = "counter"))), TableStyleState(), 0L)
    }
    val alternate = remember(seed) {
        seed.copy(
            id = "quick-values-alt",
            name = "빠른 입력 보조",
            templateState = seed.templateState.copy(cells = seed.templateState.cells.map { cell ->
                if (cell.cellId == "note") cell.copy(rawText = "SECOND", typedValue = CellValue.Text("SECOND")) else cell
            }),
            modifiedAt = 1L,
        )
    }
    val store = remember { IosWorkspaceStore(QUICK_TEST_KEY) }
    val session = remember {
        if (NSProcessInfo.processInfo.arguments.contains("--reset-quick-values-fixture")) {
            check(!IosCameraCaptureSession.hasPending())
            val placement = EditorCapturePlacement(EditorCaptureAnchor.BOTTOM_RIGHT, 50, 50, 60, 20)
            check(store.save(EditorWorkspace(listOf(
                EditorDocument(seed, nextCounter = 12, usesAutoNext = false, capturePlacement = placement),
                EditorDocument(alternate, nextCounter = 40, usesAutoNext = false, capturePlacement = placement),
            ), seed.id)))
        }
        IosCameraCaptureSession(QUICK_TEST_KEY, store.load(seed))
    }
    var revision by remember { mutableIntStateOf(0) }
    var open by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("대기") }
    val scope = rememberCoroutineScope()
    if (open) {
        var expected by remember { mutableStateOf(session.selectedDocument) }
        IosQuickValuesScreen(expected, onBack = { open = false },
            availableDocuments = session.documents,
            onSwitchTemplate = { targetId, changes ->
                session.switchQuickTemplate(expected, targetId, changes).also { switched ->
                    if (switched) { expected = session.selectedDocument; revision++ }
                }
            }) { changes ->
            session.saveQuickValues(expected, changes).also { saved ->
                if (saved) { revision++; open = false; status = "입력 저장 완료" }
            }
        }
        return
    }
    val doc = remember(revision) { session.selectedDocument }
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("빠른 입력 검증")
        Text("상태: $status")
        Text("활성템플릿: ${doc.template.name}")
        Text("입력값: ${doc.template.templateState.cells.first { it.cellId == "note" }.rawText}")
        Text("숫자값: ${doc.template.templateState.cells.first { it.cellId == "number" }.rawText}")
        Text("날짜형식: ${doc.template.templateState.cells.first { it.cellId == "date" }.formatPattern}")
        Text("문구세트: ${doc.template.templateState.cells.first { it.cellId == "phrase" }.phraseSetId}")
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
                check(iosQuickAutoValueCells(doc).map { it.cellId } == listOf("date", "phrase", "time")) {
                    "자동 셀 선별 규칙이 일치하지 않습니다."
                }
                val automatic = applyIosQuickValueChanges(doc, IosQuickValueChanges(
                    manualValues = mapOf("note" to doc.template.templateState.cells.first { it.cellId == "note" }.rawText,
                        "number" to "1.20"),
                    dateFormats = mapOf("date" to "yyMMdd"),
                    phraseSetIds = mapOf("phrase" to "set-b"),
                ))
                check(automatic.template.templateState.cells.first { it.cellId == "date" }.formatPattern == "yyMMdd") {
                    "날짜 형식 변경이 초안에 반영되지 않았습니다."
                }
                val phrase = automatic.template.templateState.cells.first { it.cellId == "phrase" }
                check(phrase.phraseSetId == "set-b" && phrase.phraseIndex == 0 && phrase.phraseCompletedShots == 0) {
                    "순환문구 세트 변경 시 해당 셀 진행 상태가 초기화되지 않았습니다."
                }
                check(runCatching { applyIosQuickValueChanges(doc, IosQuickValueChanges(
                    manualValues = mapOf("note" to "", "number" to "1.20"),
                    dateFormats = mapOf("date" to "yyyy-MM-dd"))) }.isFailure) {
                    "허용되지 않은 날짜 형식을 차단하지 못했습니다."
                }
                check(runCatching { applyIosQuickValueChanges(doc, IosQuickValueChanges(
                    manualValues = mapOf("note" to "", "number" to "1.20"),
                    phraseSetIds = mapOf("phrase" to "missing"))) }.isFailure) {
                    "존재하지 않는 순환문구 세트를 차단하지 못했습니다."
                }
                val stale = IosCameraCaptureSession(QUICK_TEST_KEY, store.load(seed))
                check(session.saveQuickValues(doc, IosQuickValueChanges(mapOf("number" to "-12.50")))) {
                    "최신 빠른 수정 초안을 저장하지 못했습니다."
                }
                check(!stale.saveQuickValues(doc, IosQuickValueChanges(mapOf("note" to "stale")))) {
                    "오래된 빠른 수정 초안을 차단하지 못했습니다."
                }
                check(session.saveQuickValues(session.selectedDocument, IosQuickValueChanges(mapOf("number" to "1.20")))) {
                    "숫자 원래 값을 복원하지 못했습니다."
                }
                check(session.switchQuickTemplate(session.selectedDocument, alternate.id,
                    IosQuickValueChanges(mapOf("note" to "", "number" to "1.20")))) {
                    "보조 템플릿으로 전환하지 못했습니다."
                }
                check(session.selectedDocument.template.id == alternate.id && session.selectedDocument.nextCounter == 40) {
                    "전환한 템플릿의 독립 번호를 복원하지 못했습니다."
                }
                check(session.switchQuickTemplate(session.selectedDocument, seed.id,
                    IosQuickValueChanges(mapOf("note" to "SECOND", "number" to "1.20")))) {
                    "원래 템플릿으로 돌아오지 못했습니다."
                }
                val restored = session.selectedDocument
                check(restored.template.id == doc.template.id && restored.nextCounter == doc.nextCounter &&
                    restored.capturePlacement == doc.capturePlacement &&
                    restored.template.templateState.cells.first { it.cellId == "number" }.rawText == "1.20") {
                    "템플릿 왕복 후 촬영 핵심 상태가 달라졌습니다."
                }
                check(session.renderLiveOverlay(160, 240).isNotEmpty()) {
                    "빠른 수정 후 실시간 표를 렌더링하지 못했습니다."
                }
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
