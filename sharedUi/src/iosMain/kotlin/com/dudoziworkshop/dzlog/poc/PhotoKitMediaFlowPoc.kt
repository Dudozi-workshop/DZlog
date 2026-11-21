package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.CaptureMediaAvailability
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.launch
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Surface
import platform.Foundation.NSUUID

private const val MEDIA_FIXTURE_PATH = "Pictures/DZlog/M3A-PhotoKit/"

/**
 * Explicit --media-flow simulator fixture. This is not the production camera UI:
 * Android is untouched, and normal iOS startup cannot write synthetic photos.
 * It exercises the real PhotoKit adapter and gallery recovery across app relaunch.
 */
@Composable
internal fun PhotoKitMediaFlowPoc() {
    val repository = remember { PhotoKitCaptureMediaRepository() }
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("저장 전") }
    var readStatus by remember { mutableStateOf("조회 전") }
    var savedAssets by remember { mutableStateOf(0) }
    var firstImage by remember { mutableStateOf<ImageBitmap?>(null) }
    var logicalPath by remember { mutableStateOf("") }
    var displayedName by remember { mutableStateOf("") }

    suspend fun refreshFromPhotos() {
        val assets = repository.listSavedAssets().filter {
            it.asset.target.relativePath.startsWith(MEDIA_FIXTURE_PATH)
        }
        savedAssets = assets.size
        logicalPath = assets.firstOrNull()?.asset?.target?.relativePath.orEmpty()
        displayedName = assets.firstOrNull()?.asset?.target?.displayName.orEmpty()
        val available = assets.firstOrNull { it.availability == CaptureMediaAvailability.PRESENT }
        val data = available?.let { repository.readAsset(it.asset.assetId) }
        firstImage = data?.takeIf { it.isNotEmpty() }?.let {
            SkiaImage.makeFromEncoded(it).toComposeImageBitmap()
        }
        readStatus = when {
            assets.isEmpty() -> "저장된 사진 없음"
            assets.any { it.availability == CaptureMediaAvailability.INACCESSIBLE } -> "사진 접근 불가"
            assets.any { it.availability == CaptureMediaAvailability.MISSING } -> "사진 누락"
            firstImage == null -> "사진 데이터 읽기 실패"
            else -> "사진 복원 완료"
        }
    }

    LaunchedEffect(Unit) {
        try { refreshFromPhotos() }
        catch (error: Throwable) { readStatus = "조회 실패: ${error.message}" }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DDZColor.Background)
            .safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("DZlog M3-A PhotoKit 검증")
        Text("저장: $saveStatus")
        Text("조회: $readStatus")
        Text("자산 수: $savedAssets")
        Text("논리 경로: $logicalPath")
        Text("사진 이름: $displayedName")
        Button(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                try {
                    val captureId = NSUUID().UUIDString.lowercase()
                    val fileName = "M3A_${captureId.take(8)}.jpg"
                    val mode = SaveMode.BOTH
                    // The fixture builds the same common Android naming/path plan.
                    val preview = FinalCapturePreview(
                        resolvedCells = emptyList(),
                        tablePatch = TablePatch(emptyMap()),
                        displayName = fileName,
                        usedCounter = 1,
                        nextPhraseProgressCursor = 0,
                        counterScope = CounterScope(buildCounterPath(MEDIA_FIXTURE_PATH, mode),
                            "M3A_", 1, false),
                        relativePathPreview = MEDIA_FIXTURE_PATH,
                    )
                    val plan = buildCaptureMediaSavePlan(preview, mode, captureId)
                    val saved = repository.saveAllOrRollback(plan, mapOf(
                        CaptureMediaRole.RESULT to fixtureJpeg(0xFF4B805A.toInt()),
                        CaptureMediaRole.ORIGINAL to fixtureJpeg(0xFF9A765A.toInt()),
                    ))
                    saveStatus = "저장 완료: ${saved.assets.size}개"
                    refreshFromPhotos()
                } catch (error: Throwable) {
                    saveStatus = "저장 실패: ${error.message}"
                } finally {
                    busy = false
                }
            }
        }) { Text(if (busy) "저장 중" else "테스트 사진 저장") }
        Button(enabled = !busy, onClick = {
            scope.launch {
                try { refreshFromPhotos() }
                catch (error: Throwable) { readStatus = "조회 실패: ${error.message}" }
            }
        }) { Text("갤러리 다시 조회") }
        firstImage?.let { image ->
            Image(bitmap = image, contentDescription = "PhotoKit 복원 이미지",
                modifier = Modifier.size(180.dp))
        }
    }
}

private fun fixtureJpeg(color: Int): ByteArray {
    val surface = Surface.makeRasterN32Premul(128, 128)
    surface.canvas.clear(color)
    return checkNotNull(surface.makeImageSnapshot().encodeToData(EncodedImageFormat.JPEG, 88)) {
        "Could not encode a simulator JPEG fixture."
    }.bytes
}
