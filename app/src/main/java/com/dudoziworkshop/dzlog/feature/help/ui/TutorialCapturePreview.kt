package com.dudoziworkshop.dzlog.feature.help.ui

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.ui.camera.preview.CaptureResultOverlay
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/** The production result overlay, with a labelled empty state instead of fabricated photos. */
@Composable
internal fun TutorialCapturePreview(uri: Uri?, progress: Float, aspect: Float) {
    val mode = guidePreviewMode(progress)
    Box(Modifier.fillMaxSize()) {
        if (guidePreviewVisible(progress)) CaptureResultOverlay(
            capturedUri = uri, continuousPreviewMode = mode, aspectRatio = aspect, onDismiss = {},
            emptyPreviewHint = "촬영한 사진이 없어요.\n촬영 후 이 위치에 사진이 표시돼요.",
            photoModifier = Modifier.tutorialTarget("camera.capture-confirmation"),
        )
        Surface(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).zIndex(11f), color = DDZColor.Surface) {
            val label = when (mode) {
                ContinuousPreviewMode.OFF -> "안 함 · 확인창 없이 계속 촬영"
                ContinuousPreviewMode.SHORT -> "잠깐 · 사진을 잠시 보여준 뒤 닫기"
                ContinuousPreviewMode.HOLD -> "유지 · 확인할 때까지 사진 표시"
            }
            Text("${if (uri != null) "저장 사진으로 보는 표시 예시" else "표시 위치 예시"} · $label", Modifier.padding(8.dp), color = DDZColor.TextPrimary)
        }
    }
}
