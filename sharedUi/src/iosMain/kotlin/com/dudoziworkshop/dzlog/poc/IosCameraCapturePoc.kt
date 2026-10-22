@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.media.PhotoKitCaptureMediaRepository
import com.dudoziworkshop.dzlog.domain.counter.CounterScope
import com.dudoziworkshop.dzlog.domain.media.CaptureMediaRole
import com.dudoziworkshop.dzlog.domain.media.buildCaptureMediaSavePlan
import com.dudoziworkshop.dzlog.domain.model.SaveMode
import com.dudoziworkshop.dzlog.domain.naming.buildCounterPath
import com.dudoziworkshop.dzlog.domain.preview.FinalCapturePreview
import com.dudoziworkshop.dzlog.domain.table.TablePatch
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSUUID
import platform.UIKit.UIView

private const val CAMERA_POC_PATH = "Pictures/DZlog/Camera-M3A/"

/**
 * Device-only AVFoundation -> PhotoKit proof of flow, behind --camera-flow.
 * Does not claim Android table overlay rendering or camera feature parity.
 * The normal iOS home/camera entry point remains unchanged during M3-A.
 */
@Composable
internal fun IosCameraCapturePoc() {
    val camera = remember { IosAvFoundationCamera() }
    val repository = remember { PhotoKitCaptureMediaRepository() }
    val scope = rememberCoroutineScope()
    var ready by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("카메라 확인 중") }
    var successfulSaves by remember { mutableIntStateOf(0) }

    LaunchedEffect(camera) {
        try {
            if (!camera.requestCameraPermission()) {
                status = "카메라 권한이 없습니다."
            } else if (!camera.configure()) {
                status = "카메라를 사용할 수 없는 기기입니다."
            } else {
                withContext(Dispatchers.Default) { camera.start() }
                ready = true
                status = "카메라 준비 완료"
            }
        } catch (error: Throwable) {
            status = "카메라 준비 실패: ${error.message}"
        }
    }
    DisposableEffect(camera) {
        onDispose { camera.stop() }
    }

    Column(
        Modifier.fillMaxSize().background(DDZColor.Background)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("iOS M3-A 실제 촬영 검증")
        Text(status)
        Text("저장 완료 수: $successfulSaves")
        if (ready) {
            UIKitView(
                modifier = Modifier.fillMaxWidth().height(360.dp),
                factory = {
                    object : UIView() {
                        override fun layoutSubviews() {
                            super.layoutSubviews()
                            camera.previewLayer.frame = bounds
                        }
                    }.apply {
                        layer.addSublayer(camera.previewLayer)
                    }
                },
            )
        } else {
            Spacer(Modifier.height(200.dp))
        }
        Button(enabled = ready && !busy, onClick = {
            busy = true
            status = "사진 촬영 중"
            runCatching {
                camera.captureJpeg { result ->
                    scope.launch {
                        try {
                            val jpeg = result.getOrThrow()
                            val captureId = NSUUID().UUIDString.lowercase()
                            val mode = SaveMode.ORIGINAL_ONLY
                            val plan = buildCaptureMediaSavePlan(
                                FinalCapturePreview(
                                    resolvedCells = emptyList(),
                                    tablePatch = TablePatch.EMPTY,
                                    displayName = "CAM_${captureId.take(8)}.jpg",
                                    usedCounter = 1,
                                    nextPhraseProgressCursor = 0,
                                    counterScope = CounterScope(
                                        buildCounterPath(CAMERA_POC_PATH, mode), "CAM_", 1, false,
                                    ),
                                    relativePathPreview = CAMERA_POC_PATH,
                                ),
                                mode,
                                captureId,
                            )
                            val receipt = repository.saveAllOrRollback(
                                plan, mapOf(CaptureMediaRole.ORIGINAL to jpeg),
                            )
                            // Never mark a capture complete before PhotoKit confirms its asset.
                            check(receipt.assets.size == 1)
                            successfulSaves += 1
                            status = "사진 저장 완료"
                        } catch (error: Throwable) {
                            status = "사진 저장 실패: ${error.message}"
                        } finally {
                            busy = false
                        }
                    }
                }
            }.onFailure { error ->
                status = "촬영 실패: ${error.message}"
                busy = false
            }
        }) { Text(if (busy) "촬영 중" else "테스트 사진 촬영") }
    }
}
