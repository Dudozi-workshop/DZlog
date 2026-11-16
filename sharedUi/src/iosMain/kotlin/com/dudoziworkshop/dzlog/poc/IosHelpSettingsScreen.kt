package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.ui.common.DDZTopBar
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private data class IosTutorialScene(val title: String, val instruction: String)

private data class IosTutorialStep(
    val title: String,
    val instruction: String,
    val scenes: List<IosTutorialScene> = emptyList(),
)

private data class IosTutorialModule(
    val id: String,
    val title: String,
    val steps: List<IosTutorialStep>,
)

private data class IosTutorialPage(val stepIndex: Int, val sceneIndex: Int?)

private val iosTutorialModules = listOf(
    IosTutorialModule("camera", "카메라 촬영", listOf(
        IosTutorialStep("촬영 화면 구성", "다음 파일명, 카메라, 표와 하단 촬영 버튼들을 확인해요."),
        IosTutorialStep("빠른 수정", "텍스트·숫자 셀을 입력하고 앞뒤 셀과 파일명·저장경로 연결을 확인해요."),
        IosTutorialStep("촬영 도구", "줌, 초점, 플래시, 격자, 표 표시와 보조 셔터를 구분해요."),
        IosTutorialStep("촬영 후 확인", "최근 사진, 앨범, 직전 촬영 삭제와 다음 파일명을 알아봐요."),
    )),
    IosTutorialModule("table-gesture", "표 조작", listOf(
        IosTutorialStep("표 선택", "표를 터치하면 상세·크기·회전·잠금 도구가 나타나요."),
        IosTutorialStep("표 이동", "표를 드래그해 촬영 영역 안에서 위치를 이동해요."),
        IosTutorialStep("표 크기", "슬라이더와 핸들로 비율을 유지하며 표를 조절해요."),
        IosTutorialStep("회전과 잠금", "0도/90도 회전, 배치 잠금과 셀 값 수정을 구분해요."),
    )),
    IosTutorialModule("table-detail", "표 상세 편집", listOf(
        IosTutorialStep("편집 화면과 양식", "내용·구조·스타일·저장설정 탭과 양식 교체 시 저장 선택을 알아봐요."),
        IosTutorialStep("셀 종류와 내용", "직접 입력과 자동 생성되는 값을 구분해요.", listOf(
            IosTutorialScene("텍스트", "문자를 직접 입력해요."),
            IosTutorialScene("숫자", "측정값을 직접 입력해요."),
            IosTutorialScene("자동번호", "촬영 성공을 기준으로 번호가 진행돼요."),
            IosTutorialScene("날짜", "촬영 날짜가 자동 표시돼요."),
            IosTutorialScene("시간", "촬영 시각이 자동 표시돼요."),
            IosTutorialScene("순환문구", "문구 목록과 변경 주기에 따라 표시돼요."),
        )),
        IosTutorialStep("표 구조", "행·열, 병합과 해제, 크기 변경 및 삭제 영향을 확인해요."),
        IosTutorialStep("표 스타일", "배경·투명도·문자 크기·정렬·테두리를 비교해요."),
        IosTutorialStep("실행 취소와 저장", "편집 취소·다시 실행과 저장/저장 안 함/취소를 구분해요."),
    )),
    IosTutorialModule("saving", "저장 및 자동번호", listOf(
        IosTutorialStep("저장설정 개요", "파일명·저장 위치·자동번호 설정은 서로 달라요."),
        IosTutorialStep("파일명 구성", "요소와 출처·순서·완성된 파일명을 확인해요."),
        IosTutorialStep("저장 위치", "DZlog 앨범의 폴더 경로에 맞춰 사진을 분류해요."),
        IosTutorialStep("자동번호", "다음 번호·자릿수·동기화·초기화·범위를 알아봐요."),
        IosTutorialStep("고급 옵션과 확인", "저장 방식과 파일명·경로·번호 결과를 확인해요."),
    )),
    IosTutorialModule("gallery", "갤러리·폴더 관리", listOf(
        IosTutorialStep("갤러리 첫 화면", "전체·폴더·즐겨찾기와 최근 촬영을 확인해요."),
        IosTutorialStep("폴더 탐색", "현재 폴더의 사진과 하위 폴더·원본사진을 구분해요."),
        IosTutorialStep("사진 보기", "뷰어·즐겨찾기·공유·정보·사진 메뉴를 알아봐요."),
        IosTutorialStep("폴더 관리", "새 폴더, 이름 변경, 이동과 삭제 전 영향을 확인해요."),
        IosTutorialStep("사진 관리", "사진 선택과 이동·공유·삭제를 구분해요."),
    )),
)

@Composable
internal fun IosHelpSettingsScreen(onBack: () -> Unit) {
    var selectedModule by remember { mutableStateOf<IosTutorialModule?>(null) }
    var showIntroduction by remember { mutableStateOf(false) }

    when {
        showIntroduction -> IosIntroductionReader(onBack = { showIntroduction = false })
        selectedModule != null -> IosTutorialReader(
            module = requireNotNull(selectedModule),
            onBack = { selectedModule = null },
        )
        else -> IosHelpIndex(
            onOpenIntroduction = { showIntroduction = true },
            onOpenModule = { selectedModule = it },
            onBack = onBack,
        )
    }
}

