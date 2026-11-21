package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.*
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.domain.model.*
import com.dudoziworkshop.dzlog.feature.table.model.TableStyleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Surface
import org.jetbrains.skia.impl.use
import platform.Foundation.NSProcessInfo
import kotlin.math.roundToInt

private const val LIVE_TEST_KEY = "dzlog.ios.live-placement.uitest.v1"

/** Explicit fixture: known sensor raster cropped to the saved capture ratio; native gestures,
 * persistence and PhotoKit are real. */
@Composable
internal fun IosLiveTableFixture() {
    val session = remember {
        val seed = SavedTableTemplate("live-placement", "표 조작 검증",
            TableTemplateState(1, 1, listOf(TableCellState(0, 0, cellId = "live-counter",
                dataType = TableCellDataType.COUNTER, typedValue = CellValue.CounterSeed(12)))), TableStyleState(), 0L)
        val store = IosWorkspaceStore(LIVE_TEST_KEY)
        if (NSProcessInfo.processInfo.arguments.contains("--reset-live-table-fixture")) {
            check(!IosCameraCaptureSession.hasPending())
            check(store.save(EditorWorkspace(listOf(EditorDocument(seed, nextCounter = 12,
                usesAutoNext = false, saveMode = SaveMode.BOTH,
                capturePlacement = EditorCapturePlacement(EditorCaptureAnchor.CUSTOM, 20, 20, 60, 20))), seed.id)))
        }
        IosCameraCaptureSession(LIVE_TEST_KEY, store.load(seed))
    }
    var revision by remember { mutableIntStateOf(0) }
    var ready by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var locked by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("대기") }
    var focusCollapseToken by remember { mutableIntStateOf(0) }
    var cameraZoom by remember { mutableFloatStateOf(1f) }
    var cameraExposureBias by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val simulateFocus = remember { NSProcessInfo.processInfo.arguments.contains("--live-focus-outcomes") }
    val doc = session.selectedDocument
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("표 조작 검증")
        Text("상태: $message")
        Text("다음 번호: ${doc.nextCounter}")
        Text(if (ready) "표 준비 완료" else "표 준비 중")
        Text("회전: ${session.tablePlacement?.rotationCwDeg}")
        Text("배율: ${((cameraZoom * 10).roundToInt() / 10f)}")
        Text("밝기 보정: ${((cameraExposureBias * 10).roundToInt() / 10f)}")
        IosLiveTablePreview(Modifier.fillMaxWidth().weight(1f), session,
            fixtureAspect = session.captureAspect.ratioF.toDouble(),
            enabled = !locked && !busy,
            focusEnabled = !locked && !busy,
            focusCollapseToken = focusCollapseToken,
            autoCollapseFocus = NSProcessInfo.processInfo.arguments.contains("--live-focus-timer"),
            minimumExposureBias = -2f,
            maximumExposureBias = 2f,
            onFocusAt = { x, _, exposureLocked ->
                if (simulateFocus && x > 0.75) {
                    message = "초점 응답 대기"
                    delay(3_000)
                }
                message = if (exposureLocked) "잠금 초점 이동" else "터치 초점 적용"
                !(simulateFocus && x < 0.25)
            },
            onExposureLock = { exposureLocked ->
                message = if (exposureLocked) "노출 잠금" else "노출 자동"
                true
            },
            onExposureBias = { next -> cameraExposureBias = next; true },
            zoomEnabled = !locked && !busy,
            currentZoom = cameraZoom,
            maximumZoom = 6f,
            onZoomChange = { next -> cameraZoom = next; true },
            onZoomFinished = { next ->
                cameraZoom = next
                message = "배율 저장 ${((next * 10).roundToInt() / 10f)}"
            },
            revision = revision, onReady = { ready = it }, onEditing = { editing = it },
            onSaved = { revision += 1; message = "표 배치 저장 완료" }, onError = { message = "실패: $it" })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !editing && !busy, onClick = { locked = !locked }) { Text(if (locked) "조작 잠금 해제" else "조작 잠금") }
            Button(enabled = !editing && !busy, onClick = {
                val p = checkNotNull(session.tablePlacement)
                check(session.savePlacement(p.copy(anchor = IosTableAnchor.CUSTOM,
                    rotationCwDeg = if (p.rotationCwDeg == 0) 90 else 0)))
                revision += 1
            }) { Text("90도 전환") }
            Button(enabled = !editing && !busy, onClick = { focusCollapseToken += 1 }) {
                Text("초점 최소화")
            }
        }
        Button(enabled = !busy && !editing, onClick = {
            busy = true
            scope.launch {
                try {
                    checkIosLiveTableGeometry()
                    val aspect = session.captureAspect
                    session.renderLiveOverlay(aspect.w * 240, aspect.h * 240)
                    val (captureWidth, captureHeight) = aspect.centerCropSize(160, 240)
                    session.checkLiveCaptureAspect(captureWidth, captureHeight)
                    val counter = session.selectedDocument.nextCounter
                    val media = PhotoKitCaptureMediaRepository()
                    val assets = media.listSavedAssets().map { it.asset.assetId }
                    // Configured capture now crops the sensor raster intentionally. Test
                    // the mismatch guard at the placed-raster boundary, before PhotoKit
                    // writes, rather than expecting a valid configured crop to fail.
                    val placement = checkNotNull(session.tablePlacement)
                    val mismatch = runCatching {
                        session.saveAndroidPlacedJpeg(liveFixtureJpeg(240, 160),
                            placement.anchor, placement.offsetXPercent, placement.offsetYPercent,
                            placement.widthRatio, placement.heightRatio, placement.rotationCwDeg)
                    }.exceptionOrNull()
                    check(mismatch is IllegalArgumentException &&
                        mismatch.message.orEmpty().startsWith("사진과 미리보기 비율이 달라")) {
                        "미리보기 비율 불일치가 저장 전에 차단되어야 합니다."
                    }
                    check(session.selectedDocument.nextCounter == counter && !session.hasPending())
                    check(media.listSavedAssets().map { it.asset.assetId } == assets)
                    message = "배치 계산·비율 차단 검증 완료"
                } catch (failure: Throwable) { message = "실패: ${failure.message}" }
                finally { busy = false }
            }
        }) { Text("배치 계산 검증") }
        Button(enabled = ready && !busy && !editing, onClick = {
            busy = true
            scope.launch {
                try {
                    val jpeg = liveFixtureJpeg(160, 240)
                    check(session.saveConfiguredJpeg(jpeg))
                    revision += 1
                    message = "표 배치·사진 저장 완료"
                } catch (failure: Throwable) { message = "실패: ${failure.message}" }
                finally { busy = false }
            }
        }) { Text("이 배치로 촬영 저장") }
    }
}

