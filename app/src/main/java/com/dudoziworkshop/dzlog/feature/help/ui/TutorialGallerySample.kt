package com.dudoziworkshop.dzlog.feature.help.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import com.dudoziworkshop.dzlog.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation

/** Read-only demo content. This screen never reads or writes MediaStore or app photo records. */
internal object TutorialGallerySample {
    val photos = listOf("단이 산책", "제주 여행", "카페 기록", "업무 기록", "단이 기록", "반려동물 기록")
    val folders = listOf("단이 사진", "여행 기록", "일상 기록")
}

/** Bundled crops from the previously approved photo reference; tutorial only. */
@Composable
private fun SamplePicture(index: Int, modifier: Modifier = Modifier, result: Boolean = false) {
    val context = LocalContext.current
    // Photo-only rectangular regions of the previously approved source montage.
    // Kept fully inside this read-only sample; never accesses the user's gallery.
    val regions = listOf(
        intArrayOf(804, 230, 141, 113), // dog outdoors
        intArrayOf(988, 250, 121, 110), // coast
        intArrayOf(806, 409, 128, 103), // coffee
        intArrayOf(976, 427, 133, 93), // notebook
        intArrayOf(534, 342, 61, 61), // dog portrait
        intArrayOf(665, 339, 59, 61), // sleeping cat
    )
    val bitmap = remember(index) {
        val source = BitmapFactory.decodeResource(context.resources, R.drawable.tutorial_dani_reference)
        val region = regions[index % regions.size]
        val scaleX = source.width / 1140f
        val scaleY = source.height / 735f
        val x = (region[0] * scaleX).toInt().coerceIn(0, source.width - 1)
        val y = (region[1] * scaleY).toInt().coerceIn(0, source.height - 1)
        val width = (region[2] * scaleX).toInt().coerceIn(1, source.width - x)
        val height = (region[3] * scaleY).toInt().coerceIn(1, source.height - y)
        android.graphics.Bitmap.createBitmap(source, x, y, width, height).asImageBitmap()
    }
    Box(modifier.background(Color(0xFFE8E2D6), RoundedCornerShape(8.dp))) {
        Image(bitmap = bitmap, contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (result) {
            Text("DZlog  |  예시 기록", modifier = Modifier.align(Alignment.BottomStart)
                .fillMaxWidth().background(Color(0xCCFFFFFF)).padding(6.dp),
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
                        Text("전체 사진 / 단이 사진", color = Color(0xFF806B59))
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
