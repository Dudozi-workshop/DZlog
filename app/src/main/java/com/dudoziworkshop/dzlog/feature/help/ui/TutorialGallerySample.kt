package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation

/** Read-only demo content. This screen never reads or writes MediaStore or app photo records. */
internal object TutorialGallerySample {
    val photos = listOf("블루베리 01", "배양 기록 02", "육묘 기록 03", "블루베리 04", "측정 기록 05", "재배 기록 06")
    val folders = listOf("블루베리 관찰", "생육 조사", "배양 기록")
}

/** Bundled illustration placeholders: replace with licensed local photographic drawables before final visual QA. */
@Composable
private fun SamplePicture(index: Int, modifier: Modifier = Modifier, result: Boolean = false) {
    val backgrounds = listOf(Color(0xFFCCDCC5), Color(0xFFE8E2D6), Color(0xFFD9E6D2))
    Box(modifier.background(backgrounds[index % backgrounds.size], RoundedCornerShape(8.dp))) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = size.minDimension
            val center = Offset(size.width * 0.5f, size.height * 0.54f)
            if (index % 3 == 0) {
                for (j in 0..4) {
                    val x = center.x + (j - 2) * unit * 0.14f
                    val y = center.y + (j % 2) * unit * 0.12f
                    drawCircle(Color(0xFF485A86), unit * 0.105f, Offset(x, y))
                    drawCircle(Color(0xFF9DAFCC), unit * 0.026f, Offset(x - unit * 0.025f, y - unit * 0.03f))
                }
                drawLine(Color(0xFF5B8060), Offset(center.x, center.y - unit * 0.28f), Offset(center.x, center.y - unit * 0.1f), unit * 0.025f)
            } else if (index % 3 == 1) {
                drawCircle(Color(0xFFF9FAF7), unit * 0.32f, center)
                drawCircle(Color(0xFFB4C6A1), unit * 0.30f, center, style = Stroke(unit * 0.028f))
                for (j in 0..6) {
                    val x = center.x + ((j % 3) - 1) * unit * 0.15f
                    val y = center.y + ((j / 3) - 1) * unit * 0.13f
                    drawCircle(Color(0xFF79A678), unit * 0.055f, Offset(x, y))
                }
            } else {
                drawLine(Color(0xFF648459), Offset(center.x, center.y + unit * 0.24f), Offset(center.x, center.y - unit * 0.25f), unit * 0.034f)
                for (j in 0..3) {
                    val left = j % 2 == 0
                    drawCircle(Color(0xFF78A477), unit * 0.11f, Offset(center.x + (if (left) -1 else 1) * unit * 0.11f, center.y + (j - 2) * unit * 0.12f))
                }
            }
            if (result) {
                drawRect(Color(0x88FFFFFF), topLeft = Offset(0f, size.height * 0.76f),
                    size = androidx.compose.ui.geometry.Size(size.width, size.height * 0.24f))
            }
        }
        if (result) {
            Text("DZlog  |  2026.10.20", modifier = Modifier.align(Alignment.BottomStart).padding(6.dp),
                color = Color(0xFF3F342A), fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun TutorialGallerySampleScreen(location: TutorialLocation) {
    val viewer = location.sessionId == "viewer"
    val selection = location.sessionId == "photos"
    val folders = location.sessionId == "folders"
    val move = location.pageId == "photos/move"
    val delete = location.pageId == "photos/delete"
    val result = location.pageId.contains("result", ignoreCase = true)
    BoxWithConstraints(Modifier.fillMaxSize().background(Color(0xFFF8F7F5))) {
        val compact = maxHeight < 560.dp
        Column(Modifier.fillMaxSize().padding(if (compact) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("전체 사진", fontWeight = FontWeight.Bold, color = Color(0xFF4B3829))
                Text("튜토리얼 예시", color = Color(0xFF806B59))
            }
            if (viewer) {
                Column(Modifier.fillMaxWidth().weight(1f).tutorialTarget("gallery.content"),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("원본", color = Color(0xFF4B3829), fontWeight = FontWeight.Bold)
                            SamplePicture(0, Modifier.fillMaxWidth().weight(1f), result = false)
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("결과", color = Color(0xFF4B3829), fontWeight = FontWeight.Bold)
                            SamplePicture(0, Modifier.fillMaxWidth().weight(1f), result = true)
                        }
                    }
                    Text("같은 사진에 기록 표기가 적용된 모습을 비교합니다.", color = Color(0xFF806B59))
                }
                Row(Modifier.fillMaxWidth().tutorialTarget("gallery.viewer-actions"),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text("원본 보기", color = Color(0xFF4B3829))
                    Text("결과 보기", color = Color(0xFF4B3829))
                    Text("공유", color = Color(0xFF4B3829))
                }
            } else {
                if (folders) {
                    Column(Modifier.fillMaxWidth().tutorialTarget("gallery.breadcrumbs"),
                        verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("전체 사진 / 블루베리 관찰", color = Color(0xFF806B59))
                        TutorialGallerySample.folders.forEach { folder ->
                            Text("▸  $folder", modifier = Modifier.fillMaxWidth()
                                .background(Color(0xFFEEE9E2), RoundedCornerShape(8.dp)).padding(6.dp),
                                color = Color(0xFF4B3829))
                        }
                    }
                } else {
                    Text("전체 / 최근 촬영 / 원본 / 결과", color = Color(0xFF806B59),
                        modifier = Modifier.tutorialTarget("gallery.breadcrumbs"))
                }
                LazyVerticalGrid(columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxWidth().weight(1f).tutorialTarget("gallery.content"),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(TutorialGallerySample.photos) { index, label ->
                        Box {
                            SamplePicture(index, Modifier.fillMaxWidth().aspectRatio(1f), result = index % 2 == 1)
                            Text((if (selection && index == 0) "☑ " else "") + label,
                                color = Color.White, modifier = Modifier.align(Alignment.BottomStart)
                                    .background(Color(0x99000000)).padding(3.dp))
                        }
                    }
                }
            }
            if (selection) {
                Row(Modifier.fillMaxWidth().tutorialTarget("gallery.selection-actions"),
                    horizontalArrangement = Arrangement.SpaceEvenly) {
                    Text("선택 1개")
                    Text("이동")
                    Text("삭제")
                    Text("공유")
                }
            }
            if (!compact) {
                Text("안내용 화면입니다. 실제 사진과 폴더는 변경되지 않습니다.",
                    color = Color(0xFF806B59))
            }
        }
        if (move || delete) {
            Column(
                Modifier.align(if (compact) Alignment.TopCenter else Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 8.dp else 16.dp, vertical = if (compact) 48.dp else 56.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (move) "사진 이동 예시" else "사진 삭제 예시",
                    fontWeight = FontWeight.Bold, color = Color(0xFF4B3829))
                Text(if (move) "선택한 사진 1장 → 블루베리 관찰 폴더"
                    else "선택한 사진 1장을 삭제하기 전 확인하는 단계입니다.",
                    color = Color(0xFF806B59))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text("취소", color = Color(0xFF806B59), modifier = Modifier.padding(end = 24.dp))
                    Text("확인 (예시)", color = Color(0xFF4B3829), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
