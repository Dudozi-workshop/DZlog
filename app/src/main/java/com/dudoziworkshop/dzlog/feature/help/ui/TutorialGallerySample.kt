package com.dudoziworkshop.dzlog.feature.help.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import com.dudoziworkshop.dzlog.R
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.feature.help.model.TutorialLocation

/** Read-only demo content. This screen never reads or writes MediaStore or app photo records. */
internal object TutorialGallerySample {
    val photos = listOf("단이 산책", "제주 여행", "카페 기록", "업무 기록", "단이 기록", "반려동물 기록")
    val folders = listOf("단이 사진", "여행 기록", "일상 기록")
}

/** Bundled onboarding photos; never reads user media or external storage. */
@Composable
private fun SamplePicture(index: Int, modifier: Modifier = Modifier, result: Boolean = false) {
    val images = listOf(
        R.drawable.onboarding_gallery_photo,
        R.drawable.onboarding_hero_photo,
        R.drawable.onboarding_starter_photo,
    )
    Box(modifier.background(Color(0xFFE8E2D6), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(images[index % images.size]),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
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
    val destination = location.pageId in setOf("photos/destination", "folders/destination")
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
        if (destination) {
            Column(
                Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .padding(horizontal = if (compact) 8.dp else 16.dp, vertical = 20.dp)
                    .tutorialTarget("guide.dialog")
                    .background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("이동할 위치 선택", fontWeight = FontWeight.Bold, color = Color(0xFF4B3829))
                Text("Pictures / DZlog / 단이 사진", color = Color(0xFF806B59))
                TutorialGallerySample.folders.forEach { folder ->
                    Text("▸  $folder", Modifier.fillMaxWidth()
                        .background(Color(0xFFEEE9E2), RoundedCornerShape(8.dp)).padding(8.dp),
                        color = Color(0xFF4B3829))
                }
                Text("폴더 및 사진 미리보기", color = Color(0xFF806B59))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index -> SamplePicture(index, Modifier.weight(1f).aspectRatio(1f)) }
                }
                Text("이 위치로 이동 (안내 전용)", color = Color(0xFF4B3829),
                    modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
        if (move || delete) {
            Column(
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = if (compact) 8.dp else 16.dp, vertical = if (compact) 16.dp else 40.dp)
                    .tutorialTarget("guide.dialog")
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(if (move) "사진 이동 예시" else "사진 삭제 예시",
                    fontWeight = FontWeight.Bold, color = Color(0xFF4B3829))
                Text(if (move) "선택한 사진 1장 → 단이 사진 폴더"
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
