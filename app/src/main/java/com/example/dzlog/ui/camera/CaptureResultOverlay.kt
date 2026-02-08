package com.example.dzlog.ui.camera

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.dzlog.domain.model.ContinuousPreviewMode
import com.example.dzlog.ui.theme.DDZColor
import com.example.dzlog.ui.theme.DDZSpacing
import com.example.dzlog.ui.theme.DDZTypography

/**
 * [CaptureResultOverlay]
 * - 목적: 촬영 직후 결과 미리보기 오버레이를 표시함(연속 미리보기 UX)
 * - 입력: capturedUri, continuousPreviewMode, aspectRatio, onDismiss
 * - 제외: 촬영 실행/저장/CameraX 제어 로직 금지(표시 전용)
 */
@Composable
fun CaptureResultOverlay(
    capturedUri: Uri?,
    continuousPreviewMode: ContinuousPreviewMode,
    aspectRatio: Float,
    onDismiss: () -> Unit
) {
    if (capturedUri == null) return
    if (continuousPreviewMode == ContinuousPreviewMode.OFF) return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() }
            .zIndex(10f),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 60.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = capturedUri,
                    contentDescription = "Captured result",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(Modifier.height(DDZSpacing.itemGap))

            val hintText = if (continuousPreviewMode == ContinuousPreviewMode.HOLD) {
                "화면을 터치하면 닫힙니다"
            } else {
                "저장 완료"
            }

            Box(
                modifier = Modifier
                    .background(
                        DDZColor.PrimaryDark.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    hintText,
                    color = DDZColor.Surface,
                    style = DDZTypography.Caption
                )
            }
        }
    }
}