@Composable
private fun IosHelpIndex(
    onOpenIntroduction: () -> Unit,
    onOpenModule: (IosTutorialModule) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = "사용법 및 도움말", onBack = onBack)
    }) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding)
                .verticalScroll(rememberScrollState()).padding(DDZSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DZlog의 주요 기능을 순서대로 살펴볼 수 있어요.",
                style = DDZTypography.Body, color = DDZColor.TextSecondary)
            IosHelpCard("처음 사용법 다시 보기", "3단계 · 기존 설정은 변경되지 않아요.", onOpenIntroduction)
            Text("기능별 사용법", style = DDZTypography.SettingLabel, color = DDZColor.PrimaryDark,
                modifier = Modifier.padding(top = 8.dp))
            iosTutorialModules.forEach { module ->
                IosHelpCard(module.title, "${module.steps.size}단계") { onOpenModule(module) }
            }
        }
    }
}

@Composable
private fun IosHelpCard(title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        color = DDZColor.Surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DDZColor.Border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.HelpOutline, null, tint = DDZColor.PrimaryDark)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Text(subtitle, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, null, tint = DDZColor.IconMuted)
        }
    }
}

@Composable
private fun IosTutorialReader(module: IosTutorialModule, onBack: () -> Unit) {
    val pages = remember(module.id) {
        module.steps.flatMapIndexed { index, step ->
            listOf(IosTutorialPage(index, null)) +
                step.scenes.indices.map { sceneIndex -> IosTutorialPage(index, sceneIndex) }
        }
    }
    var pageIndex by remember(module.id) { mutableIntStateOf(0) }
    val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
    val page = pages[safeIndex]
    val step = module.steps[page.stepIndex]
    val scene = page.sceneIndex?.let(step.scenes::get)

    IosReadOnlyTutorialPage(
        title = module.title,
        progress = "단계 ${page.stepIndex + 1}/${module.steps.size}" +
            (page.sceneIndex?.let { "  ·  세부 ${it + 1}/${step.scenes.size}" } ?: ""),
        heading = scene?.title ?: step.title,
        instruction = scene?.instruction ?: step.instruction,
        previousEnabled = safeIndex > 0,
        isLast = safeIndex == pages.lastIndex,
        onPrevious = { pageIndex = safeIndex - 1 },
        onNext = { if (safeIndex == pages.lastIndex) onBack() else pageIndex = safeIndex + 1 },
        onBack = onBack,
    )
}

@Composable
private fun IosIntroductionReader(onBack: () -> Unit) {
    val pages = listOf(
        "찍고, 기록하고, 자동으로 정리하다." to
            "사진 위에 기록표를 더하고 파일명과 폴더까지 자동으로 정리해요.",
        "사진 한 장으로 정리까지 끝내요." to
            "촬영 화면에서 필요한 값만 수정하면 표, 파일명과 저장 위치에 함께 반영돼요.",
        "나에게 맞는 기록으로 시작해요." to
            "표 편집과 저장 설정에서 기록 항목을 바꿀 수 있어요. 이 안내는 설정을 변경하지 않아요.",
    )
    var pageIndex by remember { mutableIntStateOf(0) }
    val safeIndex = pageIndex.coerceIn(0, pages.lastIndex)
    val page = pages[safeIndex]
    IosReadOnlyTutorialPage(
        title = "처음 사용법",
        progress = "${safeIndex + 1} / ${pages.size}",
        heading = page.first,
        instruction = page.second,
        previousEnabled = safeIndex > 0,
        isLast = safeIndex == pages.lastIndex,
        onPrevious = { pageIndex = safeIndex - 1 },
        onNext = { if (safeIndex == pages.lastIndex) onBack() else pageIndex = safeIndex + 1 },
        onBack = onBack,
    )
}

@Composable
private fun IosReadOnlyTutorialPage(
    title: String,
    progress: String,
    heading: String,
    instruction: String,
    previousEnabled: Boolean,
    isLast: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(containerColor = DDZColor.Background, topBar = {
        DDZTopBar(title = title, onBack = onBack)
    }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                    .padding(DDZSpacing.screenPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(progress, style = DDZTypography.Caption, color = DDZColor.PrimaryDark)
                Text(heading, style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
                Surface(color = DDZColor.Surface, shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, DDZColor.Border)) {
                    Text(instruction, style = DDZTypography.Body, color = DDZColor.TextPrimary,
                        modifier = Modifier.fillMaxWidth().padding(18.dp))
                }
                Text("읽기 전용 안내입니다. 촬영·저장·삭제·설정 변경은 수행하지 않아요.",
                    style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(DDZSpacing.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(modifier = Modifier.weight(1f), enabled = previousEnabled,
                    onClick = onPrevious) { Text("이전", color = DDZColor.PrimaryDark) }
                Button(modifier = Modifier.weight(1f), onClick = onNext,
                    colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark)) {
                    Text(if (isLast) "목록으로" else "다음")
                }
            }
        }
    }
}
