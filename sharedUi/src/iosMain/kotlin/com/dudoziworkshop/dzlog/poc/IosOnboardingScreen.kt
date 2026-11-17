package com.dudoziworkshop.dzlog.poc

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterCatalog
import com.dudoziworkshop.dzlog.data.template.OnboardingStarterDefinition
import com.dudoziworkshop.dzlog.domain.model.TableCellDataType
import com.dudoziworkshop.dzlog.ui.theme.DDZColor
import com.dudoziworkshop.dzlog.ui.theme.DDZSpacing
import com.dudoziworkshop.dzlog.ui.theme.DDZTypography

private data class IosStarterCategory(val id: String, val title: String)

private val iosStarterCategories = listOf(
    IosStarterCategory("basic", "기본 촬영"),
    IosStarterCategory("daily", "일상·취미"),
    IosStarterCategory("work", "업무·현장"),
)

@Composable
internal fun IosOnboardingScreen(busy: Boolean, onChoose: (String) -> Unit) {
    var step by remember { mutableIntStateOf(1) }
    var category by remember { mutableStateOf("basic") }
    var chosen by remember { mutableStateOf(OnboardingStarterCatalog.DEFAULT_ID) }
    val selected = OnboardingStarterCatalog.find(chosen)
        ?: OnboardingStarterCatalog.definitions.first()

    Column(
        modifier = Modifier.fillMaxSize().background(DDZColor.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DZlog", style = DDZTypography.ScreenTitle, color = DDZColor.PrimaryDark)
            Text("$step / 3", style = DDZTypography.Caption, color = DDZColor.TextSecondary)
        }
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = DDZSpacing.screenPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (step) {
                1 -> IosOnboardingIntroduction()
                2 -> IosOnboardingWorkflow()
                else -> IosStarterSelection(
                    category = category,
                    chosen = chosen,
                    selected = selected,
                    onCategory = { id ->
                        category = id
                        chosen = OnboardingStarterCatalog.definitions.first { it.category == id }.id
                    },
                    onChoose = { chosen = it },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Surface(color = DDZColor.Surface, border = BorderStroke(1.dp, DDZColor.Border)) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = { if (step == 3) onChoose(chosen) else step++ },
                    colors = ButtonDefaults.buttonColors(containerColor = DDZColor.PrimaryDark),
                ) { Text(if (step == 1) "시작하기" else if (step == 2) "다음" else "이 양식으로 시작") }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(enabled = !busy && step > 1, onClick = { step-- }) { Text("이전") }
                    if (step < 3) {
                        OutlinedButton(enabled = !busy, onClick = {
                            onChoose(OnboardingStarterCatalog.DEFAULT_ID)
                        }) { Text("건너뛰기") }
                    }
                }
                if (busy) CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}

@Composable
private fun IosOnboardingIntroduction() {
    Text("찍고, 기록하고,\n자동으로 정리하다.",
        style = DDZTypography.HomeMainTitle, color = DDZColor.TextPrimary)
    Text("사진 위에 기록표를 더하고, 파일명과 폴더까지 자동으로 정리해요.",
        style = DDZTypography.Body, color = DDZColor.TextSecondary)
    IosIntroTile("01", "사진을 찍어요", "평소처럼 촬영하면 돼요.")
    IosIntroTile("02", "표로 기록해요", "내용은 촬영 화면에서 빠르게 수정할 수 있어요.")
    IosIntroTile("03", "자동으로 저장해요", "원본과 기록사진을 함께 보관해요.")
}

@Composable
private fun IosOnboardingWorkflow() {
    Text("사진 한 장으로\n정리까지 끝내요.",
        style = DDZTypography.HomeMainTitle, color = DDZColor.TextPrimary)
    IosIntroTile("1", "사진 촬영", "촬영 화면 위에 기록표가 표시돼요.")
    IosIntroTile("2", "필요한 정보만 수정", "기록명·대상·구분 등을 직접 입력해요.")
    IosIntroTile("3", "파일명과 저장 위치 자동 구성", "예: DZlog_20261013_001.jpg · Pictures/DZlog/")
    Text("이 안내는 설정의 사용법에서 다시 볼 수 있어요.",
        style = DDZTypography.Caption, color = DDZColor.TextSecondary)
}

@Composable
private fun IosStarterSelection(
    category: String,
    chosen: String,
    selected: OnboardingStarterDefinition,
    onCategory: (String) -> Unit,
    onChoose: (String) -> Unit,
) {
    Text("어떤 기록을 시작할까요?", style = DDZTypography.ScreenTitle, color = DDZColor.TextPrimary)
    Text("나중에도 양식을 추가하거나 바꿀 수 있어요.",
        style = DDZTypography.Body, color = DDZColor.TextSecondary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        iosStarterCategories.forEach { item ->
            val active = item.id == category
            Surface(
                modifier = Modifier.weight(1f).clickable { onCategory(item.id) },
                color = if (active) DDZColor.SelectedSoft else DDZColor.Surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (active) DDZColor.Selected else DDZColor.Border),
            ) {
                Text(item.title, modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                    style = DDZTypography.Caption, color = DDZColor.TextPrimary, maxLines = 1)
            }
        }
    }
    if (category != "basic") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OnboardingStarterCatalog.definitions.filter { it.category == category }.forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { onChoose(item.id) },
                    color = if (chosen == item.id) DDZColor.SelectedSoft else DDZColor.Surface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DDZColor.Border),
                ) {
                    Text(item.title, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = DDZTypography.Body, color = DDZColor.TextPrimary)
                }
            }
        }
    }
    IosStarterPreview(selected)
}

@Composable
private fun IosIntroTile(number: String, title: String, subtitle: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = DDZColor.Surface,
        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, DDZColor.Border)) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(number, style = DDZTypography.Body, color = DDZColor.Selected)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = DDZTypography.Body, color = DDZColor.TextPrimary)
                Text(subtitle, style = DDZTypography.Caption, color = DDZColor.TextSecondary)
            }
        }
    }
}

@Composable
private fun IosStarterPreview(definition: OnboardingStarterDefinition) {
    val state = remember(definition.id) { requireNotNull(OnboardingStarterCatalog.buildState(definition.id)) }
    Surface(modifier = Modifier.fillMaxWidth(), color = DDZColor.Surface,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, DDZColor.Border)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(definition.title, style = DDZTypography.Body, color = DDZColor.PrimaryDark)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                definition.labels.forEach { label ->
                    Text(label, modifier = Modifier.weight(1f).background(DDZColor.Background)
                        .padding(horizontal = 3.dp, vertical = 8.dp),
                        style = DDZTypography.Caption, color = DDZColor.TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                state.cells.filter { it.rowIndex == 1 }.forEach { cell ->
                    val label = when (cell.dataType) {
                        TableCellDataType.DATE -> "자동 날짜"
                        TableCellDataType.COUNTER -> "0001"
                        else -> "입력"
                    }
                    Text(label, modifier = Modifier.weight(1f).padding(horizontal = 3.dp, vertical = 8.dp),
                        style = DDZTypography.Caption, color = DDZColor.TextSecondary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
