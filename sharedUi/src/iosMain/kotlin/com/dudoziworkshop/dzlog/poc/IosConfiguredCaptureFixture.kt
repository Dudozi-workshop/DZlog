package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.*
import com.dudoziworkshop.dzlog.data.template.*
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlinx.coroutines.launch
import org.jetbrains.skia.*
import platform.Foundation.NSProcessInfo

private const val CONFIGURED_TEST_KEY = "dzlog.ios.configured-capture.uitest.v1"

/** Synthetic sensor input only. Settings, compositor, PhotoKit, recovery and gallery are real. */
@Composable
internal fun IosConfiguredCaptureFixture() {
    val merged = NSProcessInfo.processInfo.arguments.contains("--merged-capture-fixture")
    val editorKey = if (merged) "$CONFIGURED_TEST_KEY.merged" else CONFIGURED_TEST_KEY
    val seed = remember(merged) {
        val counter = TableCellState(rowIndex = if (merged) 1 else 0, colIndex = 0, cellId = "counter",
            dataType = TableCellDataType.COUNTER, typedValue = CellValue.CounterSeed(12))
        val table = if (merged) TableTemplateState(2, 2, listOf(
            TableCellState(0, 0, cellId = "merged-header", rawText = "MERGED", colSpan = 2),
            TableCellState(0, 1, cellId = "covered", rawText = "HIDDEN"), counter,
            TableCellState(1, 1, cellId = "note", rawText = "FIELD"),
        ), rowWeights = listOf(1f, 2f), colWeights = listOf(2f, 1f))
        else TableTemplateState(1, 1, listOf(counter))
        SavedTableTemplate(if (merged) "configured-merged" else "configured-test", "설정 연결 검증", table,
            TableStyleState(), 0L)
    }
    val store = remember(editorKey) { IosWorkspaceStore(editorKey) }
    var workspace by remember {
        if (NSProcessInfo.processInfo.arguments.contains("--reset-configured-fixture")) {
            check(!IosCameraCaptureSession.hasPending()) { "미완료 촬영을 덮어쓸 수 없습니다." }
            check(store.save(EditorWorkspace(listOf(EditorDocument(seed, nextCounter = 12,
                usesAutoNext = false)), seed.id)))
        }
        mutableStateOf(store.load(seed))
    }
    var settings by remember { mutableStateOf(false) }
    var gallery by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("대기") }
    val scope = rememberCoroutineScope()
    val media = remember { PhotoKitCaptureMediaRepository() }
    LaunchedEffect(Unit) {
        if (IosCameraCaptureSession.hasPending()) {
            val recovered = IosCameraCaptureSession.recover(store, seed)
            check(recovered) { "복구 실패" }
            workspace = store.load(seed)
            if (merged) check((workspace.active?.template?.templateState?.cells?.first { it.cellId == "counter" }
                ?.typedValue as? CellValue.CounterSeed)?.start == 13)
            status = "재실행 복구 완료"
        }
    }
    if (settings) {
        IosCaptureSettingsScreen(checkNotNull(workspace.active), { settings = false }) { document ->
            check(store.save(workspace.save(document)))
            workspace = store.load(seed)
            check(workspace.active == document)
            settings = false
            status = "설정 복원 완료"
        }
        return
    }
    if (gallery) {
        IosPhotoKitGalleryScreen(media, onBack = { gallery = false })
        return
    }
    val doc = checkNotNull(workspace.active)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("설정 연결 촬영 검증")
        Text("상태: $status")
        Text("모드: ${doc.saveMode.name}")
        if (merged) Text("병합 표 사용 중")
        Text("다음 번호: ${doc.nextCounter}")
        Text("배치: ${doc.capturePlacement?.anchor?.name ?: "없음"}")
        Text("대기 기록: ${IosCameraCaptureSession.hasPending()}")
        Button(onClick = { settings = true }, enabled = !busy && !IosCameraCaptureSession.hasPending()) { Text("촬영 설정 열기") }
        for (interrupt in listOf(false, true)) {
            Button(enabled = !busy && !IosCameraCaptureSession.hasPending(), onClick = {
                busy = true
                scope.launch {
                    try {
                        check(IosCameraWorkspaceBridge.prepare(editorKey, workspace) == null)
                        val known = media.listSavedAssets().map { it.captureId }.toSet()
                        val surface = Surface.makeRasterN32Premul(160, 240)
                        surface.canvas.clear(0xFF4B805A.toInt())
                        val jpeg = checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 88)).bytes
                        val session = if (interrupt) IosCameraCaptureSession(editorKey, workspace,
                            persistWorkspace = { false }) else checkNotNull(IosCameraWorkspaceBridge.current)
                        val committed = session.saveConfiguredJpeg(jpeg)
                        check(committed != interrupt)
                        val added = media.listSavedAssets().filter { it.captureId !in known }
                        val roles = when (doc.saveMode) {
                            SaveMode.ORIGINAL_ONLY -> setOf(CaptureMediaRole.ORIGINAL)
                            SaveMode.WATERMARK_ONLY -> setOf(CaptureMediaRole.RESULT)
                            SaveMode.BOTH -> setOf(CaptureMediaRole.ORIGINAL, CaptureMediaRole.RESULT)
                        }
                        check(added.size == roles.size && added.map { it.asset.target.role }.toSet() == roles)
                        check(added.map { it.captureId }.distinct().size == 1)
                        for (entry in added) {
                            check(entry.availability == CaptureMediaAvailability.PRESENT)
                            val restored = checkNotNull(media.readAsset(entry.asset.assetId))
                            val image = Image.makeFromEncoded(restored)
                            check(image.width == 160 && image.height == 240)
                            if (entry.asset.target.role == CaptureMediaRole.RESULT) check(!restored.contentEquals(jpeg))
                        }
                        workspace = store.load(seed)
                        check(workspace.active?.nextCounter == if (interrupt) 12 else 13)
                        if (merged) {
                            val cells = checkNotNull(workspace.active).template.templateState.cells
                            check(cells.size == 4 && cells.first { it.cellId == "covered" }.rawText == "HIDDEN")
                            check(cells.first { it.cellId == "merged-header" }.colSpan == 2)
                            check((cells.first { it.cellId == "counter" }.typedValue as? CellValue.CounterSeed)?.start ==
                                if (interrupt) 12 else 13)
                        }
                        status = if (interrupt) "PhotoKit 저장·워크스페이스 복구 대기" else "설정대로 저장 완료"
                    } catch (error: Throwable) {
                        status = "실패: ${error.message}"
                    } finally {
                        IosCameraWorkspaceBridge.clear()
                        busy = false
                    }
                }
            }) { Text(if (interrupt) "저장 후 복구 대기" else "촬영 저장") }
        }
        Button(onClick = { gallery = true }, enabled = !busy) { Text("갤러리 열기") }
        Button(enabled = !busy, onClick = {
            status = runCatching { checkIosMergedCaptureRaster(); "병합 픽셀·오류 차단 검증 완료" }
                .getOrElse { "실패: ${it.message}" }
        }) { Text("병합 렌더 검증") }
    }
}
