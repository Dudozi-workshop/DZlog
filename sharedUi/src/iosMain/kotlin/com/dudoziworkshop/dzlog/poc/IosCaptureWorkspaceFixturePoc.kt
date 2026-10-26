package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.EditorDocument
import com.dudoziworkshop.dzlog.data.template.EditorWorkspace
import com.dudoziworkshop.dzlog.data.template.SavedTableTemplate
import com.dudoziworkshop.dzlog.domain.model.CellValue
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.domain.model.TableCellState
import com.dudoziworkshop.dzlog.domain.model.TableTemplateState
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface

// An explicit XCTest launch route only. Normal home and production preferences never use this key.
private const val CAPTURE_FIXTURE_KEY = "dzlog.ios.m3a.capture-workspace.uitest.v1"
private const val FIXTURE_COUNTER = 12

/**
 * Tests the live iOS capture-save coordinator using an in-memory JPEG instead of
 * unavailable simulator camera hardware. The PhotoKit and workspace writes are real.
 * The synthetic image cannot be accessed from the normal camera launch path.
 */
@Composable
internal fun IosCaptureWorkspaceFixturePoc() {
    val template = remember {
        SavedTableTemplate(
            id = "m3a-capture-workspace-fixture",
            name = "촬영 저장 상태 테스트",
            templateState = TableTemplateState(1, 1, listOf(
                TableCellState(
                    rowIndex = 0, colIndex = 0, cellId = "m3a-counter",
                    dataType = TableCellDataType.COUNTER,
                    typedValue = CellValue.CounterSeed(FIXTURE_COUNTER),
                ),
            )),
            styleState = TableStyleState(),
            modifiedAt = 0L,
        )
    }
    val store = remember { IosWorkspaceStore(CAPTURE_FIXTURE_KEY) }
    var workspace by remember { mutableStateOf(runCatching { store.load(template) }.getOrNull()) }
    var status by remember { mutableStateOf("대기") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val nextNumber = workspace?.active?.nextCounter?.toString() ?: "-"
    val cellNumber = ((workspace?.active?.template?.templateState?.cells?.firstOrNull()
        ?.typedValue) as? CellValue.CounterSeed)?.start?.toString() ?: "-"

    Column(
        Modifier.fillMaxSize().background(DDZColor.Background).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("iOS M3-A 워크스페이스 저장 검증")
        Text("저장 상태: " + status)
        Text("다음 촬영 번호: " + nextNumber)
        Text("자동번호 셀: " + cellNumber)
        Button(enabled = !busy, onClick = {
            busy = true
            status = "합성 이미지 누락 검증 중"
            scope.launch {
                try {
                    val doc = EditorDocument(
                        template, saveMode = SaveMode.BOTH,
                        nextCounter = FIXTURE_COUNTER, usesAutoNext = false,
                    )
                    val before = EditorWorkspace(listOf(doc), template.id)
                    check(store.save(before)) { "테스트 워크스페이스 준비 실패" }
                    val error = runCatching {
                        IosCameraCaptureSession(CAPTURE_FIXTURE_KEY, before)
                            .saveJpeg(captureFixtureJpeg())
                    }.exceptionOrNull()
                    check(error is IllegalArgumentException || error is IllegalStateException) {
                        "합성 이미지 누락이 차단되지 않았습니다."
                    }
                    val after = store.load(template)
                    check(after.active?.nextCounter == FIXTURE_COUNTER) {
                        "실패한 촬영에서 번호가 변경됐습니다."
                    }
                    check(!IosCameraCaptureSession.hasPending()) {
                        "실패한 촬영에서 미완료 저장 기록이 생성됐습니다."
                    }
                    workspace = after
                    status = "합성 이미지 누락 차단·번호 유지"
                } catch (error: Throwable) {
                    status = "실패: " + error.message.orEmpty()
                } finally {
                    busy = false
                }
            }
        }) { Text("BOTH 합성 누락 검증") }
        Button(enabled = !busy, onClick = {
            busy = true
            status = "저장 중"
            scope.launch {
                try {
                    check(!IosCameraCaptureSession.hasPending()) {
                        "이전 촬영 기록이 미완료 상태입니다."
                    }
                    val doc = EditorDocument(
                        template, saveMode = SaveMode.ORIGINAL_ONLY,
                        nextCounter = FIXTURE_COUNTER, usesAutoNext = false,
                    )
                    val before = EditorWorkspace(listOf(doc), template.id)
                    check(store.save(before)) { "테스트 워크스페이스 준비 실패" }
                    val committed = IosCameraCaptureSession(CAPTURE_FIXTURE_KEY, before)
                        .saveJpeg(captureFixtureJpeg())
                    check(committed) { "PhotoKit 저장 후 상태 확정 실패" }
                    val after = store.load(template)
                    check(after.active?.nextCounter == FIXTURE_COUNTER + 1)
                    val counterCell = after.active?.template?.templateState?.cells?.singleOrNull()
                        ?.typedValue as? CellValue.CounterSeed
                    check(counterCell?.start == FIXTURE_COUNTER + 1)
                    workspace = after
                    status = "원본 저장·워크스페이스 확정"
                } catch (error: Throwable) {
                    status = "실패: " + error.message.orEmpty()
                } finally {
                    busy = false
                }
            }
        }) { Text(if (busy) "저장 중" else "테스트 촬영 저장") }
    }
}

private fun captureFixtureJpeg(): ByteArray {
    val surface = Surface.makeRasterN32Premul(128, 128)
    surface.canvas.clear(0xFF4B805A.toInt())
    return checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 88)) {
        "Cannot encode iOS capture workspace JPEG fixture."
    }.bytes
}