private fun checkIosLiveTableGeometry() {
    // A 4:1 table fits inside the 600 x 240 placement at 600 x 150.
    val corner = IosCaptureTablePlacement(IosTableAnchor.BOTTOM_RIGHT, 0, 0, 75, 30)
    val visible = iosCaptureTableBounds(800, 600, corner, 4f)
    check(visible == IosTableBounds(200f, 450f, 600f, 150f, 200f, 450f))
    val moved = moveIosCaptureTable(corner, 800, 600, -100f, -225f, 4f)
    val middle = iosCaptureTableBounds(800, 600, moved, 4f)
    check(middle.x == 100f && middle.y == 225f)
    val shrunk = iosCaptureTableBounds(800, 600, resizeIosCaptureTable(corner, 800, 600, 0.8f, 4f), 4f)
    check(shrunk.width < visible.width && shrunk.height < visible.height)
    check(kotlin.math.abs(shrunk.x + shrunk.width / 2 - (visible.x + visible.width / 2)) < 3f)
    check(kotlin.math.abs(shrunk.y + shrunk.height / 2 - (visible.y + visible.height / 2)) < 3f)
    val turned = iosCaptureTableBounds(800, 800, moved.copy(rotationCwDeg = 90), 4f)
    check(turned.width == 150f && turned.height == 600f)

    for ((w, h) in listOf(300 to 450, 450 to 300)) {
        for (anchor in IosTableAnchor.entries) for (rotation in listOf(0, 90)) {
            if (rotation == 90 && anchor != IosTableAnchor.CUSTOM) continue
            val p = IosCaptureTablePlacement(anchor, 50, 50, 60, 20, rotation)
            val initial = iosCaptureTableBounds(w, h, p)
            check(initial.rawX + w * p.widthRatio / 200f == initial.x + initial.width / 2f)
            val moves = listOf(-10000f, -40f, 0f, 40f, 10000f).map { moveIosCaptureTable(p, w, h, it, it) }
            val resizes = listOf(0.1f, 0.5f, 1f, 2f, 10f).map { resizeIosCaptureTable(p, w, h, it) }
            for (edited in moves + resizes) {
                val b = iosCaptureTableBounds(w, h, edited)
                check(b.x >= 0 && b.y >= 0 && b.x + b.width <= w + 0.01f && b.y + b.height <= h + 0.01f)
                check(edited.rotationCwDeg == rotation)
                check(edited.widthRatio in 10..100 && edited.heightRatio in 10..100)
            }
            check(moveIosCaptureTable(p, w, h, -10000f, -10000f).offsetXPercent == 0)
            check(moveIosCaptureTable(p, w, h, 10000f, 10000f).offsetYPercent == 100)
        }
    }
    check(runCatching { moveIosCaptureTable(IosCaptureTablePlacement(IosTableAnchor.CUSTOM, 0, 0, 60, 20),
        300, 450, Float.NaN, 0f) }.isFailure)
}

private fun liveFixtureJpeg(width: Int, height: Int): ByteArray =
    Surface.makeRasterN32Premul(width, height).use { surface ->
        surface.canvas.clear(0xFF4B805A.toInt())
        surface.makeImageSnapshot().use { snapshot ->
            checkNotNull(snapshot.encodeToData(EncodedImageFormat.JPEG, 90)).use { it.bytes }
        }
    }
