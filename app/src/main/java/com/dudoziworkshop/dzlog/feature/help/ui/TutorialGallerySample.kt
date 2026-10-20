package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Purely visual teaching data. No files, folders, or MediaStore entries are created. */
internal object TutorialGallerySample {
    val photos = listOf(
        "블루베리 01", "배양 기록 02", "육묘 기록 03",
        "블루베리 04", "측정 기록 05", "재배 기록 06",
    )
    val folders = listOf("블루베리 관찰", "생육 조사", "배양 기록")
}

@Composable
internal fun TutorialGallerySampleScreen(showViewer: Boolean) {
    val photos = TutorialGallerySample.photos
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF8F7F5)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("전체 사진", fontWeight = FontWeight.Bold, color = Color(0xFF4B3829))
            Text("튜토리얼 예시", color = Color(0xFF806B59))
        }
        if (showViewer) {
            Box(
                Modifier.fillMaxWidth().weight(1f).tutorialTarget("gallery.content")
                    .background(Color(0xFFE0E9DD), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(photos.first(), color = Color(0xFF4B3829)) }
            Row(Modifier.fillMaxWidth().tutorialTarget("gallery.viewer-actions"),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Text("원본 보기")
                Text("결과 보기")
                Text("공유")
            }
        } else {
            Text("폴더  ·  " + TutorialGallerySample.folders.joinToString("  /  "),
                color = Color(0xFF806B59))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().weight(1f).tutorialTarget("gallery.content"),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(photos) { label ->
                    Box(
                        Modifier.aspectRatio(1f).background(Color(0xFFE0E9DD), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) { Text(label, color = Color(0xFF4B3829)) }
                }
            }
        }
        Text("이 화면은 기능 설명용 예시이며 실제 사진과 폴더에는 영향을 주지 않습니다.",
            color = Color(0xFF806B59))
    }
}
