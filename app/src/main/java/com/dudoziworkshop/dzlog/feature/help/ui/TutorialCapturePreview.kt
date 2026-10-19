package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.dudoziworkshop.dzlog.domain.model.ContinuousPreviewMode
import com.dudoziworkshop.dzlog.ui.theme.DDZColor

/**
 * Read-only illustrative capture: never reads a user's photos or writes media.
 * Both SHORT and HOLD use exactly the same illustrative image.
 */
@Composable
internal fun TutorialCapturePreview(progress: Float, aspect: Float) {
    val mode = guidePreviewMode(progress)
    Box(Modifier.fillMaxSize()) {
        if (guidePreviewVisible(progress)) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).zIndex(10f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.fillMaxWidth(0.72f).aspectRatio(aspect)
                        .tutorialTarget("camera.capture-confirmation")
                        .clip(RoundedCornerShape(8.dp))
                        .border(2.dp, Color.White, RoundedCornerShape(8.dp))
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFD5E5DA), Color(0xFF8BAE91), Color(0xFF486C4F))))
                        val w = size.width
                        val h = size.height
                        drawCircle(Color(0xFFFFF0C7), radius = w * 0.13f, center = Offset(w * 0.79f, h * 0.18f))
                        for (index in 0..5) {
                            val x = w * (0.08f + index * 0.17f)
                            drawLine(Color(0xFF3B6546), Offset(x, h * 0.8f), Offset(x + w * 0.04f, h * 0.38f), w * 0.023f)
                            drawCircle(Color(0xFF527E55), w * 0.095f, Offset(x + w * 0.04f, h * 0.44f))
                            drawCircle(Color(0xFF345F72), w * 0.022f, Offset(x, h * 0.52f))
                            drawCircle(Color(0xFF345F72), w * 0.022f, Offset(x + w * 0.07f, h * 0.58f))
                        }
                    }
                    Column(
                        Modifier.align(Alignment.BottomEnd).padding(9.dp)
                            .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(3.dp))
                            .padding(5.dp)
                    ) {
                        Text("관찰 기록   |   DZlog", color = Color(0xFF493D32))
                        Text("품종  블루베리   ·   번호  0001", color = Color(0xFF493D32))
                        Text("상태  생육 관찰   ·   예시 자료", color = Color(0xFF493D32))
                    }
                }
            }
        }
        Surface(Modifier.align(Alignment.TopCenter).padding(top = 12.dp).zIndex(11f), color = DDZColor.Surface) {
            val label = when (mode) {
                ContinuousPreviewMode.OFF -> "안 함 · 확인창 없이 계속 촬영"
                ContinuousPreviewMode.SHORT -> "잠깐 · 사진을 잠시 보여준 뒤 닫기"
                ContinuousPreviewMode.HOLD -> "유지 · 확인할 때까지 사진 표시"
            }
            Text("안내용 예시 화면 · $label", Modifier.padding(8.dp), color = DDZColor.TextPrimary)
        }
    }
}
